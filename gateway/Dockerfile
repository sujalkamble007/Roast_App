# Multi-stage Dockerfile for gateway
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

COPY pom.xml ./
COPY common ./common
COPY gateway ./gateway
COPY identity-service ./identity-service
COPY connector-service ./connector-service
COPY roast-service ./roast-service
COPY share-service ./share-service

RUN --mount=type=cache,target=/root/.m2 mvn clean package -pl gateway -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=builder /app/gateway/target/*.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx256m"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
