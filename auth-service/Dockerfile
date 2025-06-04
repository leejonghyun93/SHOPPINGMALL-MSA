FROM openjdk:17-ea-11-jdk-slim
VOLUME /tmp
COPY target/auth-service-1.0.jar AuthService.jar
ENTRYPOINT ["java","-jar","AuthService.jar"]