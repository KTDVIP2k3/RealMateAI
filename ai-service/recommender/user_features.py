"""
User features cho LightFM hybrid model — kết hợp Khảo sát Investor + Hồ sơ Đầu tư (InvestmentProfileVersion).
"""
import logging
import pandas as pd

logger = logging.getLogger(__name__)

# Query khảo sát người dùng từ bảng investor JOIN account
USER_SURVEY_QUERY = """
    SELECT
        a.account_id                AS account_id,
        inv.investment_experience   AS investment_experience,
        inv.investment_goal         AS investment_goal,
        inv.investment_priority     AS investment_priority,
        inv.investment_style        AS investment_style,
        inv.return_expectation      AS return_expectation,
        inv.property_preference     AS property_preference,
        inv.decision_factor         AS decision_factor,
        inv.management_ability      AS management_ability,
        inv.investment_method       AS investment_method
    FROM investor inv
    JOIN account a ON inv.account_id = a.account_id
    WHERE inv.is_active = true
"""

# Query hồ sơ phiên bản đầu tư từ investment_profile_version
USER_PROFILE_QUERY = """
    SELECT
        a.account_id                AS account_id,
        ipv.wards                   AS wards,
        s.name                      AS strategy_name
    FROM investment_profile_version ipv
    JOIN investment_profile ip ON ipv.investment_profile_id = ip.investment_profile_id
    JOIN investor inv ON ip.investor_id = inv.investor_id
    JOIN account a ON a.account_id = inv.account_id
    LEFT JOIN strategy s ON s.strategy_id = ipv.strategy_id
    WHERE ipv.is_active = true
      AND ip.is_active = true
"""

SURVEY_FIELDS = [
    "investment_experience",
    "investment_goal",
    "investment_priority",
    "investment_style",
    "return_expectation",
    "property_preference",
    "decision_factor",
    "management_ability",
    "investment_method",
]


def fetch_user_survey(engine) -> pd.DataFrame:
    return pd.read_sql(USER_SURVEY_QUERY, engine)


def fetch_user_profile(engine) -> pd.DataFrame:
    return pd.read_sql(USER_PROFILE_QUERY, engine)


def build_user_feature_tags(survey_df: pd.DataFrame, profile_df: pd.DataFrame) -> dict:
    """
    Gộp đặc trưng khảo sát và hồ sơ phiên bản của từng account_id thành danh sách các tag/features.
    Trả về: dict {account_id: set(feature_tags)}
    """
    tags_per_user = {}

    # 1. Trích xuất từ Khảo sát (Investor survey)
    if not survey_df.empty:
        for _, row in survey_df.iterrows():
            acc_id = row["account_id"]
            if pd.isna(acc_id):
                continue
            acc_id = int(acc_id)
            user_tags = tags_per_user.setdefault(acc_id, set())

            for field in SURVEY_FIELDS:
                val = row.get(field)
                if pd.notna(val) and str(val).strip():
                    user_tags.add(f"{field}:{str(val).strip()}")

    # 2. Trích xuất từ Hồ sơ phiên bản (InvestmentProfileVersion)
    if not profile_df.empty:
        for _, row in profile_df.iterrows():
            acc_id = row["account_id"]
            if pd.isna(acc_id):
                continue
            acc_id = int(acc_id)
            user_tags = tags_per_user.setdefault(acc_id, set())

            strat = row.get("strategy_name")
            if pd.notna(strat) and str(strat).strip():
                user_tags.add(f"strategy_name:{str(strat).strip()}")

    return tags_per_user