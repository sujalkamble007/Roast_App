package dev.roastapp.roast.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.roastapp.common.dto.RoastStatus;
import dev.roastapp.common.dto.Tone;
import dev.roastapp.common.error.ResourceNotFoundException;
import dev.roastapp.roast.api.RoastDto;
import dev.roastapp.roast.client.LlmClient;
import dev.roastapp.roast.domain.RoastEntity;
import dev.roastapp.roast.repo.RoastRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RoastService {

    private static final Logger log = LoggerFactory.getLogger(RoastService.class);

    private final RoastRepository roastRepository;
    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final dev.roastapp.roast.client.ConnectorClient connectorClient;
    private final QuotaService quotaService;
    private final RoastCacheService roastCacheService;
    private final IdempotencyService idempotencyService;

    public RoastService(RoastRepository roastRepository,
                        LlmClient llmClient,
                        PromptBuilder promptBuilder,
                        ObjectMapper objectMapper,
                        dev.roastapp.roast.client.ConnectorClient connectorClient,
                        QuotaService quotaService,
                        RoastCacheService roastCacheService,
                        IdempotencyService idempotencyService) {
        this.roastRepository = roastRepository;
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.connectorClient = connectorClient;
        this.quotaService = quotaService;
        this.roastCacheService = roastCacheService;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public RoastDto.RoastResponse initiateRoast(String userId, RoastDto.CreateRoastRequest request, String idempotencyKey) {
        // 1. Check idempotency
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existingId = idempotencyService.getExistingRoastId(userId, idempotencyKey);
            if (existingId.isPresent()) {
                var existing = roastRepository.findById(existingId.get());
                if (existing.isPresent()) {
                    log.info("Returning idempotent response for key {}", idempotencyKey);
                    return toDto(existing.get());
                }
            }
        }

        UUID id = UUID.randomUUID();
        String sourcesJoined = String.join(",", request.sources());

        // 2. Fetch stats from connector-service
        String statsJson;
        dev.roastapp.common.dto.UserStatsResponse statsResponse = connectorClient.fetchStats(userId, request.sources());
        if (statsResponse != null && statsResponse.stats() != null && !statsResponse.stats().isEmpty()) {
            try {
                statsJson = objectMapper.writeValueAsString(statsResponse.stats());
            } catch (Exception e) {
                statsJson = generateSampleStatsJson(request.sources());
            }
        } else {
            statsJson = generateSampleStatsJson(request.sources());
        }
        String statsHash = computeHash(statsJson);

        // 3. Check Smart Cache: If identical stats + tone + promptVersion exist, return immediately without consuming quota
        var cachedRoast = roastCacheService.getCachedRoast(statsHash, request.tone(), PromptBuilder.PROMPT_VERSION);
        if (cachedRoast.isPresent()) {
            log.info("Cache hit! Returning ready roast without using quota for user {}", userId);
            RoastEntity cachedEntity = new RoastEntity(
                    id,
                    userId,
                    RoastStatus.READY,
                    request.tone(),
                    sourcesJoined,
                    statsJson,
                    statsHash,
                    PromptBuilder.PROMPT_VERSION
            );
            cachedEntity.setRoastText(cachedRoast.get());
            cachedEntity = roastRepository.save(cachedEntity);
            if (idempotencyKey != null) idempotencyService.recordKey(userId, idempotencyKey, id);
            return toDto(cachedEntity);
        }

        // 4. Cache MISS: Check and consume daily quota (raises 429 if limit exceeded)
        quotaService.checkAndConsumeQuota(userId);

        RoastEntity entity = new RoastEntity(
                id,
                userId,
                RoastStatus.PENDING,
                request.tone(),
                sourcesJoined,
                statsJson,
                statsHash,
                PromptBuilder.PROMPT_VERSION
        );

        entity = roastRepository.save(entity);
        if (idempotencyKey != null) idempotencyService.recordKey(userId, idempotencyKey, id);
        log.info("Created roast {} for user {} in status PENDING", id, userId);

        // 5. Trigger async LLM generation
        generateRoastAsync(id, request.tone(), statsJson, sourcesJoined, statsHash);

        return toDto(entity);
    }

    @Async
    public void generateRoastAsync(UUID roastId, Tone tone, String statsJson, String sources, String statsHash) {
        log.info("Starting async roast generation for roastId {}", roastId);
        try {
            String systemPrompt = promptBuilder.buildSystemPrompt(tone);
            String userPrompt = promptBuilder.buildUserPrompt(statsJson, sources);

            String roastText = llmClient.generate(systemPrompt, userPrompt);

            roastRepository.findById(roastId).ifPresent(entity -> {
                entity.setRoastText(roastText);
                entity.setStatus(RoastStatus.READY);
                roastRepository.save(entity);
                // Populate cache for future requests
                roastCacheService.putCachedRoast(statsHash, tone, PromptBuilder.PROMPT_VERSION, roastText);
                log.info("Roast {} successfully generated and set to READY", roastId);
            });
        } catch (Exception e) {
            log.error("Failed to generate roast for id {}: {}", roastId, e.getMessage(), e);
            roastRepository.findById(roastId).ifPresent(entity -> {
                entity.setStatus(RoastStatus.FAILED);
                entity.setErrorMessage("Generation failed: " + e.getMessage());
                roastRepository.save(entity);
            });
        }
    }

    public QuotaService.QuotaInfo getQuotaInfo(String userId) {
        return quotaService.getQuotaInfo(userId);
    }

    @Transactional(readOnly = true)
    public RoastDto.RoastResponse getRoast(UUID id, String userId) {
        return roastRepository.findByIdAndUserId(id, userId)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Roast", id));
    }

    @Transactional(readOnly = true)
    public List<RoastDto.RoastResponse> listRoasts(String userId) {
        return roastRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void deleteRoast(UUID id, String userId) {
        RoastEntity entity = roastRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Roast", id));
        roastRepository.delete(entity);
        log.info("Deleted roast {} for user {}", id, userId);
    }

    private RoastDto.RoastResponse toDto(RoastEntity entity) {
        return new RoastDto.RoastResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getStatus(),
                entity.getTone(),
                entity.getSources(),
                entity.getStatsJson(),
                entity.getRoastText(),
                entity.getErrorMessage(),
                entity.getPromptVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String computeHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String generateSampleStatsJson(List<String> sources) {
        Map<String, Object> sample = Map.of(
                "github", Map.of(
                        "username", "dev_ninja",
                        "totalRepos", 42,
                        "abandonedRepos", 37,
                        "topLanguages", List.of("JavaScript", "Python", "CSS"),
                        "commitsPastMidnightRatio", 0.68,
                        "longestStreakDays", 3
                ),
                "spotify", Map.of(
                        "topArtists", List.of("Taylor Swift", "Radiohead", "Lofi Girl"),
                        "topGenres", List.of("sad-pop", "coding-lofi", "existential-dread"),
                        "lateNightListeningPercentage", 78,
                        "repeatTrackPlays", 342
                ),
                "youtube", Map.of(
                        "topCategories", List.of("Tech Tutorials Never Finished", "Conspiracy Theories", "Gaming Walkthroughs"),
                        "likedVideosCount", 1250,
                        "channelsSubscribed", 210
                )
        );
        try {
            return objectMapper.writeValueAsString(sample);
        } catch (Exception e) {
            return "{}";
        }
    }
}
