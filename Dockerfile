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
# STAGE 3: Môi trường chạy Spring Boot THUẦN (Siêu nhẹ, siêu tiết kiệm ổ đĩa)
FROM eclipse-temurin:17-jre-alpine AS final

# Tạo user để chạy ứng dụng an toàn (không dùng root)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app

# Copy các layer đã giải nén từ STAGE 2 sang đúng thư mục /app
COPY --from=extract /build/dependencies/ ./
COPY --from=extract /build/spring-boot-loader/ ./
COPY --from=extract /build/snapshot-dependencies/ ./
COPY --from=extract /build/application/ ./

# Phân quyền cho appuser
RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080
EXPOSE 8081

# FIX lỗi Classpath: Sử dụng đường dẫn tuyệt đối để JarLauncher nhận đúng file cấu hình port
ENTRYPOINT [ "java", "-cp", "/app/application:/app/dependencies:/app/spring-boot-loader:/app/snapshot-dependencies", "org.springframework.boot.loader.launch.JarLauncher" ]