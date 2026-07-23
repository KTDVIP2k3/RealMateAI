################################################################################
# Stage 1: Build và giải nén Spring Boot Layers (Dùng Maven)
################################################################################
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests && \
    java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted && \
    rm -rf ~/.m2/repository target/*.jar

################################################################################
# Stage 2: Tầng chạy ứng dụng (Sử dụng Image có sẵn toàn bộ Deps cho Playwright)
################################################################################
FROM eclipse-temurin:17-jre-jammy AS final
WORKDIR /app

ENV TZ=Asia/Ho_Chi_Minh
ENV DOCKER=true
# Chỉ định thư mục lưu trữ Browser cố định cho Playwright Java
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

USER root

# 1. Cài đặt toàn bộ dependencies hệ thống mà Playwright yêu cầu (bao gồm cả GTK, Cairo, X11...)
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    ca-certificates \
    gnupg \
    libxcursor1 \
    libgtk-3-0 \
    libpangocairo-1.0-0 \
    libcairo-gobject2 \
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
    libxslt1.1 \
    libevent-2.1-7 \
    libopus0 \
    && rm -rf /var/lib/apt/lists/*

# 2. Cài Node.js 20 & Tải trước Chromium ngay trong quá trình Build Image
RUN mkdir -p /etc/apt/keyrings \
    && curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg \
    && echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_20.x nodistro main" | tee /etc/apt/sources.list.d/nodesource.list \
    && apt-get update && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/* \
    && npx --yes playwright@1.44.0 install chromium

# 3. Tạo user và Phân quyền TRUY CẬP ĐẦY ĐỦ cho thư mục Browser & Thư mục Tạm
ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser && \
    mkdir -p /ms-playwright /tmp && \
    chown -R appuser:appuser /ms-playwright && \
    chown -R appuser:appuser /app && \
    chmod -R 777 /tmp

USER appuser

# 4. Copy Spring Boot Layers
COPY --from=builder /build/target/extracted/dependencies/ ./
COPY --from=builder /build/target/extracted/spring-boot-loader/ ./
COPY --from=builder /build/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/target/extracted/application/ ./

EXPOSE 8080

ENTRYPOINT [ "java", "-Duser.timezone=Asia/Ho_Chi_Minh", "org.springframework.boot.loader.launch.JarLauncher" ]