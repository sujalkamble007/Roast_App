package dev.roastapp.roast.service;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private RedisOperations<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        idempotencyService = new IdempotencyService(redisTemplate);
    }

    @Test
    void getExistingRoastId_whenKeyBlankOrNull_returnsEmpty() {
        assertThat(idempotencyService.getExistingRoastId("user-1", null)).isEmpty();
        assertThat(idempotencyService.getExistingRoastId("user-1", "   ")).isEmpty();
    }

    @Test
    void getExistingRoastId_whenKeyExists_returnsUuid() {
        UUID expectedId = UUID.randomUUID();
        when(valueOperations.get("idemp:user-1:key-123")).thenReturn(expectedId.toString());

        Optional<UUID> actual = idempotencyService.getExistingRoastId("user-1", "key-123");

        assertThat(actual).contains(expectedId);
    }

    @Test
    void recordKey_storesInRedisWithTtl() {
        UUID roastId = UUID.randomUUID();
        idempotencyService.recordKey("user-1", "key-123", roastId);

        verify(valueOperations).set(eq("idemp:user-1:key-123"), eq(roastId.toString()), eq(Duration.ofMinutes(15)));
    }
}
