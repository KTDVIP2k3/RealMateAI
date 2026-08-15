"""
Cấu hình kết nối PostgreSQL — đọc từ biến môi trường (.env cho local, hoặc
biến môi trường Docker Compose truyền thẳng cho server). Dùng chung cho toàn
bộ recommender package.
"""
import os
from urllib.parse import quote_plus

try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass  # python-dotenv là optional — nếu không cài, đọc thẳng từ os.environ

DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "RealmateAi")
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASSWORD = os.getenv("DB_PASSWORD", "postgres")

# DB_USER có thể là email (VD "abc@fpt.edu.vn") — ký tự "@" bên trong username
# làm chuỗi kết nối ghép thô bị parse SAI vị trí phân cách user:password@host
# (2 dấu "@" trong 1 URL). Bắt buộc mã hoá (URL-encode) DB_USER/DB_PASSWORD
# bằng quote_plus() trước khi ghép — áp dụng luôn cho DB_PASSWORD vì mật khẩu
# cũng có thể chứa ký tự đặc biệt (@, #, %, khoảng trắng...) gây lỗi tương tự.
SQLALCHEMY_URL = (
    f"postgresql+psycopg2://{quote_plus(DB_USER)}:{quote_plus(DB_PASSWORD)}"
    f"@{DB_HOST}:{DB_PORT}/{DB_NAME}"
)

# ── Trọng số sự kiện — theo đúng thiết kế trong RealMateAI_AI_Architecture.md ──
# SHARE không có trong tài liệu gốc — tạm đặt 3 (giữa CLICK=2 và SAVE=5) vì
# hành động "chia sẻ" thể hiện mức quan tâm cao hơn click nhưng chưa chắc
# bằng việc lưu yêu thích. Điều chỉnh lại nếu team có số liệu thực tế khác.
EVENT_WEIGHTS = {
    "VIEW": 1,
    "CLICK": 2,
    "SHARE": 3,
    "SAVE": 5,
    "CONTACT": 10,
    "SEARCH": 0,  # SEARCH không gắn với 1 listing cụ thể -> không đưa vào interaction matrix
}

MODEL_DIR = os.getenv("MODEL_DIR", os.path.join(os.path.dirname(__file__), "..", "models"))
TOP_N = int(os.getenv("TOP_N", "10"))
