# Multi-stage Dockerfile for Render backend deployment
FROM maven:3.9-eclipse-temurin-26-alpine AS builder
WORKDIR /app

COPY pom.xml ./
COPY common ./common
COPY gateway ./gateway
COPY identity-service ./identity-service
COPY connector-service ./connector-service
COPY roast-service ./roast-service
COPY share-service ./share-service

RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl bash redis

WORKDIR /app

COPY --from=builder /app/gateway/target/*.jar ./gateway.jar
COPY --from=builder /app/identity-service/target/*.jar ./identity-service.jar
COPY --from=builder /app/connector-service/target/*.jar ./connector-service.jar
COPY --from=builder /app/roast-service/target/*.jar ./roast-service.jar
COPY --from=builder /app/share-service/target/*.jar ./share-service.jar

COPY entrypoint.sh ./entrypoint.sh
RUN chmod +x ./entrypoint.sh

ENV PORT=10000
EXPOSE 10000

ENTRYPOINT ["./entrypoint.sh"]
