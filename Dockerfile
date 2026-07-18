# syntax=docker/dockerfile:1

################################################################################
# Stage 1: Tải các dependencies về trước sử dụng image Playwright Java
FROM mcr.microsoft.com/playwright/java:v1.44.0-jammy as deps

WORKDIR /build

COPY --chmod=0755 mvnw mvnw
COPY .mvn/ .mvn/

RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 ./mvnw dependency:go-offline -DskipTests

################################################################################
# Stage 2: Build code Spring Boot ra file Jar
FROM deps as package

WORKDIR /build

COPY ./src src/
RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests && \
    mv target/$(./mvnw help:evaluate -Dexpression=project.artifactId -q -DforceStdout)-$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout).jar target/app.jar

################################################################################
# Stage 3: Giải nén các Layer của Spring Boot
FROM package as extract

WORKDIR /build

RUN java -Djarmode=layertools -jar target/app.jar extract --destination target/extracted

################################################################################
# Stage 4: Stage CHẠY CUỐI CÙNG - DÙNG TRÌNH DUYỆT CÓ SẴN
FROM mcr.microsoft.com/playwright/java:v1.44.0-jammy AS final

USER root

# BƯỚC 1: Chỉ định cho Playwright dùng trình duyệt đã tích hợp sẵn trong image Microsoft
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

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

# Copy từ stage 'extract' các layer của Spring Boot sang
COPY --from=extract /build/target/extracted/dependencies/ ./
COPY --from=extract /build/target/extracted/spring-boot-loader/ ./
COPY --from=extract /build/target/extracted/snapshot-dependencies/ ./
COPY --from=extract /build/target/extracted/application/ ./

# BƯỚC 2: ĐÃ XOÁ BỎ LỆNH "RUN java -cp ... com.microsoft.playwright.CLI install" GÂY LỖI VÀ CHẬM

# Cấp lại quyền chuẩn chỉnh cho appuser đọc thư mục chứa trình duyệt và thư mục /app
RUN chmod -R 755 /ms-playwright && chown -R appuser:appuser /app

USER appuser

# ĐÃ MỞ CẢ 2 CỔNG ĐỂ BẠN TIỆN CẤU HÌNH VÀO WEB HOẶC PORTAINER
EXPOSE 8080
EXPOSE 8081

ENTRYPOINT [ "java", "org.springframework.boot.loader.launch.JarLauncher" ]