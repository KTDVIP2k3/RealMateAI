"""
user_features.py — Lấy đặc trưng NGƯỜI DÙNG (không chỉ đặc trưng listing như
item_features.py) từ 2 nguồn:
  1. Investor (bảng khảo sát khẩu vị đầu tư — investment_experience,
     investment_goal, property_preference, risk-related fields...)
  2. InvestmentProfileVersion đang active gần nhất (hồ sơ/kế hoạch đầu tư —
     risk_tolerance_level, ward quan tâm, strategy, expected_roi...)

Mục đích: chuyển LightFM từ mô hình THUẦN collaborative-filtering (chỉ học
từ lịch sử tương tác ActiveLog) sang HYBRID MODEL — kết hợp cả:
  - Collaborative signal: user từng xem/lưu/liên hệ listing nào (như cũ)
  - Content signal: user khai báo mình thích loại BĐS gì, khu vực nào, khẩu
    vị rủi ro ra sao (khảo sát + hồ sơ đầu tư)

Lợi ích rõ nhất: xử lý COLD-START tốt hơn nhiều — 1 investor MỚI đăng ký,
CHƯA có bất kỳ tương tác nào (ActiveLog rỗng) nhưng ĐÃ khai báo khảo sát +
tạo 1 kế hoạch đầu tư, vẫn nhận được gợi ý phù hợp ngay từ lần đầu tiên,
thay vì rơi vào nhánh "chưa đủ dữ liệu" như trước.

Đã đối chiếu đúng schema thật (backup1.sql) — mọi tên bảng/cột dưới đây đều
khớp 100% với DB thật, không cần chỉnh sửa gì thêm.
"""
import pandas as pd

SURVEY_FEATURES_QUERY = """
    SELECT
        a.account_id                 AS account_id,
        i.investment_experience      AS investment_experience,
        i.investment_goal            AS investment_goal,
        i.investment_priority        AS investment_priority,
        i.investment_style           AS investment_style,
        i.return_expectation         AS return_expectation,
        i.property_preference        AS property_preference,
        i.decision_factor            AS decision_factor,
        i.management_ability         AS management_ability,
        i.investment_method          AS investment_method
    FROM investor i
    JOIN account a ON a.account_id = i.account_id
"""

# InvestmentProfileVersion đang ACTIVE gần nhất của MỖI InvestmentProfile
# thuộc investor đó — 1 investor có thể có NHIỀU InvestmentProfile song song
# (VD "đầu tư dài hạn" + "lướt sóng ngắn hạn"), lấy TẤT CẢ (không chỉ 1 cái)
# vì mỗi profile phản ánh 1 khẩu vị/chiến lược khác nhau, đều là tín hiệu hữu
# ích cho gợi ý — không cố ép về 1 profile "đại diện" duy nhất.
PROFILE_FEATURES_QUERY = """
    SELECT
        a.account_id                       AS account_id,
        ipv.risk_tolerance_level            AS risk_tolerance_level,
        ipv.ward                            AS ward,
        s.name                              AS strategy_name
    FROM investment_profile_version ipv
    JOIN investment_profile ip ON ip.investment_profile_id = ipv.investment_profile_id
    JOIN investor i ON i.investor_id = ip.investor_id
    JOIN account a ON a.account_id = i.account_id
    LEFT JOIN strategy s ON s.strategy_id = ipv.strategy_id
    WHERE ipv.is_active = true
      AND ip.is_active = true
"""


def fetch_survey_features(engine) -> pd.DataFrame:
    return pd.read_sql(SURVEY_FEATURES_QUERY, engine)


def fetch_profile_features(engine) -> pd.DataFrame:
    return pd.read_sql(PROFILE_FEATURES_QUERY, engine)


def build_user_feature_tags(survey_df: pd.DataFrame, profile_df: pd.DataFrame) -> dict:
    """
    Gộp 2 nguồn (khảo sát + hồ sơ đầu tư) thành dict {account_id: [tag, tag, ...]}
    — mỗi tag dạng "fieldname:value", bỏ qua field null/rỗng (investor có thể
    chưa điền hết khảo sát, hoặc chưa tạo kế hoạch đầu tư nào — không lỗi,
    chỉ đơn giản có ÍT tag hơn cho user đó).
    """
    tags_by_account: dict[int, set[str]] = {}

    def add_tag(account_id, field_name, value):
        # Giá trị rỗng trong DataFrame đọc qua itertuples có thể là None
        # (Python) HOẶC NaN (float, cách pandas biểu diễn NULL cho cột dạng
        # object/mixed) — pd.isna() bắt được CẢ 2 trường hợp, thiếu kiểm tra
        # NaN sẽ lọt tag rác dạng "field:nan" (bug thật đã bắt được qua test).
        if pd.isna(value):
            return
        if isinstance(value, str) and not value.strip():
            return
        tags_by_account.setdefault(account_id, set()).add(f"{field_name}:{value}")

    survey_cols = [
        "investment_experience", "investment_goal", "investment_priority",
        "investment_style", "return_expectation", "property_preference",
        "decision_factor", "management_ability", "investment_method",
    ]
    for row in survey_df.itertuples():
        for col in survey_cols:
            add_tag(row.account_id, col, getattr(row, col))

    profile_cols = ["risk_tolerance_level", "ward", "strategy_name"]
    for row in profile_df.itertuples():
        # 1 investor có thể có NHIỀU dòng (nhiều InvestmentProfile) — add_tag
        # dùng set() nên tự động loại trùng nếu 2 profile cùng risk level.
        for col in profile_cols:
            add_tag(row.account_id, col, getattr(row, col))

    return {k: sorted(v) for k, v in tags_by_account.items()}
