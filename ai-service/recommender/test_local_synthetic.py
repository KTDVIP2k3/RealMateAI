"""
test_local_synthetic.py — Tự kiểm tra phần LOGIC TỔNG HỢP TRỌNG SỐ + USER
FEATURES (không cần kết nối Postgres thật, không cần cài lightfm) bằng dữ
liệu giả lập.

Chạy: python -m recommender.test_local_synthetic
"""
import numpy as np
import pandas as pd

from recommender.data_extraction import aggregate_interactions
from recommender.user_features import build_user_feature_tags
from recommender import config


def make_synthetic_events(n_users=20, n_items=15, n_events=300, seed=42) -> pd.DataFrame:
    rng = np.random.default_rng(seed)
    event_types = list(config.EVENT_WEIGHTS.keys())
    return pd.DataFrame({
        "account_id": rng.integers(1, n_users + 1, size=n_events),
        "event_type": rng.choice(event_types, size=n_events),
        "listing_id": rng.integers(1, n_items + 1, size=n_events),
        "created_at": pd.Timestamp.now(),
    })


def run():
    print("=== TEST 1: DataFrame rỗng không được crash ===")
    empty_result = aggregate_interactions(pd.DataFrame(columns=["account_id", "event_type", "listing_id", "created_at"]))
    assert empty_result.empty
    assert list(empty_result.columns) == ["account_id", "listing_id", "weight"]
    print("OK — input rỗng trả về đúng cấu trúc rỗng, không lỗi.\n")

    print("=== TEST 2: SEARCH (weight=0) phải bị loại hoàn toàn khỏi kết quả ===")
    only_search = pd.DataFrame({
        "account_id": [1, 1, 2],
        "event_type": ["SEARCH", "SEARCH", "SEARCH"],
        "listing_id": [10, 20, 10],
        "created_at": pd.Timestamp.now(),
    })
    result = aggregate_interactions(only_search)
    assert result.empty, f"Kỳ vọng rỗng vì toàn SEARCH (weight=0), nhưng ra: {result}"
    print("OK — SEARCH không được đưa vào interaction matrix.\n")

    print("=== TEST 3: Cộng dồn trọng số đúng khi user tương tác NHIỀU LẦN với CÙNG 1 listing ===")
    repeated = pd.DataFrame({
        "account_id": [1, 1, 1],
        "event_type": ["VIEW", "VIEW", "SAVE"],
        "listing_id": [99, 99, 99],
        "created_at": pd.Timestamp.now(),
    })
    result = aggregate_interactions(repeated)
    assert len(result) == 1
    weight = result.iloc[0]["weight"]
    assert weight == 7, f"Kỳ vọng weight=7 (1+1+5), nhưng ra {weight}"
    print(f"OK — weight cộng dồn đúng: VIEW(1) + VIEW(1) + SAVE(5) = {weight}\n")

    print("=== TEST 4: CONTACT phải có trọng số cao nhất ===")
    mixed = pd.DataFrame({
        "account_id": [1, 2],
        "event_type": ["CONTACT", "CLICK"],
        "listing_id": [1, 1],
        "created_at": pd.Timestamp.now(),
    })
    result = aggregate_interactions(mixed)
    contact_weight = result[result["account_id"] == 1]["weight"].iloc[0]
    click_weight = result[result["account_id"] == 2]["weight"].iloc[0]
    assert contact_weight > click_weight
    print(f"OK — CONTACT weight={contact_weight} > CLICK weight={click_weight}\n")

    print("=== TEST 5: Chạy full pipeline (không DB) với 300 event giả lập ===")
    synthetic = make_synthetic_events()
    result = aggregate_interactions(synthetic)
    print(f"  {synthetic['account_id'].nunique()} user, {synthetic['listing_id'].nunique()} listing giả lập")
    print(f"  -> sau khi tổng hợp: {len(result)} cặp (user, listing) có tương tác thật (weight > 0)")
    print(f"  -> weight nhỏ nhất={result['weight'].min()}, lớn nhất={result['weight'].max()}")
    assert (result["weight"] > 0).all()
    print("OK — pipeline chạy hết không lỗi với dữ liệu giả lập quy mô vừa.\n")

    print("=" * 60)
    print("TẤT CẢ TEST TỔNG HỢP TRỌNG SỐ PASS.")
    print("=" * 60)


def run_user_features_tests():
    print("=== TEST 6: build_user_feature_tags — gộp đúng khảo sát + hồ sơ đầu tư ===")
    survey_df = pd.DataFrame({
        "account_id": [1, 2],
        "investment_experience": ["Beginner", None],
        "investment_goal": ["CashFlow", "CapitalGain"],
        "investment_priority": [None, None],
        "investment_style": [None, None],
        "return_expectation": [None, None],
        "property_preference": ["Apartment", "Land"],
        "decision_factor": [None, None],
        "management_ability": [None, None],
        "investment_method": [None, None],
    })
    profile_df = pd.DataFrame({
        "account_id": [1, 1, 2],  # user 1 co 2 profile (2 dong)
        "risk_tolerance_level": ["Medium", "High", "Low"],
        "ward": ["WardA", "WardB", "WardC"],
        "strategy_name": ["DONG_TIEN", "LUOT_SONG", None],
    })

    tags = build_user_feature_tags(survey_df, profile_df)

    assert 1 in tags and 2 in tags, "Ca 2 user phai co tag"
    assert "investment_experience:Beginner" in tags[1]
    assert "investment_goal:CashFlow" in tags[1]
    # user 1 co 2 profile -> phai gom CA 2 risk_tolerance_level (khong ghi de)
    assert "risk_tolerance_level:Medium" in tags[1]
    assert "risk_tolerance_level:High" in tags[1]
    print(f"  user 1 tags: {tags[1]}")

    # user 2: investment_experience=None -> KHONG duoc co tag nay
    assert not any(t.startswith("investment_experience:") for t in tags[2])
    # user 2: strategy_name=None (do LEFT JOIN strategy khong khop) -> khong co tag strategy
    assert not any(t.startswith("strategy_name:") for t in tags[2])
    print(f"  user 2 tags: {tags[2]}")
    print("OK — gop dung, bo qua None/NaN, khong ghi de khi co nhieu profile.\n")

    print("=== TEST 7: User hoan toan chua khao sat/chua co ho so -> khong loi, tra dict rong ===")
    empty_survey = pd.DataFrame(columns=survey_df.columns)
    empty_profile = pd.DataFrame(columns=profile_df.columns)
    empty_tags = build_user_feature_tags(empty_survey, empty_profile)
    assert empty_tags == {}
    print("OK — DataFrame rong tra ve dict rong, khong crash.\n")

    print("=" * 60)
    print("TAT CA TEST USER FEATURES PASS.")
    print("=" * 60)


if __name__ == "__main__":
    run()
    run_user_features_tests()
    print("\n" + "=" * 60)
    print("TOAN BO 7 TEST PASS. Logic dung, san sang train voi DB that.")
    print("Buoc tiep theo:")
    print("  1. python -m recommender.train")
    print("  2. python -m recommender.generate_recommendations")
    print("=" * 60)
