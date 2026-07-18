# Stage 4: Stage CHẠY CUỐI CÙNG - TỐI ƯU CÀI ĐẶT PLAYWRIGHT TRỰC TIẾP
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

# Copy các layer Spring Boot từ stage 'extract' sang
COPY --from=extract build/target/extracted/dependencies/ ./
COPY --from=extract build/target/extracted/spring-boot-loader/ ./
COPY --from=extract build/target/extracted/snapshot-dependencies/ ./
COPY --from=extract build/target/extracted/application/ ./

# Gọi Playwright CLI thông qua JarLauncher / Classpath tổng hợp để tự động nhận diện và giải nén driver
RUN java -cp "dependencies/*:spring-boot-loader/*:snapshot-dependencies/*:application/*" com.microsoft.playwright.CLI install

# Cấp lại quyền chuẩn chỉnh cho appuser đọc thư mục chứa trình duyệt và thư mục /app
RUN chmod -R 755 /ms-playwright && chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

ENTRYPOINT [ "java", "org.springframework.boot.loader.launch.JarLauncher" ]