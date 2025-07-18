# API Gateway Service Dockerfile
FROM maven:3.9.4-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# 의존성 캐싱을 위해 pom.xml 먼저 복사
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x ./mvnw

# 의존성 다운로드 (캐시 레이어)
RUN ./mvnw dependency:go-offline -B

# 소스 코드 복사 및 빌드
COPY src src
RUN ./mvnw clean package -DskipTests -B

# 런타임 이미지
FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl tzdata && \
    rm -rf /var/cache/apk/* && \
    addgroup -g 1000 appuser && \
    adduser -u 1000 -G appuser -D appuser

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN chown appuser:appuser app.jar

USER appuser

# API Gateway용 메모리 설정 (라우팅 처리 - 중간 메모리)
ENV JAVA_OPTS="-Xmx384m -Xms192m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

HEALTHCHECK --interval=60s --timeout=10s --start-period=120s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]