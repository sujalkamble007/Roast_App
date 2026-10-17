# Multi-stage Dockerfile for roast-service
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

COPY pom.xml ./
COPY common ./common
RUN mvn install -pl common -DskipTests

COPY roast-service ./roast-service
RUN mvn package -pl roast-service -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=builder /app/roast-service/target/*.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx256m"
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
