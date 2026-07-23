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
# Stage 2: Tầng chạy ứng dụng cuối cùng (Cài Java + Node.js + Playwright Chromium)
################################################################################
FROM eclipse-temurin:17-jre-jammy AS final
WORKDIR /app

ENV TZ=Asia/Ho_Chi_Minh
ENV DOCKER=true
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=0
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

USER root

# 1. Cài đặt Curl, GPG và Node.js 20
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    ca-certificates \
    gnupg \
    && mkdir -p /etc/apt/keyrings \
    && curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg \
    && echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_20.x nodistro main" | tee /etc/apt/sources.list.d/nodesource.list \
    && apt-get update && apt-get install -y --no-install-recommends \
    nodejs \
    && rm -rf /var/lib/apt/lists/*

# 2. Cài Playwright, Tải Chromium & Tự động cài ĐÚNG TÊN dependencies cho Chromium
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
RUN npm install -g playwright && \
    npx playwright install chromium && \
    npx playwright install-deps chromium && \
    npm cache clean --force

# 3. Khởi tạo user bảo mật (Non-root) và cấp quyền các thư mục tạm
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

# 4. Sao chép các lớp (Layers) từ tầng builder
COPY --from=builder /build/target/extracted/dependencies/ ./
COPY --from=builder /build/target/extracted/spring-boot-loader/ ./
COPY --from=builder /build/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/target/extracted/application/ ./

EXPOSE 8080

ENTRYPOINT [ "java", "-Duser.timezone=Asia/Ho_Chi_Minh", "org.springframework.boot.loader.launch.JarLauncher" ]