################################################################################
# Stage 1: Build source code bằng Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy file pom.xml và tải dependencies trước để tận dụng cache
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy toàn bộ mã nguồn vào và build file JAR
COPY src ./src
RUN mvn clean package -DskipTests

################################################################################
# Stage 2: Giải nén file JAR thành các lớp layer (Tối ưu hóa Docker layer)
FROM eclipse-temurin:17-jre-jammy AS extract
WORKDIR /build
# Copy file JAR từ Stage builder sang để giải nén
COPY --from=builder /build/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract --destination target/extracted

################################################################################
# Stage 3: Tầng chạy ứng dụng cuối cùng (Sử dụng JRE + Cài Playwright tối ưu)
FROM eclipse-temurin:17-jre-jammy AS final
WORKDIR /app

# 1. Cài đặt các thư viện hệ thống cần thiết cho Playwright chạy ngầm
USER root
RUN apt-get update && apt-get install -y --no-install-recommends \
    nodejs \
    npm \
    && rm -rf /var/lib/apt/lists/*

# 2. Cài đặt Playwright và CHỈ tải duy nhất trình duyệt chromium (Tiết kiệm dung lượng VPS)
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

# 4. Sao chép các lớp ứng dụng Spring Boot đã giải nén từ Stage `extract`
COPY --from=extract /build/target/extracted/dependencies/ ./
COPY --from=extract /build/target/extracted/spring-boot-loader/ ./
COPY --from=extract /build/target/extracted/snapshot-dependencies/ ./
COPY --from=extract /build/target/extracted/application/ ./

EXPOSE 8080

ENTRYPOINT [ "java", "org.springframework.boot.loader.launch.JarLauncher" ]