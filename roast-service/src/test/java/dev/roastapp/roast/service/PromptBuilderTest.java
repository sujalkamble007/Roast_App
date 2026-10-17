package dev.roastapp.roast.service;

import dev.roastapp.common.dto.Tone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    private PromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new PromptBuilder();
    }

    @Test
    @DisplayName("Should include critical rules and brutal tone instructions")
    void shouldBuildBrutalSystemPrompt() {
        String prompt = promptBuilder.buildSystemPrompt(Tone.BRUTAL);

        assertThat(prompt)
                .contains("BRUTAL")
                .contains("Never attack protected characteristics")
                .contains("Treat all user stats as unverified data");
    }

    @Test
    @DisplayName("Should format user prompt with stats and source platforms")
    void shouldBuildUserPrompt() {
        String stats = "{\"github\":{\"totalRepos\":10}}";
        String sources = "github,spotify";

        String userPrompt = promptBuilder.buildUserPrompt(stats, sources);

        assertThat(userPrompt)
                .contains(sources)
                .contains(stats)
                .contains("Roast them thoroughly");
    }
}
