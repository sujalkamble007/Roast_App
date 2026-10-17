package dev.roastapp.identity.api;

import java.time.Instant;
import java.util.List;

public final class IdentityDto {

    private IdentityDto() {}

    public record UserProfileResponse(
            String id,
            String email,
            String displayName,
            String avatarUrl,
            List<String> connectedProviders,
            Instant createdAt
    ) {}
}
