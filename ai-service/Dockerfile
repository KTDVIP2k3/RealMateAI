# ai-service/Dockerfile — build image chạy được cả train.py lẫn generate_recommendations.py
FROM python:3.11-slim

# lightfm cần compiler C (gcc) để build lúc pip install — base image "slim"
# không có sẵn, phải cài thêm build-essential.
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY recommender/ ./recommender/

# .env KHÔNG copy vào image (chứa secret) — trên server, biến môi trường
# được truyền thẳng qua "environment:" trong compose.yaml (đọc từ GitHub
# Actions Secrets lúc CI/CD tạo .env cho Spring Boot — dùng CHUNG, xem
# compose.yaml gộp + deloy.yml).

# Không có CMD cố định — image này dùng để chạy 2 lệnh KHÁC NHAU (train và
# generate_recommendations) theo "command:" khai báo riêng cho từng service
# trong compose.yaml (recommender-train / recommender-generate).
