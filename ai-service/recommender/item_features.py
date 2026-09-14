"""
item_features.py — Trích xuất và xử lý đặc trưng cho từng Bất động sản (Listings/Items).
"""
import logging
import pandas as pd

logger = logging.getLogger(__name__)

ITEM_FEATURES_QUERY = """
    SELECT
        l.listing_id,
        pt.name AS property_type,
        p.price,
        p.area,
        p.bedroom AS num_bedrooms,
        p.bathroom AS num_bathrooms,
        pr.name AS province_name,
        w.name AS district_name
    FROM public.listing l
    JOIN public.property p ON l.property_id = p.property_id
    LEFT JOIN public.property_type pt ON p.property_type_id = pt.property_type_id
    LEFT JOIN public.location loc ON p.location_id = loc.location_id
    LEFT JOIN public.ward w ON loc.ward_code = w.ward_code
    LEFT JOIN public.province pr ON w.province_code = pr.province_code
    WHERE l.is_active = true
"""


def fetch_item_features(engine) -> pd.DataFrame:
    """
    Truy vấn thông tin đặc trưng của các Listing từ Database.
    """
    return pd.read_sql(ITEM_FEATURES_QUERY, engine)


def build_feature_list(row) -> list:
    """
    Chuyển đổi từng dòng thông tin của Listing thành danh sách các feature tags dạng chuỗi.
    """
    features = []

    if pd.notna(row.get("property_type")):
        features.append(f"property_type:{row['property_type']}")

    if pd.notna(row.get("province_name")):
        features.append(f"province:{row['province_name']}")

    if pd.notna(row.get("district_name")):
        features.append(f"district:{row['district_name']}")

    return features


def build_item_feature_tags(item_df: pd.DataFrame) -> dict:
    """
    Tạo dictionary chứa danh sách feature tags cho từng listing_id.
    Trả về: {listing_id: set(feature_tags)}
    """
    item_tags = {}
    if item_df.empty:
        return item_tags

    for _, row in item_df.iterrows():
        item_id = row.get("listing_id")
        if pd.isna(item_id):
            continue
        item_tags[int(item_id)] = set(build_feature_list(row))

    return item_tags