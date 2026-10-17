package dev.roastapp.connector.service;

import dev.roastapp.common.dto.Provider;
import dev.roastapp.connector.domain.ProviderStats;
import dev.roastapp.connector.service.impl.GitHubConnector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubConnectorTest {

    @Test
    @DisplayName("Should fetch and normalize GitHub user repos and languages")
    void shouldNormalizeGitHubStats() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.github.com");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        GitHubConnector connector = new GitHubConnector(builder.build());

        // Mock /user
        mockServer.expect(requestTo("https://api.github.com/user"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-gh-token"))
                .andRespond(withSuccess(
                        """
                        {
                          "login": "octocat",
                          "name": "The Octocat",
                          "bio": "Coding 24/7",
                          "public_repos": 15,
                          "followers": 100,
                          "following": 10
                        }
                        """, MediaType.APPLICATION_JSON));

        // Mock /user/repos
        mockServer.expect(requestTo("https://api.github.com/user/repos?per_page=100&sort=updated"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-gh-token"))
                .andRespond(withSuccess(
                        """
                        [
                          {
                            "name": "repo1",
                            "fork": false,
                            "language": "Java",
                            "stargazers_count": 10,
                            "pushed_at": "2026-10-10T00:00:00Z"
                          },
                          {
                            "name": "repo2",
                            "fork": true,
                            "language": "TypeScript",
                            "stargazers_count": 5,
                            "pushed_at": "2024-01-01T00:00:00Z"
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        // Mock /users/octocat/events
        mockServer.expect(requestTo("https://api.github.com/users/octocat/events?per_page=50"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-gh-token"))
                .andRespond(withSuccess(
                        """
                        [
                          {
                            "type": "PushEvent",
                            "created_at": "2026-03-01T02:30:00Z"
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        ProviderStats stats = connector.fetch("test-gh-token");

        mockServer.verify();

        assertThat(stats.provider()).isEqualTo(Provider.GITHUB);
        assertThat(stats.data()).containsEntry("username", "octocat");
        assertThat(stats.data()).containsEntry("totalStarsEarned", 15);
        assertThat(stats.data()).containsEntry("totalReposAnalyzed", 2);
        assertThat(stats.data()).containsEntry("forksCount", 1);
        assertThat(stats.data()).containsEntry("abandonedReposCount", 1);
        @SuppressWarnings("unchecked")
        List<String> topLanguages = (List<String>) stats.data().get("topLanguages");
        assertThat(topLanguages).contains("Java", "TypeScript");
        assertThat(stats.data()).containsEntry("lateNightCommitRatio", 1.0);
    }
}
