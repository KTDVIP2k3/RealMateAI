################################################################################
# Stage 1: Tải dependencies của Java
FROM eclipse-temurin:17-jdk-jammy as deps
WORKDIR /build

COPY --chmod=0755 mvnw mvnw
COPY .mvn/ .mvn/

RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 ./mvnw dependency:go-offline -DskipTests

################################################################################
# Stage 2: Đóng gói ứng dụng thành file JAR
FROM deps as package
WORKDIR /build
COPY ./src src/
RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests && \
    mv target/$(./mvnw help:evaluate -Dexpression=project.artifactId -q -DforceStdout)-$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout).jar target/app.jar

################################################################################
# Stage 3: Giải nén Spring Boot Layers để tối ưu cache
FROM package as extract
WORKDIR /build
RUN java -Djarmode=layertools -jar target/app.jar extract --destination target/extracted

################################################################################
# Stage 4: Stage chạy ứng dụng (Đã tích hợp Playwright + Java)
# Sử dụng image Ubuntu của Playwright chứa sẵn mọi trình duyệt và thư viện hệ thống cần thiết
FROM mcr.microsoft.com/playwright:v1.49.0-noble AS final

# Cài đặt OpenJDK 17 JRE trên nền Ubuntu
RUN apt-get update && apt-get install -y \
    openjdk-17-jre-headless \
    && rm -rf /var/list/apt/lists/*

WORKDIR /app

# Khởi tạo user bảo mật (Non-root)
ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser

# Đổi quyền sở hữu thư mục làm việc cho appuser (Playwright cần quyền ghi một số file tạm nếu có)
RUN chown appuser:appuser /app
USER appuser

# Copy các lớp ứng dụng Spring Boot từ stage extract
COPY --from=extract build/target/extracted/dependencies/ ./
COPY --from=extract build/target/extracted/spring-boot-loader/ ./
COPY --from=extract build/target/extracted/snapshot-dependencies/ ./
COPY --from=extract build/target/extracted/application/ ./

# Biến môi trường báo cho Playwright biết các trình duyệt đã có sẵn ở đâu trong hệ thống
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

EXPOSE 8080

ENTRYPOINT [ "java", "org.springframework.boot.loader.launch.JarLauncher" ]