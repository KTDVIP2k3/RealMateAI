"""
train.py — Huấn luyện LightFM HYBRID MODEL (interactions + item features +
user features) từ dữ liệu trong PostgreSQL.

Chạy LOCAL trước:
    cd ai-service
    pip install -r requirements.txt
    cp .env.example .env   # rồi điền thông tin DB thật
    python -m recommender.train

Kết quả:
    - models/lightfm_model.pkl          (model đã train)
    - models/dataset_mapping.pkl        (mapping account_id/listing_id <-> internal index)
    - models/item_features_matrix.pkl
    - models/user_features_matrix.pkl
"""
import logging
import os
import pickle
import sys

from lightfm import LightFM
from lightfm.data import Dataset
from sqlalchemy import create_engine

from . import config
from .data_extraction import fetch_raw_events, aggregate_interactions
from .item_features import fetch_item_features, build_feature_list
from .user_features import fetch_survey_features, fetch_profile_features, build_user_feature_tags

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)


def build_dataset(interactions_df, item_features_df, user_tags: dict):
    """
    Dùng lightfm.data.Dataset để tự động map account_id/listing_id sang
    internal index liên tục 0..N mà LightFM cần.

    "all_user_ids" là HỢP (union) của:
      - user đã có tương tác (interactions_df)
      - user CHỈ có khảo sát/hồ sơ đầu tư nhưng CHƯA có tương tác nào
    Lý do bắt buộc: nếu chỉ fit() những user CÓ tương tác, Dataset sẽ không
    biết tới user cold-start -> KHÔNG THỂ gọi model.predict() cho họ được
    (LightFM báo lỗi "unknown user"), dù mục tiêu chính của việc thêm user
    features là để phục vụ ĐÚNG nhóm user này.
    """

    valid_item_ids = set(item_features_df["listing_id"].unique())
    interactions_df = interactions_df[interactions_df["listing_id"].isin(valid_item_ids)]
    dataset = Dataset()

    interaction_user_ids = set(interactions_df["account_id"].unique().tolist())
    survey_user_ids = set(user_tags.keys())
    all_user_ids = sorted(interaction_user_ids | survey_user_ids)

    all_item_ids = item_features_df["listing_id"].unique().tolist()
    all_item_features = sorted({
        feat for _, row in item_features_df.iterrows() for feat in build_feature_list(row)
    })
    all_user_features = sorted({tag for tags in user_tags.values() for tag in tags})

    dataset.fit(
        users=all_user_ids,
        items=all_item_ids,
        item_features=all_item_features,
        user_features=all_user_features,
    )

    (interactions_matrix, weights_matrix) = dataset.build_interactions(
        (row.account_id, row.listing_id, row.weight) for row in interactions_df.itertuples()
    )

    item_features_matrix = dataset.build_item_features(
        (row.listing_id, build_feature_list(row)) for _, row in item_features_df.iterrows()
    )

    # User nào không có tag nào (chưa khảo sát, chưa tạo kế hoạch đầu tư) vẫn
    # PHẢI xuất hiện trong build_user_features với danh sách rỗng — nếu bỏ
    # sót, LightFM sẽ báo lỗi thiếu feature cho user đó.
    user_features_matrix = dataset.build_user_features(
        (uid, user_tags.get(uid, [])) for uid in all_user_ids
    )

    return dataset, interactions_matrix, weights_matrix, item_features_matrix, user_features_matrix


def train_model(interactions_matrix, weights_matrix, item_features_matrix, user_features_matrix,
                 no_components: int = 32, epochs: int = 30, learning_rate: float = 0.05):
    """WARP loss — tối ưu trực tiếp cho bài toán ranking Top-N."""
    model = LightFM(no_components=no_components, loss="warp", learning_rate=learning_rate, random_state=42)
    model.fit(
        interactions_matrix,
        sample_weight=weights_matrix,
        item_features=item_features_matrix,
        user_features=user_features_matrix,
        epochs=epochs,
        # QUAN TRỌNG: LUÔN dùng num_threads=1. Bản lightfm build từ source
        # (bắt buộc trên Windows do lỗi __LIGHTFM_SETUP__ khi build qua pip
        # hiện đại) KHÔNG có OpenMP support — ép num_threads > 1 trong tình
        # huống này gây DEADLOCK THẬT (treo hẳn, không phải chậm) do cơ chế
        # khoá nội bộ của lightfm không hoạt động đúng khi thiếu OpenMP. Đã
        # xác nhận bug này thật khi test trên Windows — giữ num_threads=1 là
        # bắt buộc, không phải tối ưu hoá tuỳ chọn.
        num_threads=1,
        verbose=True,
    )
    return model


def main():
    engine = create_engine(config.SQLALCHEMY_URL)

    logger.info("Bước 1/6 — Trích xuất raw events từ DB...")
    raw_events = fetch_raw_events(engine)

    logger.info("Bước 2/6 — Tổng hợp interaction matrix theo trọng số sự kiện...")
    interactions_df = aggregate_interactions(raw_events)

    logger.info("Bước 3/6 — Lấy khảo sát + hồ sơ đầu tư (user features)...")
    survey_df = fetch_survey_features(engine)
    profile_df = fetch_profile_features(engine)
    user_tags = build_user_feature_tags(survey_df, profile_df)
    logger.info("Có %d user có ít nhất 1 tag đặc trưng (khảo sát/hồ sơ đầu tư)", len(user_tags))

    if interactions_df.empty and not user_tags:
        logger.error(
            "Không có tương tác (active_log) LẪN không có khảo sát/hồ sơ đầu tư nào trong DB. "
            "Không đủ dữ liệu để train ở cả 2 nguồn — kiểm tra lại đã có traffic/khảo sát thật chưa."
        )
        sys.exit(1)
    if interactions_df.empty:
        logger.warning(
            "Chưa có tương tác nào (active_log rỗng) — model sẽ CHỈ học được từ khảo sát/hồ sơ đầu "
            "tư (content-based thuần), chưa có tín hiệu collaborative filtering. Vẫn train được, "
            "nhưng độ chính xác sẽ thấp hơn khi có đủ cả 2 nguồn."
        )

    logger.info("Bước 4/6 — Lấy item features (property_type, province)...")
    item_features_df = fetch_item_features(engine)
    logger.info("Có %d listing với đầy đủ feature", len(item_features_df))

    logger.info("Bước 5/6 — Build Dataset (interactions + item features + user features)...")
    dataset, interactions_matrix, weights_matrix, item_features_matrix, user_features_matrix = build_dataset(
        interactions_df, item_features_df, user_tags)

    logger.info("Bước 6/6 — Huấn luyện LightFM hybrid model...")
    model = train_model(interactions_matrix, weights_matrix, item_features_matrix, user_features_matrix)

    os.makedirs(config.MODEL_DIR, exist_ok=True)
    with open(os.path.join(config.MODEL_DIR, "lightfm_model.pkl"), "wb") as f:
        pickle.dump(model, f)
    with open(os.path.join(config.MODEL_DIR, "dataset_mapping.pkl"), "wb") as f:
        pickle.dump(dataset, f)
    with open(os.path.join(config.MODEL_DIR, "item_features_matrix.pkl"), "wb") as f:
        pickle.dump(item_features_matrix, f)
    with open(os.path.join(config.MODEL_DIR, "user_features_matrix.pkl"), "wb") as f:
        pickle.dump(user_features_matrix, f)

    logger.info("Huấn luyện xong. Model lưu tại: %s", config.MODEL_DIR)


if __name__ == "__main__":
    main()
