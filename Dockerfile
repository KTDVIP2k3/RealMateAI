FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build



COPY pom.xml .

COPY src ./src



RUN mvn clean package -DskipTests && \

java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted && \

rm -rf ~/.m2/repository target/*.jar



FROM mcr.microsoft.com/playwright/java:v1.44.0-jammy AS final

WORKDIR /app



ENV TZ=Asia/Ho_Chi_Minh

ENV DOCKER=true

ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

ENV LANG=C.UTF-8

ENV LC_ALL=C.UTF-8



USER root



RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone && \

apt-get update && apt-get install -y --no-install-recommends \

fonts-liberation \

fonts-noto-cjk \

fonts-wqy-zenhei \

libnss3 \

libatk-bridge2.0-0 \

libcups2 \

libdrm2 \

libxcomposite1 \

libxdamage1 \

libxrandr2 \

libgbm1 \

libasound2 \

&& rm -rf /var/lib/apt/lists/* && \

mkdir -p /tmp/.org.chromium.Chromium /tmp/chrome-profile-bot /tmp/chrome-crashes && \

chmod -R 777 /tmp /ms-playwright



COPY --from=builder /build/target/extracted/dependencies/ ./

COPY --from=builder /build/target/extracted/spring-boot-loader/ ./

COPY --from=builder /build/target/extracted/snapshot-dependencies/ ./

COPY --from=builder /build/target/extracted/application/ ./



EXPOSE 8080



ENTRYPOINT [ "java", "-Duser.timezone=Asia/Ho_Chi_Minh", "org.springframework.boot.loader.launch.JarLauncher" ]