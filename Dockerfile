################################################################################
# Stage 4: Stage CHẠY CUỐI CÙNG (Đã sửa tag image chuẩn của Microsoft)
FROM mcr.microsoft.com/playwright/java:v1.44.0-jammy AS final

USER root
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
WORKDIR /app

# Biến môi trường này báo cho lệnh cài đặt biết CHỈ tự động xử lý Chromium
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

# Tạo user appuser để chạy ứng dụng an toàn
ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser

# Copy các layer Spring Boot đã giải nén từ Stage 3
COPY --from=extract /build/target/extracted/dependencies/ ./
COPY --from=extract /build/target/extracted/spring-boot-loader/ ./
COPY --from=extract /build/target/extracted/snapshot-dependencies/ ./
COPY --from=extract /build/target/extracted/application/ ./

# Xóa sạch đống browser mặc định đi trước để giải phóng dung lượng (~1.5GB)
RUN rm -rf /ms-playwright/*

# Tiến hành CHỈ cài đặt lại duy nhất Chromium và các thư viện cần thiết của nó
RUN java -cp ".:./dependencies/*:./snapshot-dependencies/*:./application/*" com.microsoft.playwright.CLI install-deps chromium && \
    java -cp ".:./dependencies/*:./snapshot-dependencies/*:./application/*" com.microsoft.playwright.CLI install chromium

# Phân quyền lại thư mục và chuyển sang user appuser
RUN chmod -R 755 /ms-playwright && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
EXPOSE 8081

ENTRYPOINT [ "java", "org.springframework.boot.loader.launch.JarLauncher" ]