package dev.roastapp.connector.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.roastapp.common.dto.Provider;
import dev.roastapp.connector.domain.ProviderStats;
import dev.roastapp.connector.service.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SpotifyConnector implements Connector {

    private static final Logger log = LoggerFactory.getLogger(SpotifyConnector.class);
    private final RestClient restClient;

    public SpotifyConnector() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.spotify.com/v1")
                .build();
    }

    public SpotifyConnector(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Provider provider() {
        return Provider.SPOTIFY;
    }

    @Override
    public ProviderStats fetch(String accessToken) {
        log.info("Fetching Spotify listening stats");

        // 1. Fetch Top Artists
        SpotifyArtistsResponse topArtistsResponse = restClient.get()
                .uri("/me/top/artists?limit=15&time_range=medium_term")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(SpotifyArtistsResponse.class);

        List<SpotifyArtist> artists = topArtistsResponse != null && topArtistsResponse.items() != null
                ? topArtistsResponse.items()
                : Collections.emptyList();

        List<String> topArtistNames = artists.stream()
                .map(SpotifyArtist::name)
                .limit(8)
                .toList();

        // Extract and aggregate top genres
        Map<String, Long> genreCounts = artists.stream()
                .flatMap(a -> a.genres() != null ? a.genres().stream() : java.util.stream.Stream.empty())
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()));

        List<String> topGenres = genreCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();

        // Average popularity (0-100, where 100 is mainstream pop, low is underground hipster)
        double avgPopularity = artists.stream()
                .mapToInt(SpotifyArtist::popularity)
                .average()
                .orElse(50.0);

        // 2. Fetch Top Tracks
        SpotifyTracksResponse topTracksResponse = restClient.get()
                .uri("/me/top/tracks?limit=10&time_range=medium_term")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(SpotifyTracksResponse.class);

        List<String> topTrackNames = topTracksResponse != null && topTracksResponse.items() != null
                ? topTracksResponse.items().stream().map(t -> t.name() + " - " + (t.artists().isEmpty() ? "" : t.artists().get(0).name())).toList()
                : Collections.emptyList();

        // 3. Recently played (for late night listening calculation)
        int lateNightPlays = 0;
        int totalPlaysObserved = 0;
        try {
            SpotifyRecentlyPlayedResponse recent = restClient.get()
                    .uri("/me/player/recently-played?limit=50")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(SpotifyRecentlyPlayedResponse.class);

            if (recent != null && recent.items() != null) {
                for (SpotifyPlayItem item : recent.items()) {
                    totalPlaysObserved++;
                    if (item.playedAt() != null) {
                        int hour = item.playedAt().atZone(ZoneOffset.UTC).getHour();
                        if (hour >= 23 || hour <= 4) {
                            lateNightPlays++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch recently played tracks: {}", e.getMessage());
        }

        double lateNightRatio = totalPlaysObserved > 0
                ? Math.round(((double) lateNightPlays / totalPlaysObserved) * 100.0) / 100.0
                : 0.0;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("topArtists", topArtistNames);
        data.put("topGenres", topGenres);
        data.put("topTracks", topTrackNames);
        data.put("averageArtistPopularity", Math.round(avgPopularity));
        data.put("lateNightListeningPercentage", Math.round(lateNightRatio * 100));

        return new ProviderStats(Provider.SPOTIFY, data);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SpotifyArtistsResponse(List<SpotifyArtist> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SpotifyArtist(String name, List<String> genres, int popularity) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SpotifyTracksResponse(List<SpotifyTrack> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SpotifyTrack(String name, List<SpotifyArtistRef> artists) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SpotifyArtistRef(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SpotifyRecentlyPlayedResponse(List<SpotifyPlayItem> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SpotifyPlayItem(@JsonProperty("played_at") Instant playedAt) {}
}
