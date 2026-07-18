# STAGE 1: Build source bằng Maven có sẵn cache
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
# Download trước các thư viện để tối ưu dung lượng và thời gian build lần sau
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# STAGE 2: Giải nén layer bằng layertools
FROM eclipse-temurin:17-jre-alpine AS extract
WORKDIR /build
COPY --from=build /build/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

################################################################################
# STAGE 3: Môi trường chạy Spring Boot + Playwright (Bản tối ưu dung lượng)
FROM eclipse-temurin:17-jre-alpine AS final

# 1. Cài đặt các thư viện C++ bắt buộc để chạy được Chromium của Playwright trên Alpine
RUN apk add --no-cache \
    chromium \
    nss \
    freetype \
    harfbuzz \
    ca-certificates \
    ttf-freefont \
    fontconfig

# 2. Thiết lập biến môi trường để Playwright DÙNG CHUNG Chromium của hệ thống (Không tự tải về bản nặng)
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
ENV PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH=/usr/bin/chromium-browser

# Tạo user để chạy ứng dụng an toàn
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app

# 3. Copy các layer đã giải nén từ STAGE 2 sang
COPY --from=extract /build/dependencies/ ./
COPY --from=extract /build/spring-boot-loader/ ./
COPY --from=extract /build/snapshot-dependencies/ ./
COPY --from=extract /build/application/ ./

# Phân quyền cho appuser
RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080
EXPOSE 8081

# 4. FIX lỗi Classpath: Chuyển dấu hai chấm sang đúng chuẩn của thư mục hiện tại (.) cho JarLauncher hoạt động
ENTRYPOINT [ "java", "-cp", ".:application:dependencies:spring-boot-loader:snapshot-dependencies", "org.springframework.boot.loader.launch.JarLauncher" ]