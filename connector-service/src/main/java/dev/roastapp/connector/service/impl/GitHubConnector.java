package dev.roastapp.connector.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.roastapp.common.dto.Provider;
import dev.roastapp.connector.domain.ProviderStats;
import dev.roastapp.connector.service.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class GitHubConnector implements Connector {

    private static final Logger log = LoggerFactory.getLogger(GitHubConnector.class);
    private final RestClient restClient;

    public GitHubConnector() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .build();
    }

    // Constructor for testing / WireMock injection
    public GitHubConnector(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Provider provider() {
        return Provider.GITHUB;
    }

    @Override
    public ProviderStats fetch(String accessToken) {
        log.info("Fetching GitHub stats using provided token");

        // 1. Fetch user profile
        GitHubUser user = restClient.get()
                .uri("/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(GitHubUser.class);

        if (user == null) {
            throw new RuntimeException("Failed to fetch GitHub user profile");
        }

        // 2. Fetch repos (up to 100)
        List<GitHubRepo> repos = restClient.get()
                .uri("/user/repos?per_page=100&sort=updated")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<List<GitHubRepo>>() {});

        if (repos == null) {
            repos = Collections.emptyList();
        }

        // 3. Compute Repo Stats
        int totalRepos = repos.size();
        int forksCount = (int) repos.stream().filter(GitHubRepo::fork).count();
        int starsCount = repos.stream().mapToInt(GitHubRepo::stargazersCount).sum();

        // Abandoned repos: not pushed/updated in > 180 days
        Instant sixMonthsAgo = Instant.now().minusSeconds(180L * 24 * 3600);
        int abandonedRepos = (int) repos.stream()
                .filter(r -> r.pushedAt() != null && r.pushedAt().toInstant().isBefore(sixMonthsAgo))
                .count();

        // Top languages by frequency
        Map<String, Long> languageCounts = repos.stream()
                .map(GitHubRepo::language)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(l -> l, Collectors.counting()));

        List<String> topLanguages = languageCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();

        // 4. Fetch recent events for commit hours
        int lateNightCommits = 0;
        int totalCommitsObserved = 0;
        try {
            List<GitHubEvent> events = restClient.get()
                    .uri("/users/{username}/events?per_page=50", user.login())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<GitHubEvent>>() {});

            if (events != null) {
                for (GitHubEvent event : events) {
                    if ("PushEvent".equals(event.type()) && event.createdAt() != null) {
                        int hour = event.createdAt().atZoneSameInstant(ZoneOffset.UTC).getHour();
                        totalCommitsObserved++;
                        if (hour >= 0 && hour < 5) {
                            lateNightCommits++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch user events for {}: {}", user.login(), e.getMessage());
        }

        double lateNightRatio = totalCommitsObserved > 0
                ? Math.round(((double) lateNightCommits / totalCommitsObserved) * 100.0) / 100.0
                : 0.0;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("username", user.login());
        data.put("name", user.name() != null ? user.name() : user.login());
        data.put("bio", user.bio());
        data.put("publicRepos", user.publicRepos());
        data.put("followers", user.followers());
        data.put("following", user.following());
        data.put("totalReposAnalyzed", totalRepos);
        data.put("forksCount", forksCount);
        data.put("totalStarsEarned", starsCount);
        data.put("abandonedReposCount", abandonedRepos);
        data.put("topLanguages", topLanguages);
        data.put("lateNightCommitRatio", lateNightRatio);

        return new ProviderStats(Provider.GITHUB, data);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubUser(
            String login,
            String name,
            String bio,
            @JsonProperty("public_repos") int publicRepos,
            int followers,
            int following
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubRepo(
            String name,
            boolean fork,
            String language,
            @JsonProperty("stargazers_count") int stargazersCount,
            @JsonProperty("pushed_at") OffsetDateTime pushedAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubEvent(
            String type,
            @JsonProperty("created_at") OffsetDateTime createdAt
    ) {}
}
