#!/bin/bash
set -e

echo "Starting Redis server..."
redis-server --daemonize yes

echo "Starting backend microservices with optimized memory..."
JVM_OPTS="-Xmx80m -Xms32m -XX:+UseSerialGC -XX:MaxMetaspaceSize=64m -XX:ReservedCodeCacheSize=24m -Xss256k"

java $JVM_OPTS -jar identity-service.jar > /dev/null 2>&1 &
java $JVM_OPTS -jar connector-service.jar > /dev/null 2>&1 &
java $JVM_OPTS -jar roast-service.jar > /dev/null 2>&1 &
java $JVM_OPTS -jar share-service.jar > /dev/null 2>&1 &

echo "Waiting for microservices to bind..."
sleep 6

echo "Starting Gateway on port ${PORT:-10000}..."
exec java -Xmx120m -Xms48m -XX:+UseSerialGC -Dserver.port=${PORT:-10000} -jar gateway.jar
