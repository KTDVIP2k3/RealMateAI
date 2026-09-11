"""
train.py — Huấn luyện LightFM HYBRID MODEL (interactions + item features +
user features) từ dữ liệu trong PostgreSQL.
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
from .user_features import fetch_user_survey, fetch_user_profile, build_user_feature_tags

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)


def build_dataset(interactions_df, item_features_df, user_tags: dict):
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

    user_features_matrix = dataset.build_user_features(
        (uid, user_tags.get(uid, [])) for uid in all_user_ids
    )

    return dataset, interactions_matrix, weights_matrix, item_features_matrix, user_features_matrix


def train_model(interactions_matrix, weights_matrix, item_features_matrix, user_features_matrix,
                 no_components: int = 32, epochs: int = 30, learning_rate: float = 0.05):
    model = LightFM(no_components=no_components, loss="warp", learning_rate=learning_rate, random_state=42)
    model.fit(
        interactions_matrix,
        sample_weight=weights_matrix,
        item_features=item_features_matrix,
        user_features=user_features_matrix,
        epochs=epochs,
        num_threads=1,
        verbose=True,
    )
    return model


def main():
    engine = create_engine(config.SQLALCHEMY_URL)

    logger.info("Bước 1/6 — Trích xuất raw events từ DB...")
    raw_events = fetch_raw_events(engine)

    logger.info("Bước 2/6 — Tổng hợp interaction matrix...")
    interactions_df = aggregate_interactions(raw_events)

    logger.info("Bước 3/6 — Lấy khảo sát + hồ sơ đầu tư (user features)...")
    survey_df = fetch_user_survey(engine)
    profile_df = fetch_user_profile(engine)
    user_tags = build_user_feature_tags(survey_df, profile_df)
    logger.info("Có %d user có ít nhất 1 tag đặc trưng", len(user_tags))

    if interactions_df.empty and not user_tags:
        logger.error("Không đủ dữ liệu để train ở cả 2 nguồn.")
        sys.exit(1)

    logger.info("Bước 4/6 — Lấy item features...")
    item_features_df = fetch_item_features(engine)
    logger.info("Có %d listing với đầy đủ feature", len(item_features_df))

    logger.info("Bước 5/6 — Build Dataset...")
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