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

import java.util.*;

@Component
public class YouTubeConnector implements Connector {

    private static final Logger log = LoggerFactory.getLogger(YouTubeConnector.class);
    private final RestClient restClient;

    public YouTubeConnector() {
        this.restClient = RestClient.builder()
                .baseUrl("https://www.googleapis.com/youtube/v3")
                .build();
    }

    public YouTubeConnector(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Provider provider() {
        return Provider.YOUTUBE;
    }

    @Override
    public ProviderStats fetch(String accessToken) {
        log.info("Fetching YouTube activity stats");

        // 1. Fetch user subscriptions
        List<String> topChannels = new ArrayList<>();
        int totalSubscriptions = 0;
        try {
            YouTubeSubscriptionResponse subs = restClient.get()
                    .uri("/subscriptions?mine=true&part=snippet&maxResults=50")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(YouTubeSubscriptionResponse.class);

            if (subs != null && subs.items() != null) {
                totalSubscriptions = subs.pageInfo() != null ? subs.pageInfo().totalResults() : subs.items().size();
                topChannels = subs.items().stream()
                        .map(item -> item.snippet() != null ? item.snippet().title() : null)
                        .filter(Objects::nonNull)
                        .limit(8)
                        .toList();
            }
        } catch (Exception e) {
            log.warn("Could not fetch YouTube subscriptions: {}", e.getMessage());
        }

        // 2. Fetch liked videos
        List<String> likedVideoTitles = new ArrayList<>();
        int totalLikedVideos = 0;
        try {
            YouTubeVideoResponse liked = restClient.get()
                    .uri("/videos?myRating=like&part=snippet&maxResults=25")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(YouTubeVideoResponse.class);

            if (liked != null && liked.items() != null) {
                totalLikedVideos = liked.pageInfo() != null ? liked.pageInfo().totalResults() : liked.items().size();
                likedVideoTitles = liked.items().stream()
                        .map(item -> item.snippet() != null ? item.snippet().title() : null)
                        .filter(Objects::nonNull)
                        .limit(5)
                        .toList();
            }
        } catch (Exception e) {
            log.warn("Could not fetch YouTube liked videos: {}", e.getMessage());
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalSubscriptions", totalSubscriptions);
        data.put("subscribedChannels", topChannels);
        data.put("totalLikedVideos", totalLikedVideos);
        data.put("recentLikedVideos", likedVideoTitles);

        return new ProviderStats(Provider.YOUTUBE, data);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YouTubeSubscriptionResponse(
            List<SubscriptionItem> items,
            PageInfo pageInfo
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SubscriptionItem(Snippet snippet) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YouTubeVideoResponse(
            List<VideoItem> items,
            PageInfo pageInfo
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VideoItem(Snippet snippet) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Snippet(String title, @JsonProperty("channelTitle") String channelTitle) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PageInfo(int totalResults) {}
}
