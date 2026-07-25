FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests && \
java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted && \
rm -rf ~/.m2/repository target/*.jar

FROM eclipse-temurin:17-jre-jammy AS final
WORKDIR /app

ENV TZ=Asia/Ho_Chi_Minh
ENV DOCKER=true
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

USER root

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone && \
mkdir -p /tmp/chrome-crashes && \
chmod -R 777 /tmp

COPY --from=builder /build/target/extracted/dependencies/ ./
COPY --from=builder /build/target/extracted/spring-boot-loader/ ./
COPY --from=builder /build/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/target/extracted/application/ ./

EXPOSE 8080

ENTRYPOINT [ "java", "-Duser.timezone=Asia/Ho_Chi_Minh", "org.springframework.boot.loader.launch.JarLauncher" ]