# Stage 1: Build (Ví dụ thôi nhé, hãy giữ nguyên cấu hình cũ của bạn)
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /build
# ... các lệnh build của bạn ...

# Stage 2: Extract (ĐẢM BẢO có chữ "AS extract" viết thường y chang thế này)
FROM eclipse-temurin:17-jre-alpine AS extract
WORKDIR /build
# Lệnh giải nén của bạn (ví dụ: RUN java -Djarmode=layertools -jar target/*.jar extract)
# Hãy chắc chắn thư mục giải nén nằm đúng trong /build/target/extracted/...

################################################################################
# Stage 4: Stage CHẠY CUỐI CÙNG (Giữ nguyên đoạn mình vừa sửa)
FROM mcr.microsoft.com/playwright/java:v1.44.0-jammy AS final

USER root
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
WORKDIR /app

ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser

# Các lệnh copy này sẽ hoạt động vì đã tìm thấy đúng "AS extract" ở trên
COPY --from=extract /build/target/extracted/dependencies/ ./
COPY --from=extract /build/target/extracted/spring-boot-loader/ ./
COPY --from=extract /build/target/extracted/snapshot-dependencies/ ./
COPY --from=extract /build/target/extracted/application/ ./

RUN rm -rf /ms-playwright/*

RUN java -cp ".:./dependencies/*:./snapshot-dependencies/*:./application/*" com.microsoft.playwright.CLI install-deps chromium && \
    java -cp ".:./dependencies/*:./snapshot-dependencies/*:./application/*" com.microsoft.playwright.CLI install chromium

RUN chmod -R 755 /ms-playwright && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
EXPOSE 8081

ENTRYPOINT [ "java", "org.springframework.boot.loader.launch.JarLauncher" ]