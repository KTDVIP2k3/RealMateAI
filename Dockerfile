# STAGE 1: Build source bằng Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# STAGE 2: Giải nén layer bằng layertools
FROM eclipse-temurin:17-jre AS extract
WORKDIR /build
COPY --from=build /build/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

################################################################################
# STAGE 3: Môi trường chạy Spring Boot + Playwright (Bản Debian Slim ổn định)
FROM eclipse-temurin:17-jre-jammy AS final

# 1. Cài đặt các gói bổ trợ mạng và Chromium trên môi trường Ubuntu/Debian
RUN apt-get update && apt-get install -y --no-install-recommends \
    chromium \
    fonts-liberation \
    libgconf-2-4 \
    libnss3 \
    && rm -rf /var/lib/apt/lists/*

# 2. Thiết lập biến môi trường cho Playwright dùng chung Chromium hệ thống
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
ENV PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH=/usr/bin/chromium

# Tạo user an toàn
RUN useradd -ms /bin/sh appuser
WORKDIR /app

# 3. Copy các layer đã giải nén từ STAGE 2 sang đúng thư mục /app
COPY --from=extract /build/dependencies/ ./
COPY --from=extract /build/spring-boot-loader/ ./
COPY --from=extract /build/snapshot-dependencies/ ./
COPY --from=extract /build/application/ ./

# Phân quyền cho appuser
RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
EXPOSE 8081

# 4. Gọi Launcher chuẩn xác của Spring Boot 3.2+
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]