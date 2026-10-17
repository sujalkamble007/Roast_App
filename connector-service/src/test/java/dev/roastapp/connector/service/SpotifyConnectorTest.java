package dev.roastapp.connector.service;

import dev.roastapp.common.dto.Provider;
import dev.roastapp.connector.domain.ProviderStats;
import dev.roastapp.connector.service.impl.SpotifyConnector;
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

class SpotifyConnectorTest {

    @Test
    @DisplayName("Should fetch and normalize Spotify top artists, genres and late-night listening")
    void shouldNormalizeSpotifyStats() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.spotify.com/v1");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        SpotifyConnector connector = new SpotifyConnector(builder.build());

        // Mock /me/top/artists
        mockServer.expect(requestTo("https://api.spotify.com/v1/me/top/artists?limit=15&time_range=medium_term"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-sp-token"))
                .andRespond(withSuccess(
                        """
                        {
                          "items": [
                            {"name": "Radiohead", "genres": ["art rock", "alternative rock"], "popularity": 80},
                            {"name": "Lofi Girl", "genres": ["lo-fi", "chillhop"], "popularity": 70}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        // Mock /me/top/tracks
        mockServer.expect(requestTo("https://api.spotify.com/v1/me/top/tracks?limit=10&time_range=medium_term"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-sp-token"))
                .andRespond(withSuccess(
                        """
                        {
                          "items": [
                            {"name": "Karma Police", "artists": [{"name": "Radiohead"}]}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        // Mock /me/player/recently-played
        mockServer.expect(requestTo("https://api.spotify.com/v1/me/player/recently-played?limit=50"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-sp-token"))
                .andRespond(withSuccess(
                        """
                        {
                          "items": [
                            {"played_at": "2026-10-17T01:30:00Z"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        ProviderStats stats = connector.fetch("test-sp-token");

        mockServer.verify();

        assertThat(stats.provider()).isEqualTo(Provider.SPOTIFY);
        @SuppressWarnings("unchecked")
        List<String> topArtists = (List<String>) stats.data().get("topArtists");
        assertThat(topArtists).contains("Radiohead", "Lofi Girl");

        @SuppressWarnings("unchecked")
        List<String> topGenres = (List<String>) stats.data().get("topGenres");
        assertThat(topGenres).contains("art rock", "lo-fi");

        assertThat(stats.data()).containsEntry("averageArtistPopularity", 75L);
        assertThat(stats.data()).containsEntry("lateNightListeningPercentage", 100L);
    }
}
