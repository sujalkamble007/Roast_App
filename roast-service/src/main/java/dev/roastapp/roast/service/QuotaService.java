package dev.roastapp.roast.service;

import dev.roastapp.common.error.QuotaExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class QuotaService {

    private static final Logger log = LoggerFactory.getLogger(QuotaService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RedisOperations<String, String> redisTemplate;
    private final int dailyLimit;

    public QuotaService(
            RedisOperations<String, String> redisTemplate,
            @Value("${roast.daily-limit:3}") int dailyLimit) {
        this.redisTemplate = redisTemplate;
        this.dailyLimit = dailyLimit;
    }

    public void checkAndConsumeQuota(String userId) {
        String key = buildQuotaKey(userId);
        Long currentCount = 0L;

        try {
            currentCount = redisTemplate.opsForValue().increment(key);
            if (currentCount != null && currentCount == 1) {
                // First roast of the day: set TTL to midnight UTC
                Duration ttl = calculateTtlUntilMidnightUtc();
                redisTemplate.expire(key, ttl);
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for quota check, allowing request: {}", e.getMessage());
            return;
        }

        if (currentCount != null && currentCount > dailyLimit) {
            long retryAfterSeconds = calculateTtlUntilMidnightUtc().getSeconds();
            log.warn("Daily quota exceeded for user {}. Used: {}, Limit: {}", userId, currentCount, dailyLimit);
            throw new QuotaExceededException(
                    "You have reached your daily limit of " + dailyLimit + " roasts. Please try again tomorrow.",
                    retryAfterSeconds
            );
        }

        log.info("Quota consumed for user {}: {}/{} used today", userId, currentCount, dailyLimit);
    }

    public QuotaInfo getQuotaInfo(String userId) {
        String key = buildQuotaKey(userId);
        int used = 0;

        try {
            String val = redisTemplate.opsForValue().get(key);
            if (val != null) {
                used = Integer.parseInt(val);
            }
        } catch (Exception e) {
            log.warn("Could not query quota from Redis: {}", e.getMessage());
        }

        int remaining = Math.max(0, dailyLimit - used);
        Instant resetsAt = LocalDate.now(ZoneOffset.UTC)
                .plusDays(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);

        return new QuotaInfo(dailyLimit, used, remaining, resetsAt);
    }

    private String buildQuotaKey(String userId) {
        String today = LocalDate.now(ZoneOffset.UTC).format(DATE_FORMATTER);
        return "quota:" + userId + ":" + today;
    }

    private Duration calculateTtlUntilMidnightUtc() {
        Instant now = Instant.now();
        Instant midnightUtc = LocalDate.now(ZoneOffset.UTC)
                .plusDays(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
        return Duration.between(now, midnightUtc);
    }

    public record QuotaInfo(int limit, int used, int remaining, Instant resetsAt) {}
}
