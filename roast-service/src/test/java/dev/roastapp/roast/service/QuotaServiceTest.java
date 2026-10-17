package dev.roastapp.roast.service;

import dev.roastapp.common.error.QuotaExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuotaServiceTest {

    @Mock
    private RedisOperations<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private QuotaService quotaService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        quotaService = new QuotaService(redisTemplate, 3);
    }

    @Test
    void checkAndConsumeQuota_whenFirstRoastOfDay_setsTtlAndPasses() {
        when(valueOperations.increment(any(String.class))).thenReturn(1L);

        quotaService.checkAndConsumeQuota("user-1");

        verify(redisTemplate).expire(any(String.class), any(Duration.class));
    }

    @Test
    void checkAndConsumeQuota_whenWithinLimit_passesWithoutExpiringAgain() {
        when(valueOperations.increment(any(String.class))).thenReturn(2L);

        quotaService.checkAndConsumeQuota("user-1");

        verify(redisTemplate, never()).expire(any(String.class), any(Duration.class));
    }

    @Test
    void checkAndConsumeQuota_whenExceedsLimit_throwsQuotaExceededException() {
        when(valueOperations.increment(any(String.class))).thenReturn(4L);

        assertThatThrownBy(() -> quotaService.checkAndConsumeQuota("user-1"))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("daily limit of 3 roasts");
    }

    @Test
    void getQuotaInfo_returnsAccurateUsedAndRemaining() {
        when(valueOperations.get(any(String.class))).thenReturn("2");

        QuotaService.QuotaInfo info = quotaService.getQuotaInfo("user-1");

        assertThat(info.limit()).isEqualTo(3);
        assertThat(info.used()).isEqualTo(2);
        assertThat(info.remaining()).isEqualTo(1);
        assertThat(info.resetsAt()).isNotNull();
    }
}
