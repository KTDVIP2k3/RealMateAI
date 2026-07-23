################################################################################
# Stage 1: Build và giải nén Spring Boot Layers (Dùng Maven)
################################################################################
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml .
COPY src ./src

# Gộp Build + Giải nén JAR + Xóa sạch cache Maven (.m2) trong CÙNG 1 LAYER
RUN mvn clean package -DskipTests && \
    java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted && \
    rm -rf ~/.m2/repository target/*.jar

################################################################################
# Stage 2: Tầng chạy ứng dụng cuối cùng (Cài Java + Node.js + Playwright Chromium)
################################################################################
FROM eclipse-temurin:17-jre-jammy AS final
WORKDIR /app

# Thiết lập múi giờ hệ thống & Biến môi trường nhận diện Docker cho App Java
ENV TZ=Asia/Ho_Chi_Minh
ENV DOCKER=true
# Chỉ cài Chromium, bỏ qua việc tải Webkit/Firefox
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=0
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

USER root

# 1. Cài đặt ĐẦY ĐỦ các thư viện hệ thống cần thiết cho Chromium + Node.js 20
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    ca-certificates \
    gnupg \
    # --- Dependencies đồ họa & giao diện căn bản ---
    libgdk-pixbuf-2.0-0 \
    libnss3 \
    libnspr4 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libxkbcommon0 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libpango-1.0-0 \
    libcairo2 \
    libasound2 \
    libxcursor1 \
    libgtk-3-0 \
    libpangocairo-1.0-0 \
    libcairo-gobject2 \
    # --- Dependencies bổ sung bị thiếu gây ra lỗi Host Validation Warning ---
    libgstreamer-1.0-0 \
    libgstreamer-plugins-base1.0-0 \
    libatomic1 \
    libxslt1.1 \
    libwoff2dec1.0.2 \
    libvpx7 \
    libevent-2.1-7 \
    libopus0 \
    libwebpdemux2 \
    libharfbuzz-icu0 \
    libenchant-2-2 \
    libsecret-1-0 \
    libhyphen0 \
    libgles2 \
    libx264-dev \
    && mkdir -p /etc/apt/keyrings \
    && curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg \
    && echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_20.x nodistro main" | tee /etc/apt/sources.list.d/nodesource.list \
    && apt-get update && apt-get install -y --no-install-recommends \
    nodejs \
    && rm -rf /var/lib/apt/lists/*

# 2. Cài đặt Playwright và CHỈ tải duy nhất trình duyệt Chromium
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
RUN npm install -g playwright && \
    npx playwright install chromium && \
    npm cache clean --force

# 3. Khởi tạo user bảo mật (Non-root) và cấp quyền thư mục làm việc + cache Chromium
ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser && \
    mkdir -p /tmp/.org.chromium.Chromium /tmp/chrome-profile-bot /tmp/chrome-crashes && \
    chown -R appuser:appuser /ms-playwright && \
    chown -R appuser:appuser /app && \
    chown -R appuser:appuser /tmp/.org.chromium.Chromium && \
    chown -R appuser:appuser /tmp/chrome-profile-bot && \
    chown -R appuser:appuser /tmp/chrome-crashes

USER appuser

# 4. Sao chép các lớp (Layers) đã giải nén từ tầng `builder`
COPY --from=builder /build/target/extracted/dependencies/ ./
COPY --from=builder /build/target/extracted/spring-boot-loader/ ./
COPY --from=builder /build/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/target/extracted/application/ ./

EXPOSE 8080

ENTRYPOINT [ "java", "-Duser.timezone=Asia/Ho_Chi_Minh", "org.springframework.boot.loader.launch.JarLauncher" ]