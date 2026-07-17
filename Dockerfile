################################################################################
# Stage CHẠY CUỐI CÙNG: Môi trường Playwright Java
FROM mcr.microsoft.com/playwright/java:v1.44.0-jammy AS final

USER root

# Đường dẫn lưu trình duyệt của image Microsoft Playwright
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

# Tạo thư mục làm việc cụ thể cho ứng dụng để quản lý quyền dễ hơn
WORKDIR /app

ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser

# Cấp quyền cho appuser đọc thư mục chứa trình duyệt và thư mục /app
RUN chmod -R 755 /ms-playwright && chown -R appuser:appuser /app

USER appuser

# THÊM --chown=appuser:appuser để tránh lỗi Permission Denied khi chạy JarLauncher
COPY --from=extract --chown=appuser:appuser build/target/extracted/dependencies/ ./
COPY --from=extract --chown=appuser:appuser build/target/extracted/spring-boot-loader/ ./
COPY --from=extract --chown=appuser:appuser build/target/extracted/snapshot-dependencies/ ./
COPY --from=extract --chown=appuser:appuser build/target/extracted/application/ ./

EXPOSE 8080

ENTRYPOINT [ "java", "org.springframework.boot.loader.launch.JarLauncher" ]