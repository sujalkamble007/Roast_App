package dev.roastapp.share.service;

import dev.roastapp.common.error.ResourceNotFoundException;
import dev.roastapp.share.domain.ShareEntity;
import dev.roastapp.share.repo.ShareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShareServiceTest {

    @Mock
    private ShareRepository shareRepository;

    @Mock
    private CardRenderer cardRenderer;

    @Mock
    private R2StorageService r2StorageService;

    private ShareService shareService;

    @BeforeEach
    void setUp() {
        shareService = new DefaultShareService(shareRepository, cardRenderer, r2StorageService, "http://localhost:8080");
    }

    @Test
    void createShare_whenExistingActiveShare_returnsExisting() {
        UUID roastId = UUID.randomUUID();
        ShareEntity existing = new ShareEntity("existing1234", roastId, "http://localhost:8080/card.png", "text", "BRUTAL", "github");
        when(shareRepository.findFirstByRoastIdAndRevokedAtIsNull(roastId)).thenReturn(Optional.of(existing));

        ShareService.ShareResult result = shareService.createShare(roastId, "text", "BRUTAL", "github");

        assertThat(result.id()).isEqualTo("existing1234");
        assertThat(result.shareUrl()).isEqualTo("/s/existing1234");
        verifyNoInteractions(cardRenderer);
        verifyNoInteractions(r2StorageService);
    }

    @Test
    void createShare_whenNew_rendersCardUploadsAndSaves() {
        UUID roastId = UUID.randomUUID();
        when(shareRepository.findFirstByRoastIdAndRevokedAtIsNull(roastId)).thenReturn(Optional.empty());
        when(cardRenderer.renderCard(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});
        when(shareRepository.save(any(ShareEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShareService.ShareResult result = shareService.createShare(roastId, "brutal text", "BRUTAL", "spotify");

        assertThat(result.id()).hasSize(12);
        assertThat(result.shareUrl()).isEqualTo("/s/" + result.id());
        assertThat(result.imageUrl()).contains("/api/v1/public/shares/" + result.id() + "/card.png");
        verify(r2StorageService).uploadCard(eq(result.id() + ".png"), any(byte[].class));
        verify(shareRepository).save(any(ShareEntity.class));
    }

    @Test
    void getShare_whenFound_returnsEntity() {
        ShareEntity entity = new ShareEntity("share123", UUID.randomUUID(), "http://img", "text", "SARCASTIC", "github");
        when(shareRepository.findByIdAndRevokedAtIsNull("share123")).thenReturn(Optional.of(entity));

        ShareEntity result = shareService.getShare("share123");

        assertThat(result.getId()).isEqualTo("share123");
    }

    @Test
    void getShare_whenNotFoundOrRevoked_throwsNotFound() {
        when(shareRepository.findByIdAndRevokedAtIsNull("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shareService.getShare("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void revokeShare_marksRevokedAt() {
        ShareEntity entity = new ShareEntity("share123", UUID.randomUUID(), "http://img", "text", "SARCASTIC", "github");
        when(shareRepository.findByIdAndRevokedAtIsNull("share123")).thenReturn(Optional.of(entity));

        shareService.revokeShare("share123");

        assertThat(entity.isRevoked()).isTrue();
        verify(shareRepository).save(entity);
    }
}
