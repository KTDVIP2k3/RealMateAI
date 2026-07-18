################################################################################
# Stage 3: Tầng chạy ứng dụng cuối cùng (Sử dụng JRE + Cài Playwright tối ưu)
FROM eclipse-temurin:17-jre-jammy AS final
WORKDIR /app

# 1. Cài đặt các thư viện hệ thống cần thiết và Node.js 20 (LTS) chính thức
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