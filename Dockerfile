# STAGE 1: Build source
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# STAGE 2: Giải nén layer
FROM eclipse-temurin:17-jre-alpine AS extract
WORKDIR /build
COPY --from=build /build/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

################################################################################
# STAGE 3: Khởi chạy môi trường Spring Boot THUẦN (Siêu nhẹ, tiết kiệm ổ đĩa)
FROM eclipse-temurin:17-jre-alpine AS final

# Tạo user để chạy ứng dụng an toàn (không dùng root)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app

# Copy các layer đã giải nén từ STAGE 2 sang
COPY --from=extract /build/dependencies/ ./dependencies/
COPY --from=extract /build/spring-boot-loader/ ./spring-boot-loader/
COPY --from=extract /build/snapshot-dependencies/ ./snapshot-dependencies/
COPY --from=extract /build/application/ ./application/

# Phân quyền cho appuser
RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080
EXPOSE 8081

ENTRYPOINT [ "java", "-cp", "application:dependencies:spring-boot-loader:snapshot-dependencies", "org.springframework.boot.loader.launch.JarLauncher" ]