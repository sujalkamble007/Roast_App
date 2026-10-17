package dev.roastapp.roast.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(15);

    private final RedisOperations<String, String> redisTemplate;

    public IdempotencyService(RedisOperations<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<UUID> getExistingRoastId(String userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        String key = "idemp:" + userId + ":" + idempotencyKey.trim();
        try {
            String existingId = redisTemplate.opsForValue().get(key);
            if (existingId != null) {
                log.info("Idempotent hit for user {} with key {}", userId, idempotencyKey);
                return Optional.of(UUID.fromString(existingId));
            }
        } catch (Exception e) {
            log.debug("Redis lookup for idempotency key failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public void recordKey(String userId, String idempotencyKey, UUID roastId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        String key = "idemp:" + userId + ":" + idempotencyKey.trim();
        try {
            redisTemplate.opsForValue().set(key, roastId.toString(), IDEMPOTENCY_TTL);
            log.debug("Stored idempotency key {} -> {}", key, roastId);
        } catch (Exception e) {
            log.warn("Could not save idempotency key to Redis: {}", e.getMessage());
        }
    }
}
