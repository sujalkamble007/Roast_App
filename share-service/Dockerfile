# Multi-stage Dockerfile for share-service
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

COPY pom.xml ./
COPY common ./common
RUN mvn install -pl common -DskipTests

COPY share-service ./share-service
RUN mvn package -pl share-service -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=builder /app/share-service/target/*.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx256m"
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
