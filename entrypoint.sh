#!/bin/bash
set -e

echo "Starting Redis..."
redis-server --daemonize yes --maxmemory 20mb --maxmemory-policy allkeys-lru

# Ultra-low-memory JVM settings for Render Free tier (512MB RAM / 0.1 CPU):
# 1. TieredStopAtLevel=1: Uses C1 compiler only (cuts JIT memory by 75% and boots 3x faster)
# 2. CICompilerCount=1: Prevents multi-threaded JIT thrashing on 0.1 CPU
# 3. UseSerialGC: Lowest memory overhead GC
# 4. Xss228k: Compact thread stacks
LOW_MEM_OPTS="-XX:TieredStopAtLevel=1 -XX:+UseSerialGC -XX:CICompilerCount=1 -Xss228k -XX:ReservedCodeCacheSize=14m"

echo "Starting Gateway immediately on port ${PORT:-10000} to satisfy Render port check..."
java $LOW_MEM_OPTS -Xmx70m -Xms32m -XX:MaxMetaspaceSize=45m -Dserver.port=${PORT:-10000} -jar gateway.jar &
GATEWAY_PID=$!

echo "Waiting for Gateway to bind port..."
sleep 6

echo "Starting Identity Service..."
java $LOW_MEM_OPTS -Xmx50m -Xms24m -XX:MaxMetaspaceSize=40m -jar identity-service.jar &
sleep 4

echo "Starting Connector Service..."
java $LOW_MEM_OPTS -Xmx40m -Xms20m -XX:MaxMetaspaceSize=35m -jar connector-service.jar &
sleep 4

echo "Starting Roast Service..."
java $LOW_MEM_OPTS -Xmx50m -Xms24m -XX:MaxMetaspaceSize=40m -jar roast-service.jar &
sleep 4

echo "Starting Share Service..."
java $LOW_MEM_OPTS -Xmx40m -Xms20m -XX:MaxMetaspaceSize=35m -jar share-service.jar &

echo "All services initiated. Keeping container alive on Gateway..."
wait $GATEWAY_PID
