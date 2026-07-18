################################################################################
# Stage 3: Khởi chạy môi trường Playwright & Spring Boot (Tiết kiệm đĩa)
FROM mcr.microsoft.com/playwright/java:v1.44.0-jammy AS final

USER root
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
WORKDIR /app

# Chặn tạm thời lệnh tự động tải của Playwright lúc khởi tạo ban đầu
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

# Tạo user appuser để chạy ứng dụng an toàn không cần root
ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser

# SỬA ĐỂ COPY TOÀN BỘ LAYER ĐÚNG CẤU TRÚC (Bao gồm cả thư mục cha của Spring Boot 3.x)
COPY --from=extract /build/dependencies/ ./dependencies/
COPY --from=extract /build/spring-boot-loader/ ./spring-boot-loader/
COPY --from=extract /build/snapshot-dependencies/ ./snapshot-dependencies/
COPY --from=extract /build/application/ ./application/

# Dọn sạch 1.5GB các browser mặc định (Firefox/WebKit) đi kèm image của Microsoft để tiết kiệm dung lượng
RUN rm -rf /ms-playwright/*

# === THAY ĐỔI QUAN TRỌNG Ở ĐÂY ===
# Sử dụng trực tiếp lệnh CLI hợp lệ có sẵn trong base image của Microsoft để cài đặt Chromium
RUN npx playwright install-deps chromium && \
    npx playwright install chromium

# Phân quyền lại thư mục chạy và đổi sang quyền user thường
RUN chmod -R 755 /ms-playwright && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
EXPOSE 8081

# Lệnh kích hoạt Launcher của Spring Boot 3.x (Sửa lại classpath trỏ vào folder application)
ENTRYPOINT [ "java", "-cp", "application:dependencies:spring-boot-loader:snapshot-dependencies", "org.springframework.boot.loader.launch.JarLauncher" ]