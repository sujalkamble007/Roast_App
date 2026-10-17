package dev.roastapp.roast.client;

/**
 * Interface defining LLM prompt generation.
 * This decouples the roast service from the specific LLM provider.
 */
public interface LlmClient {

    /**
     * Sends prompt to LLM and returns generated roast text.
     */
    String generate(String systemPrompt, String userPrompt);
}
