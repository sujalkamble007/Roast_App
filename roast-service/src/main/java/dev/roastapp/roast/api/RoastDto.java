package dev.roastapp.roast.api;

import dev.roastapp.common.dto.RoastStatus;
import dev.roastapp.common.dto.Tone;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class RoastDto {

    private RoastDto() {}

    public record CreateRoastRequest(
            @NotNull(message = "Tone is required")
            Tone tone,

            @NotEmpty(message = "At least one provider source is required")
            List<String> sources
    ) {}

    public record RoastResponse(
            UUID id,
            String userId,
            RoastStatus status,
            Tone tone,
            String sources,
            String statsJson,
            String roastText,
            String errorMessage,
            String promptVersion,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record QuotaResponse(
            int limit,
            int used,
            int remaining,
            Instant resetsAt
    ) {}
}
