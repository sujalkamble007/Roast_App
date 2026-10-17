package dev.roastapp.roast.service;

import dev.roastapp.common.dto.RoastStatus;
import dev.roastapp.common.dto.Tone;
import dev.roastapp.roast.domain.RoastEntity;
import dev.roastapp.roast.repo.RoastRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoastCacheServiceTest {

    @Mock
    private RedisOperations<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RoastRepository roastRepository;

    private RoastCacheService roastCacheService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        roastCacheService = new RoastCacheService(redisTemplate, roastRepository);
    }

    @Test
    void getCachedRoast_whenRedisHasKey_returnsRedisValue() {
        when(valueOperations.get("cache:roast:hash123:sarcastic:v1.0.0")).thenReturn("Brutal roast here");

        Optional<String> result = roastCacheService.getCachedRoast("hash123", Tone.SARCASTIC, "v1.0.0");

        assertThat(result).contains("Brutal roast here");
        verifyNoInteractions(roastRepository);
    }

    @Test
    void getCachedRoast_whenRedisMisses_checksDbAndRepopulatesRedis() {
        when(valueOperations.get(anyString())).thenReturn(null);

        RoastEntity entity = new RoastEntity(
                UUID.randomUUID(), "user-1", RoastStatus.READY, Tone.SARCASTIC, "github", "{}", "hash123", "v1.0.0"
        );
        entity.setRoastText("Found from database!");
        when(roastRepository.findFirstByStatsHashAndToneAndPromptVersion("hash123", Tone.SARCASTIC, "v1.0.0"))
                .thenReturn(Optional.of(entity));

        Optional<String> result = roastCacheService.getCachedRoast("hash123", Tone.SARCASTIC, "v1.0.0");

        assertThat(result).contains("Found from database!");
        verify(valueOperations).set(eq("cache:roast:hash123:sarcastic:v1.0.0"), eq("Found from database!"), any(Duration.class));
    }

    @Test
    void getCachedRoast_whenBothMiss_returnsEmpty() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(roastRepository.findFirstByStatsHashAndToneAndPromptVersion(anyString(), any(), anyString()))
                .thenReturn(Optional.empty());

        Optional<String> result = roastCacheService.getCachedRoast("missing", Tone.BRUTAL, "v1.0.0");

        assertThat(result).isEmpty();
    }

    @Test
    void putCachedRoast_setsRedisWithTtl() {
        roastCacheService.putCachedRoast("hash123", Tone.BRUTAL, "v1.0.0", "Some roast");

        verify(valueOperations).set(eq("cache:roast:hash123:brutal:v1.0.0"), eq("Some roast"), eq(Duration.ofDays(7)));
    }
}
