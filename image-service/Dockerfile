FROM openjdk:17-jdk-slim

WORKDIR /app

# 이미지 처리로 인해 메모리를 좀 더 할당
ENV JAVA_OPTS="-Xmx200m -Xms100m -XX:MaxMetaspaceSize=100m -XX:+UseG1GC"

COPY target/*.jar image-service.jar

EXPOSE 8088

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8088/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar image-service.jar"]