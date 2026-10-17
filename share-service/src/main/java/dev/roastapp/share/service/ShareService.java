package dev.roastapp.share.service;

import dev.roastapp.share.domain.ShareEntity;
import java.util.UUID;

public interface ShareService {

    ShareResult createShare(UUID roastId, String roastText, String tone, String sources);

    ShareEntity getShare(String id);

    byte[] getCardImage(String id);

    void revokeShare(String id);

    record ShareResult(String id, String shareUrl, String imageUrl) {}
}
