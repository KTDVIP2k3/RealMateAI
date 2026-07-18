# syntax=docker/dockerfile:1

################################################################################
# Stage 1: Tải các dependencies (Dùng image Maven nhẹ để cache thư viện tốt hơn)
FROM maven:3.9.6-eclipse-temurin-17 AS deps
WORKDIR /build

COPY --chmod=0755 mvnw mvnw
COPY .mvn/ .mvn/

RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 ./mvnw dependency:go-offline -DskipTests

################################################################################
# Stage 2: Build code Spring Boot ra file Jar (Kế thừa từ image Maven nhẹ)
FROM maven:3.9.6-eclipse-temurin-17 AS package
WORKDIR /build

# Đưa mvnw và cấu hình .mvn vào để chạy được lệnh build bên dưới
COPY --chmod=0755 mvnw mvnw
COPY .mvn/ .mvn/
COPY ./src src/

RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests && \
    mv target/$(./mvnw help:evaluate -Dexpression=project.artifactId -q -DforceStdout)-$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout).jar target/app.jar

################################################################################
# Stage 3: Giải nén các Layer của Spring Boot (Dùng JRE nhẹ để xử lý layertools)
FROM eclipse-temurin:17-jre AS extract
WORKDIR /build

COPY --from=package /build/target/app.jar target/app.jar
RUN java -Djarmode=layertools -jar target/app.jar extract --destination target/extracted

################################################################################
# Stage 4: Stage CHẠY CUỐI CÙNG - Dùng trình duyệt tích hợp sẵn trong Microsoft Playwright
FROM mcr.microsoft.com/playwright/java:v1.44.0-jammy AS final

USER root

# Chỉ định Playwright sử dụng thẳng trình duyệt có sẵn trong image gốc
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

# Copy các layer Spring Boot đã giải nén siêu nhẹ từ stage 'extract' sang
COPY --from=extract /build/target/extracted/dependencies/ ./
COPY --from=extract /build/target/extracted/spring-boot-loader/ ./
COPY --from=extract /build/target/extracted/snapshot-dependencies/ ./
COPY --from=extract /build/target/extracted/application/ ./

# Cấp quyền chuẩn chỉnh cho appuser truy cập trình duyệt và app
RUN chmod -R 755 /ms-playwright && chown -R appuser:appuser /app

USER appuser

# Mở cổng ứng dụng
EXPOSE 8080
EXPOSE 8081

ENTRYPOINT [ "java", "org.springframework.boot.loader.launch.JarLauncher" ]