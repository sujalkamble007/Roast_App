package dev.roastapp.roast.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Groq LLM client using Spring 6 RestClient.
 * Supports multiple API keys with round-robin load-balancing and automatic failover on rate limits (429).
 */
@Component
public class GroqLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GroqLlmClient.class);

    private final RestClient restClient;
    private final List<String> apiKeys;
    private final String model;
    private final AtomicInteger keyIndex = new AtomicInteger(0);

    public GroqLlmClient(
            @Value("${groq.api-keys}") String apiKeysConfig,
            @Value("${groq.model:llama-3.3-70b-versatile}") String model,
            @Value("${groq.base-url:https://api.groq.com/openai/v1}") String baseUrl) {
        
        this.apiKeys = Arrays.stream(apiKeysConfig.split(","))
                .map(String::trim)
                .filter(k -> !k.isBlank())
                .toList();

        if (this.apiKeys.isEmpty()) {
            throw new IllegalStateException("At least one Groq API key must be configured in 'groq.api-keys'.");
        }

        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("Initialized GroqLlmClient with {} API key(s) and model '{}'", this.apiKeys.size(), model);
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        int attempts = 0;
        int maxAttempts = apiKeys.size() * 2; // Allow trying each key with retry

        Exception lastException = null;

        while (attempts < maxAttempts) {
            int currentIdx = Math.abs(keyIndex.getAndIncrement() % apiKeys.size());
            String apiKey = apiKeys.get(currentIdx);
            attempts++;

            try {
                ChatCompletionRequest request = new ChatCompletionRequest(
                        model,
                        List.of(
                                new ChatMessage("system", systemPrompt),
                                new ChatMessage("user", userPrompt)
                        ),
                        0.85,
                        1024
                );

                ChatCompletionResponse response = restClient.post()
                        .uri("/chat/completions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .body(request)
                        .retrieve()
                        .body(ChatCompletionResponse.class);

                if (response != null && !response.choices().isEmpty()) {
                    return response.choices().get(0).message().content();
                } else {
                    throw new RuntimeException("Received empty response from Groq API");
                }
            } catch (HttpClientErrorException.TooManyRequests e) {
                log.warn("Groq API rate limit (429) hit on key index {}. Failing over to next key.", currentIdx);
                lastException = e;
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                log.error("Groq API error on key index {}: {} - {}", currentIdx, e.getStatusCode(), e.getResponseBodyAsString());
                lastException = e;
            } catch (Exception e) {
                log.error("Unexpected error contacting Groq API on key index {}: {}", currentIdx, e.getMessage());
                lastException = e;
            }
        }

        throw new RuntimeException("Failed to generate roast with Groq LLM after " + attempts + " attempts", lastException);
    }

    // DTOs for OpenAI/Groq compatible chat completions
    public record ChatMessage(String role, String content) {}

    public record ChatCompletionRequest(
            String model,
            List<ChatMessage> messages,
            double temperature,
            @JsonProperty("max_tokens") int maxTokens
    ) {}

    public record ChatCompletionResponse(
            String id,
            List<Choice> choices
    ) {}

    public record Choice(
            int index,
            ChatMessage message,
            @JsonProperty("finish_reason") String finishReason
    ) {}
}
