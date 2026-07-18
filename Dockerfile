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
# Stage 3: Giải nén các Layer của Spring Boot (Bắt buộc phải có để Stage 4 copy)
FROM package as extract

WORKDIR /build

RUN java -Djarmode=layertools -jar target/app.jar extract --destination target/extracted

################################################################################
# Stage 4: Stage CHẠY CUỐI CÙNG - ĐÃ THÊM LỆNH CÀI ĐẶT PLAYWRIGHT
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

# Copy từ stage 'extract' ở bước 3 sang trước để có file pom.xml và code chạy lệnh cài
COPY --from=extract build/target/extracted/dependencies/ ./
COPY --from=extract build/target/extracted/spring-boot-loader/ ./
COPY --from=extract build/target/extracted/snapshot-dependencies/ ./
COPY --from=extract build/target/extracted/application/ ./

# Copy thêm file mvnw và pom.xml tạm thời vào Stage 4 để thực thi lệnh cài đặt của Maven
COPY --from=extract build/mvnw ./mvnw
COPY --from=extract build/.mvn/ ./.mvn
COPY pom.xml ./pom.xml

> 💡 **Đoạn thêm mới**: Ép hệ thống kiểm tra và tải/cấu hình chính xác các trình duyệt Playwright vào thư mục hệ thống, sau đó dọn dẹp file thừa để tiết kiệm đĩa.
RUN ./mvnw playwright:install -DskipTests && \
    rm -rf ./mvnw ./.mvn ./pom.xml

# Cấp lại quyền chuẩn chỉnh cho appuser đọc thư mục chứa trình duyệt và thư mục /app
RUN chmod -R 755 /ms-playwright && chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

ENTRYPOINT [ "java", "org.springframework.boot.loader.launch.JarLauncher" ]