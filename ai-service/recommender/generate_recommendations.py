"""
generate_recommendations.py — Dùng model đã train (train.py) để sinh sẵn
Top-N gợi ý cho TỪNG user, ghi kết quả vào bảng recommendation_result.

Java (Spring Boot) chỉ ĐỌC LẠI bảng này khi Investor gọi
GET /api/recommendations/{userId} — KHÔNG gọi sang Python lúc request.

Chạy:
    python -m recommender.generate_recommendations
"""
import logging
import os
import pickle

import numpy as np
from sqlalchemy import create_engine, text

from . import config
from .data_extraction import fetch_raw_events, aggregate_interactions

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)

CREATE_TABLE_SQL = """
CREATE TABLE IF NOT EXISTS recommendation_result (
    recommendation_result_id BIGSERIAL PRIMARY KEY,
    account_id      INTEGER NOT NULL,
    listing_id      INTEGER NOT NULL,
    score           DOUBLE PRECISION NOT NULL,
    rank            INTEGER NOT NULL,
    generated_at    TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_recommendation_result_account
    ON recommendation_result (account_id, rank);
"""


def load_artifacts():
    with open(os.path.join(config.MODEL_DIR, "lightfm_model.pkl"), "rb") as f:
        model = pickle.load(f)
    with open(os.path.join(config.MODEL_DIR, "dataset_mapping.pkl"), "rb") as f:
        dataset = pickle.load(f)
    with open(os.path.join(config.MODEL_DIR, "item_features_matrix.pkl"), "rb") as f:
        item_features_matrix = pickle.load(f)
    with open(os.path.join(config.MODEL_DIR, "user_features_matrix.pkl"), "rb") as f:
        user_features_matrix = pickle.load(f)
    return model, dataset, item_features_matrix, user_features_matrix


def generate_for_all_users(model, dataset, item_features_matrix, user_features_matrix,
                            already_interacted: dict, top_n: int):
    """
    already_interacted: dict {account_id -> set(listing_id đã tương tác)} —
    LOẠI những item user đã xem/lưu/liên hệ rồi ra khỏi danh sách gợi ý.

    Trả về list[(account_id, listing_id, score, rank)].
    """
    user_id_map, _, item_id_map, _ = dataset.mapping()
    internal_to_listing = {v: k for k, v in item_id_map.items()}
    n_items = len(item_id_map)
    all_item_indices = np.arange(n_items)

    results = []
    for account_id, user_internal_id in user_id_map.items():
        # Truyền user_features=user_features_matrix — PHẢI khớp với lúc train,
        # nếu thiếu, model sẽ predict như thể user KHÔNG có đặc trưng gì (sai
        # lệch so với lúc train), đặc biệt sai với đúng nhóm user cold-start
        # mà tính năng này nhắm tới.
        scores = model.predict(
            user_internal_id, all_item_indices,
            item_features=item_features_matrix,
            user_features=user_features_matrix,
        )
        order = np.argsort(-scores)  # giảm dần

        seen = already_interacted.get(account_id, set())
        rank = 0
        for item_internal_id in order:
            listing_id = internal_to_listing[item_internal_id]
            if listing_id in seen:
                continue
            rank += 1
            results.append((account_id, listing_id, float(scores[item_internal_id]), rank))
            if rank >= top_n:
                break

    logger.info("Sinh xong gợi ý cho %d user", len(user_id_map))
    return results


def save_results(engine, results):
    from datetime import datetime
    now = datetime.now()

    with engine.begin() as conn:
        conn.execute(text(CREATE_TABLE_SQL))
        # Bảng recommendation_result có thể đã được HIBERNATE tự tạo trước đó
        # (khi Spring Boot quét entity RecommendationResult, ddl-auto=update)
        # — bảng đó KHÔNG có UNIQUE constraint trên (account_id, listing_id)
        # vì entity Java không khai báo. "CREATE TABLE IF NOT EXISTS" ở trên
        # khi đó bị bỏ qua hoàn toàn (bảng đã tồn tại), nên KHÔNG dùng
        # "ON CONFLICT" (sẽ lỗi "no unique or exclusion constraint").
        #
        # Cách xử lý: KHÔNG cần ON CONFLICT — DELETE toàn bộ bảng NGAY TRƯỚC
        # KHI INSERT (trong CÙNG transaction) đã đảm bảo không bao giờ có
        # xung đột thật để cần xử lý.
        conn.execute(text("DELETE FROM recommendation_result"))
        conn.execute(
            text("""
                INSERT INTO recommendation_result (account_id, listing_id, score, rank, generated_at)
                VALUES (:account_id, :listing_id, :score, :rank, :generated_at)
            """),
            [
                {"account_id": r[0], "listing_id": r[1], "score": r[2], "rank": r[3], "generated_at": now}
                for r in results
            ],
        )
    logger.info("Đã ghi %d dòng gợi ý vào bảng recommendation_result", len(results))


def main():
    engine = create_engine(config.SQLALCHEMY_URL)

    logger.info("Load model đã train (kèm user_features_matrix)...")
    model, dataset, item_features_matrix, user_features_matrix = load_artifacts()

    logger.info("Đọc lại interactions để loại item đã tương tác khỏi gợi ý...")
    raw_events = fetch_raw_events(engine)
    interactions_df = aggregate_interactions(raw_events)
    already_interacted = (
        interactions_df.groupby("account_id")["listing_id"].apply(set).to_dict()
    )

    results = generate_for_all_users(
        model, dataset, item_features_matrix, user_features_matrix, already_interacted, config.TOP_N)
    save_results(engine, results)


if __name__ == "__main__":
    main()
