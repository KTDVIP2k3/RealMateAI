"""
Trích xuất interaction data từ audit_log JOIN active_log, tổng hợp trọng số
theo (account_id, listing_id), trả về pandas DataFrame sẵn sàng để build
interaction matrix cho LightFM.

Tách riêng module này (không phụ thuộc lightfm) để có thể unit-test độc lập
phần logic tổng hợp trọng số mà không cần cài lightfm hay kết nối DB thật —
xem test_local_synthetic.py.
"""
import logging

import pandas as pd

from . import config

logger = logging.getLogger(__name__)

# Câu SQL lấy toàn bộ event có gắn listing_id cụ thể (bỏ SEARCH vì
# active_log.listing_id = null với event SEARCH — không có ý nghĩa cho ma
# trận user-item). Đã đối chiếu đúng với schema thật (backup1.sql).
INTERACTIONS_QUERY = """
    SELECT
        al1.account_id  AS account_id,
        al2.event_type  AS event_type,
        al2.listing_id  AS listing_id,
        al2.created_at  AS created_at
    FROM active_log al2
    JOIN audit_log al1 ON al2.audit_log_id = al1.audit_log_id
    WHERE al2.event_type IS NOT NULL
      AND al2.listing_id IS NOT NULL
      AND al1.account_id IS NOT NULL
"""


def fetch_raw_events(engine) -> pd.DataFrame:
    """Đọc thẳng từ DB — trả về DataFrame thô, mỗi dòng là 1 event."""
    df = pd.read_sql(INTERACTIONS_QUERY, engine)
    logger.info("Đã đọc %d dòng event thô từ DB", len(df))
    return df


def aggregate_interactions(raw_events: pd.DataFrame) -> pd.DataFrame:
    """
    Tổng hợp weighted-sum theo (account_id, listing_id) — cùng 1 user xem
    lại cùng 1 listing nhiều lần, hoặc vừa xem vừa lưu yêu thích, thì cộng dồn
    trọng số lại (không lấy max) để phản ánh đúng mức độ quan tâm tích luỹ.

    Input:  DataFrame với cột [account_id, event_type, listing_id, created_at]
    Output: DataFrame với cột [account_id, listing_id, weight]
    """
    if raw_events.empty:
        return pd.DataFrame(columns=["account_id", "listing_id", "weight"])

    df = raw_events.copy()
    df["weight"] = df["event_type"].map(config.EVENT_WEIGHTS).fillna(0)

    # SEARCH (weight=0) không đóng góp gì cho ma trận — loại bỏ sớm để
    # tránh những user CHỈ có SEARCH bị tính là "không có tương tác" nhưng
    # vẫn tốn bộ nhớ giữ lại dòng vô ích.
    df = df[df["weight"] > 0]

    aggregated = (
        df.groupby(["account_id", "listing_id"], as_index=False)["weight"]
        .sum()
    )
    logger.info(
        "Tổng hợp xong: %d user, %d listing, %d cặp (user,item) có tương tác",
        aggregated["account_id"].nunique(),
        aggregated["listing_id"].nunique(),
        len(aggregated),
    )
    return aggregated
