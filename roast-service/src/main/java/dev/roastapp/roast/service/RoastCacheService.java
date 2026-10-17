package dev.roastapp.roast.service;

import dev.roastapp.common.dto.Tone;
import dev.roastapp.roast.domain.RoastEntity;
import dev.roastapp.roast.repo.RoastRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class RoastCacheService {

    private static final Logger log = LoggerFactory.getLogger(RoastCacheService.class);
    private static final Duration CACHE_TTL = Duration.ofDays(7);

    private final RedisOperations<String, String> redisTemplate;
    private final RoastRepository roastRepository;

    public RoastCacheService(RedisOperations<String, String> redisTemplate, RoastRepository roastRepository) {
        this.redisTemplate = redisTemplate;
        this.roastRepository = roastRepository;
    }

    public Optional<String> getCachedRoast(String statsHash, Tone tone, String promptVersion) {
        String cacheKey = buildCacheKey(statsHash, tone, promptVersion);

        // 1. Try Redis cache
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isBlank()) {
                log.info("Roast cache HIT in Redis for key {}", cacheKey);
                return Optional.of(cached);
            }
        } catch (Exception e) {
            log.debug("Redis cache read failed: {}", e.getMessage());
        }

        // 2. Fallback to database lookup
        Optional<RoastEntity> dbMatch = roastRepository.findFirstByStatsHashAndToneAndPromptVersion(
                statsHash, tone, promptVersion);

        if (dbMatch.isPresent() && dbMatch.get().getRoastText() != null) {
            String roastText = dbMatch.get().getRoastText();
            log.info("Roast cache HIT in DB for statsHash {}", statsHash);
            // Repopulate Redis cache
            putCachedRoast(statsHash, tone, promptVersion, roastText);
            return Optional.of(roastText);
        }

        log.debug("Roast cache MISS for key {}", cacheKey);
        return Optional.empty();
    }

    public void putCachedRoast(String statsHash, Tone tone, String promptVersion, String roastText) {
        String cacheKey = buildCacheKey(statsHash, tone, promptVersion);
        try {
            redisTemplate.opsForValue().set(cacheKey, roastText, CACHE_TTL);
            log.debug("Saved roast to cache key {}", cacheKey);
        } catch (Exception e) {
            log.warn("Could not save roast to Redis cache: {}", e.getMessage());
        }
    }

    private String buildCacheKey(String statsHash, Tone tone, String promptVersion) {
        return "cache:roast:" + statsHash + ":" + tone.name().toLowerCase() + ":" + promptVersion;
    }
}
