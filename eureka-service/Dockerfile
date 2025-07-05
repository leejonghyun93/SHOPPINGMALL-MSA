FROM openjdk:17-jdk-slim AS builder
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests -Dmaven.javadoc.skip=true

FROM amazoncorretto:17-alpine-jdk
RUN apk add --no-cache curl && \
    rm -rf /var/cache/apk/*

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# 안전하고 검증된 JVM 옵션만 사용
ENV JAVA_OPTS="-Xmx128m -Xms64m"

EXPOSE 8761

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8761/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]