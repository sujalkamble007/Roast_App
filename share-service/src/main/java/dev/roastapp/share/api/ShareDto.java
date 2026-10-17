package dev.roastapp.share.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public class ShareDto {

    public record CreateShareRequest(
            @NotNull UUID roastId,
            @NotBlank String roastText,
            String tone,
            String sources
    ) {}

    public record ShareResponse(
            String id,
            String shareUrl,
            String imageUrl
    ) {}

    public record ShareDetails(
            String id,
            UUID roastId,
            String imageUrl,
            String roastText,
            String tone,
            String sources,
            Instant createdAt
    ) {}
}
