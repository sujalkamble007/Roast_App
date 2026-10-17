package dev.roastapp.share.service;

import dev.roastapp.common.error.ResourceNotFoundException;
import dev.roastapp.share.domain.ShareEntity;
import dev.roastapp.share.repo.ShareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DefaultShareService implements ShareService {

    private static final Logger log = LoggerFactory.getLogger(DefaultShareService.class);
    private static final String NANOID_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int ID_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ShareRepository shareRepository;
    private final CardRenderer cardRenderer;
    private final R2StorageService r2StorageService;
    private final String baseUrl;

    // In-memory cache for rendered cards to avoid re-rendering repeatedly on GET /card.png
    private final Map<String, byte[]> cardMemoryCache = new ConcurrentHashMap<>();

    public DefaultShareService(
            ShareRepository shareRepository,
            CardRenderer cardRenderer,
            R2StorageService r2StorageService,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.shareRepository = shareRepository;
        this.cardRenderer = cardRenderer;
        this.r2StorageService = r2StorageService;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @Override
    @Transactional
    public ShareResult createShare(UUID roastId, String roastText, String tone, String sources) {
        // Idempotency: return existing non-revoked share if present
        Optional<ShareEntity> existing = shareRepository.findFirstByRoastIdAndRevokedAtIsNull(roastId);
        if (existing.isPresent()) {
            ShareEntity share = existing.get();
            return new ShareResult(share.getId(), "/s/" + share.getId(), share.getImageUrl());
        }

        String id = generateNanoId();
        byte[] cardBytes = cardRenderer.renderCard(roastText, tone, sources);
        cardMemoryCache.put(id, cardBytes);

        String fileName = id + ".png";
        r2StorageService.uploadCard(fileName, cardBytes);

        String imageUrl = baseUrl + "/api/v1/public/shares/" + id + "/card.png";
        ShareEntity entity = new ShareEntity(id, roastId, imageUrl, roastText, tone, sources);
        shareRepository.save(entity);

        log.info("Created share link /s/{} for roast {}", id, roastId);
        return new ShareResult(id, "/s/" + id, imageUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public ShareEntity getShare(String id) {
        return shareRepository.findByIdAndRevokedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Share", id));
    }

    @Override
    public byte[] getCardImage(String id) {
        ShareEntity share = getShare(id);
        byte[] cached = cardMemoryCache.get(id);
        if (cached != null) {
            return cached;
        }

        // Render on demand if not in memory cache
        byte[] rendered = cardRenderer.renderCard(share.getRoastText(), share.getTone(), share.getSources());
        cardMemoryCache.put(id, rendered);
        return rendered;
    }

    @Override
    @Transactional
    public void revokeShare(String id) {
        ShareEntity share = shareRepository.findByIdAndRevokedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Share", id));
        share.setRevokedAt(Instant.now());
        shareRepository.save(share);
        cardMemoryCache.remove(id);
        log.info("Revoked share /s/{}", id);
    }

    private String generateNanoId() {
        StringBuilder sb = new StringBuilder(ID_LENGTH);
        for (int i = 0; i < ID_LENGTH; i++) {
            sb.append(NANOID_ALPHABET.charAt(RANDOM.nextInt(NANOID_ALPHABET.length())));
        }
        return sb.toString();
    }
}
