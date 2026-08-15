"""
Item features cho LightFM hybrid model — property_type + province làm
feature — có sẵn, ổn định, mang nhiều thông tin về "loại tài sản" hơn là chỉ
dựa vào lịch sử tương tác thuần (collaborative filtering) — quan trọng với
listing MỚI chưa có ai xem (cold-start item). Đã đối chiếu đúng schema thật
(backup1.sql).
"""
import pandas as pd

ITEM_FEATURES_QUERY = """
    SELECT
        l.listing_id        AS listing_id,
        pt.name              AS property_type_name,
        p.province_code      AS province_code
    FROM listing l
    JOIN property prop ON l.property_id = prop.property_id
    LEFT JOIN property_type pt ON prop.property_type_id = pt.property_type_id
    LEFT JOIN location loc ON prop.location_id = loc.location_id
    LEFT JOIN ward w ON loc.ward_code = w.ward_code
    LEFT JOIN province p ON w.province_code = p.province_code
    WHERE l.is_active = true
"""


def fetch_item_features(engine) -> pd.DataFrame:
    df = pd.read_sql(ITEM_FEATURES_QUERY, engine)
    df["property_type_name"] = df["property_type_name"].fillna("UNKNOWN_TYPE")
    df["province_code"] = df["province_code"].fillna("UNKNOWN_PROVINCE")
    return df


def build_feature_list(row) -> list:
    """Mỗi listing có 2 feature dạng chuỗi: 'type:<tên loại>' và 'province:<mã tỉnh>'."""
    return [f"type:{row['property_type_name']}", f"province:{row['province_code']}"]
