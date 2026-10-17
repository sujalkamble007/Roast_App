package dev.roastapp.roast.client;

import dev.roastapp.common.dto.UserStatsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

/**
 * REST client for calling connector-service internal endpoint.
 * Configured with a 5-second timeout as specified in the ground rules.
 */
@Component
public class ConnectorClient {

    private static final Logger log = LoggerFactory.getLogger(ConnectorClient.class);
    private final RestClient restClient;

    public ConnectorClient(
            @Value("${connector.base-url:http://localhost:8082}") String baseUrl) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public UserStatsResponse fetchStats(String userId, List<String> providers) {
        String providersParam = String.join(",", providers);
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/v1/stats")
                            .queryParam("userId", userId)
                            .queryParam("providers", providersParam)
                            .build())
                    .retrieve()
                    .body(UserStatsResponse.class);
        } catch (Exception e) {
            log.warn("connector-service call failed for user {} ({}), falling back gracefully", userId, e.getMessage());
            return null;
        }
    }
}
