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
    rm -rf /var/cache/apk/* && \
    addgroup -g 1000 appuser && \
    adduser -u 1000 -G appuser -D appuser

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN chown appuser:appuser app.jar

USER appuser

# 사용자 서비스용 메모리 설정 (CRUD 작업)
ENV JAVA_OPTS="-Xmx80m -Xms40m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8103

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8103/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]