# syntax=docker/dockerfile:1

################################################################################
# Stage 1: Tải các dependencies về trước sử dụng image Playwright Java
FROM mcr.microsoft.com/playwright/java:v1.44.0-jammy AS DEPS

WORKDIR /build

COPY --chmod=0755 mvnw mvnw
COPY .mvn/ .mvn/

RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 ./mvnw dependency:go-offline -DskipTests

################################################################################
# Stage 2: Build code Spring Boot ra file Jar
FROM DEPS AS PACKAGE

WORKDIR /build

COPY ./src src/
RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests && \
    mv target/$(./mvnw help:evaluate -Dexpression=project.artifactId -q -DforceStdout)-$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout).jar target/app.jar

################################################################################
# Stage 3: Giải nén các Layer của Spring Boot
FROM PACKAGE AS EXTRACT

WORKDIR /build

RUN java -Djarmode=layertools -jar target/app.jar extract --destination target/extracted

################################################################################
# Stage 4: Stage CHẠY CUỐI CÙNG
FROM mcr.microsoft.com/playwright/java:v1.44.0-jammy AS FINAL

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

# Copy các layer Spring Boot từ stage 'EXTRACT' sang (Đã đổi thành viết hoa)
COPY --from=EXTRACT build/target/extracted/dependencies/ ./
COPY --from=EXTRACT build/target/extracted/spring-boot-loader/ ./
COPY --from=EXTRACT build/target/extracted/snapshot-dependencies/ ./
COPY --from=EXTRACT build/target/extracted/application/ ./

# Gọi Playwright CLI thông qua JarLauncher / Classpath tổng hợp để tự động cài đặt
RUN java -cp "dependencies/*:spring-boot-loader/*:snapshot-dependencies/*:application/*" com.microsoft.playwright.CLI install

# Cấp lại quyền chuẩn chỉnh cho appuser đọc thư mục chứa trình duyệt và thư mục /app
RUN chmod -R 755 /ms-playwright && chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

ENTRYPOINT [ "java", "org.springframework.boot.loader.launch.JarLauncher" ]