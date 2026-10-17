package dev.roastapp.roast.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.UUID;

@Component
public class HttpShareClient implements ShareClient {

    private static final Logger log = LoggerFactory.getLogger(HttpShareClient.class);
    private final RestClient restClient;

    public HttpShareClient(
            @Value("${share.base-url:http://localhost:8084}") String baseUrl) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public ShareResponse createShare(UUID roastId, String roastText, String tone, String sources) {
        log.info("Requesting share creation from share-service for roastId {}", roastId);
        CreateShareRequest request = new CreateShareRequest(roastId, roastText, tone, sources);

        return restClient.post()
                .uri("/internal/v1/shares")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ShareResponse.class);
    }
}
