package dev.roastapp.connector.api;

import dev.roastapp.common.dto.Provider;
import dev.roastapp.common.dto.UserStatsResponse;
import dev.roastapp.connector.service.ConnectorCoordinator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/internal/v1/stats")
public class StatsController {

    private final ConnectorCoordinator coordinator;

    public StatsController(ConnectorCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @GetMapping
    public ResponseEntity<UserStatsResponse> getStats(
            @RequestParam("userId") String userId,
            @RequestParam("providers") List<String> providerNames,
            @RequestHeader(value = "X-GitHub-Token", required = false) String githubToken,
            @RequestHeader(value = "X-Spotify-Token", required = false) String spotifyToken,
            @RequestHeader(value = "X-YouTube-Token", required = false) String youtubeToken) {

        List<Provider> providers = new ArrayList<>();
        for (String name : providerNames) {
            try {
                providers.add(Provider.valueOf(name.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        Map<Provider, String> headerTokens = new EnumMap<>(Provider.class);
        if (githubToken != null) headerTokens.put(Provider.GITHUB, githubToken);
        if (spotifyToken != null) headerTokens.put(Provider.SPOTIFY, spotifyToken);
        if (youtubeToken != null) headerTokens.put(Provider.YOUTUBE, youtubeToken);

        UserStatsResponse response = coordinator.fetchAggregatedStats(userId, providers, headerTokens);
        return ResponseEntity.ok(response);
    }
}
