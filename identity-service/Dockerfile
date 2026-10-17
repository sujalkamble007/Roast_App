# Multi-stage Dockerfile for identity-service
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

COPY pom.xml ./
COPY common ./common
RUN mvn install -pl common -DskipTests

COPY identity-service ./identity-service
RUN mvn package -pl identity-service -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=builder /app/identity-service/target/*.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx256m"
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
