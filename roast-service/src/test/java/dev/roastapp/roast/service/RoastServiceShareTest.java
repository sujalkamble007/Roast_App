package dev.roastapp.roast.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.roastapp.common.dto.RoastStatus;
import dev.roastapp.common.dto.Tone;
import dev.roastapp.common.error.ResourceNotFoundException;
import dev.roastapp.roast.client.ConnectorClient;
import dev.roastapp.roast.client.LlmClient;
import dev.roastapp.roast.client.ShareClient;
import dev.roastapp.roast.domain.RoastEntity;
import dev.roastapp.roast.repo.RoastRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoastServiceShareTest {

    @Mock
    private RoastRepository roastRepository;
    @Mock
    private LlmClient llmClient;
    @Mock
    private ConnectorClient connectorClient;
    @Mock
    private ShareClient shareClient;
    @Mock
    private QuotaService quotaService;
    @Mock
    private RoastCacheService roastCacheService;
    @Mock
    private IdempotencyService idempotencyService;

    private RoastService roastService;

    @BeforeEach
    void setUp() {
        roastService = new RoastService(
                roastRepository,
                llmClient,
                new PromptBuilder(),
                new ObjectMapper(),
                connectorClient,
                shareClient,
                quotaService,
                roastCacheService,
                idempotencyService
        );
    }

    @Test
    void shareRoast_whenRoastNotFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(roastRepository.findByIdAndUserId(id, "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roastService.shareRoast(id, "user-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shareRoast_whenRoastNotReady_throwsIllegalStateException() {
        UUID id = UUID.randomUUID();
        RoastEntity entity = new RoastEntity(id, "user-1", RoastStatus.PENDING, Tone.BRUTAL, "github", "{}", "hash", "v1.0.0");
        when(roastRepository.findByIdAndUserId(id, "user-1")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> roastService.shareRoast(id, "user-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot share a roast that is not READY");
    }

    @Test
    void shareRoast_whenRoastReady_callsShareClientAndReturnsResult() {
        UUID id = UUID.randomUUID();
        RoastEntity entity = new RoastEntity(id, "user-1", RoastStatus.READY, Tone.BRUTAL, "github", "{}", "hash", "v1.0.0");
        entity.setRoastText("Certified brutal roast text");
        when(roastRepository.findByIdAndUserId(id, "user-1")).thenReturn(Optional.of(entity));

        ShareClient.ShareResponse mockResponse = new ShareClient.ShareResponse(
                "nanoid123456",
                "/s/nanoid123456",
                "http://localhost:8080/card.png"
        );
        when(shareClient.createShare(id, "Certified brutal roast text", "BRUTAL", "github"))
                .thenReturn(mockResponse);

        ShareClient.ShareResponse result = roastService.shareRoast(id, "user-1");

        assertThat(result.id()).isEqualTo("nanoid123456");
        assertThat(result.shareUrl()).isEqualTo("/s/nanoid123456");
        verify(shareClient).createShare(id, "Certified brutal roast text", "BRUTAL", "github");
    }
}
