# STAGE 1: Build source
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# STAGE 2: Giải nén layer (HÃY KIỂM TRA KỸ DÒNG NÀY XEM CÓ CHỮ "AS extract" CHƯA)
FROM eclipse-temurin:17-jre-alpine AS extract
WORKDIR /build
COPY --from=build /build/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

################################################################################
# STAGE 3: Khởi chạy môi trường Playwright & Spring Boot
FROM mcr.microsoft.com/playwright/java:v1.44.0-jammy AS final

USER root
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
WORKDIR /app
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

# ... (đoạn tạo user giữ nguyên) ...

# KIỂM TRA ĐOẠN NÀY: chữ "extract" phải trùng khớp với tên đặt ở Stage 2
COPY --from=extract /build/dependencies/ ./dependencies/
COPY --from=extract /build/spring-boot-loader/ ./spring-boot-loader/
COPY --from=extract /build/snapshot-dependencies/ ./snapshot-dependencies/
COPY --from=extract /build/application/ ./application/

RUN rm -rf /ms-playwright/*

RUN npx playwright install-deps chromium && \
    npx playwright install chromium

RUN chmod -R 755 /ms-playwright && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
EXPOSE 8081

ENTRYPOINT [ "java", "-cp", "application:dependencies:spring-boot-loader:snapshot-dependencies", "org.springframework.boot.loader.launch.JarLauncher" ]