# Stage 1: Build source code bằng Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /build
# Copy file cấu hình pom.xml và source code vào
COPY pom.xml .
COPY src ./src
# Build ra file jar (bỏ qua chạy thử test để tăng tốc và tiết kiệm đĩa)
RUN mvn clean package -DskipTests

# Stage 2: Giải nén file JAR theo cấu trúc Layer của Spring Boot
FROM eclipse-temurin:17-jre-alpine AS extract
WORKDIR /build
# Copy file jar vừa build thành công ở Stage 1 sang đây
COPY --from=build /build/target/*.jar app.jar
# Chạy lệnh layertools của Spring Boot để giải nén các layer ra thư mục hiện tại (/build)
RUN java -Djarmode=layertools -jar app.jar extract

################################################################################
# Stage 3: Khởi chạy môi trường Playwright & Spring Boot (Tiết kiệm đĩa)
FROM mcr.microsoft.com/playwright/java:v1.44.0-jammy AS final

USER root
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
WORKDIR /app

# Chặn tạm thời lệnh tự động tải của Playwright lúc khởi tạo
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

# SỬA ĐƯỜNG DẪN CHUẨN: Copy các layer từ thư mục gốc /build của Stage "extract" sang
COPY --from=extract /build/dependencies/ ./
COPY --from=extract /build/spring-boot-loader/ ./
COPY --from=extract /build/snapshot-dependencies/ ./
COPY --from=extract /build/application/ ./

# Dọn sạch 1.5GB các browser mặc định (Firefox/WebKit) đi kèm image của Microsoft
RUN rm -rf /ms-playwright/*

# Tiến hành cài duy nhất Chromium và các thư viện hỗ trợ (dependencies hệ thống) của nó
RUN java -cp ".:./dependencies/*:./snapshot-dependencies/*:./application/*" com.microsoft.playwright.CLI install-deps chromium && \
    java -cp ".:./dependencies/*:./snapshot-dependencies/*:./application/*" com.microsoft.playwright.CLI install chromium

# Phân quyền lại thư mục chạy và đổi sang quyền user thường
RUN chmod -R 755 /ms-playwright && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
EXPOSE 8081

# Lệnh kích hoạt Launcher của Spring Boot 3.x
ENTRYPOINT [ "java", "org.springframework.boot.loader.launch.JarLauncher" ]