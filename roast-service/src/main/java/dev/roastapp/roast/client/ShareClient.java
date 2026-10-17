package dev.roastapp.roast.client;

import java.util.UUID;

public interface ShareClient {

    ShareResponse createShare(UUID roastId, String roastText, String tone, String sources);

    record CreateShareRequest(UUID roastId, String roastText, String tone, String sources) {}
    record ShareResponse(String id, String shareUrl, String imageUrl) {}
}
