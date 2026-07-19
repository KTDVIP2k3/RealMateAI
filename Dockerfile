################################################################################
# Stage 1: Build và giải nén Spring Boot Layers (Dùng Maven)
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

# Tận dụng cache của Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build dự án
COPY src ./src
RUN mvn clean package -DskipTests

# Giải nén file JAR ngay tại tầng này sang thư mục target/extracted
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

################################################################################
# Stage 2: Tầng chạy ứng dụng cuối cùng (Cài Java + Node.js + Playwright)
FROM eclipse-temurin:17-jre-jammy AS final
WORKDIR /app

# 1. Cài đặt các thư viện hệ thống và Node.js 20 chính thức cho Playwright
USER root
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

# 2. Cài đặt Playwright và CHỈ tải duy nhất trình duyệt chromium
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
RUN npm install -g playwright && \
    npx playwright install chromium && \
    npx playwright install-deps chromium && \
    npm cache clean --force

# 3. Khởi tạo user bảo mật (Non-root) và phân quyền thư mục browser
ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser && \
    chown -R appuser:appuser /ms-playwright && \
    chown -R appuser:appuser /app

USER appuser

# 4. Sao chép trực tiếp từ tầng `builder` (Đã đổi tên nguồn từ --from=extract thành --from=builder)
COPY --from=builder /build/target/extracted/dependencies/ ./
COPY --from=builder /build/target/extracted/spring-boot-loader/ ./
COPY --from=builder /build/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/target/extracted/application/ ./

EXPOSE 8080

ENTRYPOINT [ "java", "org.springframework.boot.loader.launch.JarLauncher" ]