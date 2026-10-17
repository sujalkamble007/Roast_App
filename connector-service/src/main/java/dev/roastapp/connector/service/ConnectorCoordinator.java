package dev.roastapp.connector.service;

import dev.roastapp.common.dto.Provider;
import dev.roastapp.common.dto.UserStatsResponse;
import dev.roastapp.connector.domain.ProviderStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class ConnectorCoordinator {

    private static final Logger log = LoggerFactory.getLogger(ConnectorCoordinator.class);

    private final Map<Provider, Connector> connectorMap = new EnumMap<>(Provider.class);
    private final StringRedisTemplate redisTemplate;

    public ConnectorCoordinator(List<Connector> connectors, StringRedisTemplate redisTemplate) {
        for (Connector connector : connectors) {
            this.connectorMap.put(connector.provider(), connector);
        }
        this.redisTemplate = redisTemplate;
        log.info("Registered connectors for providers: {}", connectorMap.keySet());
    }

    public UserStatsResponse fetchAggregatedStats(
            String userId,
            List<Provider> requestedProviders,
            Map<Provider, String> headerTokens) {

        Map<String, Object> aggregatedStats = new LinkedHashMap<>();
        List<String> failedProviders = new ArrayList<>();

        List<CompletableFuture<Void>> futures = requestedProviders.stream().map(provider ->
                CompletableFuture.runAsync(() -> {
                    Connector connector = connectorMap.get(provider);
                    if (connector == null) {
                        log.warn("No connector registered for provider: {}", provider);
                        synchronized (failedProviders) {
                            failedProviders.add(provider.name() + ": not implemented");
                        }
                        return;
                    }

                    // 1. Look for explicit token from header (used during Step 3 dev/testing)
                    String token = headerTokens.get(provider);

                    // 2. Fallback to Redis token stored by identity-service
                    if (token == null || token.isBlank()) {
                        try {
                            String redisKey = "tok:" + userId + ":" + provider.name().toLowerCase();
                            token = redisTemplate.opsForValue().get(redisKey);
                        } catch (Exception e) {
                            log.debug("Could not read token from Redis: {}", e.getMessage());
                        }
                    }

                    if (token == null || token.isBlank()) {
                        log.warn("No access token found for user {} and provider {}", userId, provider);
                        synchronized (failedProviders) {
                            failedProviders.add(provider.name() + ": missing access token");
                        }
                        return;
                    }

                    try {
                        ProviderStats stats = connector.fetch(token);
                        synchronized (aggregatedStats) {
                            aggregatedStats.put(provider.name().toLowerCase(), stats.data());
                        }
                    } catch (Exception e) {
                        log.error("Failed to fetch stats from provider {}: {}", provider, e.getMessage(), e);
                        synchronized (failedProviders) {
                            failedProviders.add(provider.name() + ": " + e.getMessage());
                        }
                    }
                })
        ).toList();

        // Wait for all connectors to finish in parallel
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return new UserStatsResponse(userId, aggregatedStats, failedProviders);
    }
}
