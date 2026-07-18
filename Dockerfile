# syntax=docker/dockerfile:1

################################################################################
# Stage 1: Tải các dependencies (Dùng bản alpine siêu nhẹ)
FROM maven:3.9.6-eclipse-temurin-17-alpine AS deps
WORKDIR /build

COPY --chmod=0755 mvnw mvnw
COPY .mvn/ .mvn/

RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 ./mvnw dependency:go-offline -DskipTests

################################################################################
# Stage 2: Build code Spring Boot ra file Jar (Dùng bản alpine siêu nhẹ)
FROM maven:3.9.6-eclipse-temurin-17-alpine AS package
WORKDIR /build

COPY --chmod=0755 mvnw mvnw
COPY .mvn/ .mvn/
COPY ./src src/

RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests && \
    mv target/$(./mvnw help:evaluate -Dexpression=project.artifactId -q -DforceStdout)-$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout).jar target/app.jar

################################################################################
# Stage 3: Giải nén các Layer của Spring Boot
FROM eclipse-temurin:17-jre-alpine AS extract
WORKDIR /build

COPY --from=package /build/target/app.jar target/app.jar
RUN java -Djarmode=layertools -jar target/app.jar extract --destination target/extracted

################################################################################
# Stage 4: Stage CHẠY CUỐI CÙNG (Đã tối ưu chỉ cài Chromium để tiết kiệm đĩa)
# Sử dụng bản no-browsers (chưa có sẵn browser nào) để tự chọn browser cần cài
FROM mcr.microsoft.com/playwright/java:v1.44.0-jammy-nobrowsers AS final

USER root
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
WORKDIR /app

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

# Dùng Playwright CLI chạy qua Java để CHỈ cài đặt Chromium và các dependencies hệ thống của nó
RUN java -cp ".:./dependencies/*:./snapshot-dependencies/*:./application/*" com.microsoft.playwright.CLI install-deps chromium && \
    java -cp ".:./dependencies/*:./snapshot-dependencies/*:./application/*" com.microsoft.playwright.CLI install chromium

# Phân quyền lại thư mục và chuyển sang user appuser
RUN chmod -R 755 /ms-playwright && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
EXPOSE 8081

ENTRYPOINT [ "java", "org.springframework.boot.loader.launch.JarLauncher" ]