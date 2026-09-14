"""
train.py — Train LightFM model từ database Postgres và lưu ra các file pkl artifact.
"""
import logging
import os
import pickle
import numpy as np
import pandas as pd
from lightfm import LightFM
from lightfm.data import Dataset
from sqlalchemy import create_engine

from . import config
from .data_extraction import fetch_raw_events, aggregate_interactions
from .item_features import fetch_item_features, build_item_feature_tags
from .user_features import fetch_user_survey, fetch_user_profile, build_user_feature_tags

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)


def main():
    os.makedirs(config.MODEL_DIR, exist_ok=True)
    engine = create_engine(config.SQLALCHEMY_URL)

    logger.info("1/4. Đọc dữ liệu từ DB...")
    raw_events = fetch_raw_events(engine)
    interactions_df = aggregate_interactions(raw_events)

    item_df = fetch_item_features(engine)
    item_tags = build_item_feature_tags(item_df)

    survey_df = fetch_user_survey(engine)
    profile_df = fetch_user_profile(engine)
    user_tags = build_user_feature_tags(survey_df, profile_df)

    all_users = set(interactions_df["account_id"].unique()).union(user_tags.keys())
    all_items = set(interactions_df["listing_id"].unique()).union(item_tags.keys())

    all_item_features = set()
    for tags in item_tags.values():
        all_item_features.update(tags)

    all_user_features = set()
    for tags in user_tags.values():
        all_user_features.update(tags)

    logger.info("2/4. Fit Dataset LightFM (%d users, %d items)...", len(all_users), len(all_items))
    dataset = Dataset()
    dataset.fit(
        users=all_users,
        items=all_items,
        user_features=all_user_features,
        item_features=all_item_features,
    )

    (interactions_matrix, weights_matrix) = dataset.build_interactions(
        [(row["account_id"], row["listing_id"], row["weight"]) for _, row in interactions_df.iterrows()]
    )

    item_features_matrix = dataset.build_item_features(
        [(item_id, tags) for item_id, tags in item_tags.items()]
    )

    user_features_matrix = dataset.build_user_features(
        [(user_id, tags) for user_id, tags in user_tags.items()]
    )

    logger.info("3/4. Huấn luyện LightFM model...")
    model = LightFM(
        loss=getattr(config, "LIGHTFM_LOSS", "warp"),
        no_components=getattr(config, "LIGHTFM_NO_COMPONENTS", 32),
        learning_rate=getattr(config, "LIGHTFM_LEARNING_RATE", 0.05),
        random_state=getattr(config, "LIGHTFM_RANDOM_STATE", 42),
    )
    model.fit(
        interactions=interactions_matrix,
        sample_weight=weights_matrix,
        user_features=user_features_matrix,
        item_features=item_features_matrix,
        epochs=getattr(config, "LIGHTFM_EPOCHS", 30),
        num_threads=getattr(config, "LIGHTFM_NUM_THREADS", 1),
    )

    logger.info("4/4. Lưu model và artifacts...")
    with open(os.path.join(config.MODEL_DIR, "lightfm_model.pkl"), "wb") as f:
        pickle.dump(model, f)
    with open(os.path.join(config.MODEL_DIR, "dataset_mapping.pkl"), "wb") as f:
        pickle.dump(dataset, f)
    with open(os.path.join(config.MODEL_DIR, "item_features_matrix.pkl"), "wb") as f:
        pickle.dump(item_features_matrix, f)
    with open(os.path.join(config.MODEL_DIR, "user_features_matrix.pkl"), "wb") as f:
        pickle.dump(user_features_matrix, f)

    logger.info("Huấn luyện hoàn tất thành công!")


if __name__ == "__main__":
    main()