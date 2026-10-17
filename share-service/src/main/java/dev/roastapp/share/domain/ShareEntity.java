package dev.roastapp.share.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shares")
public class ShareEntity {

    @Id
    @Column(name = "id", length = 16, nullable = false)
    private String id;

    @Column(name = "roast_id", nullable = false)
    private UUID roastId;

    @Column(name = "image_url", length = 512, nullable = false)
    private String imageUrl;

    @Column(name = "roast_text", columnDefinition = "TEXT")
    private String roastText;

    @Column(name = "tone", length = 32)
    private String tone;

    @Column(name = "sources", length = 255)
    private String sources;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public ShareEntity() {
    }

    public ShareEntity(String id, UUID roastId, String imageUrl, String roastText, String tone, String sources) {
        this.id = id;
        this.roastId = roastId;
        this.imageUrl = imageUrl;
        this.roastText = roastText;
        this.tone = tone;
        this.sources = sources;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UUID getRoastId() {
        return roastId;
    }

    public void setRoastId(UUID roastId) {
        this.roastId = roastId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getRoastText() {
        return roastText;
    }

    public void setRoastText(String roastText) {
        this.roastText = roastText;
    }

    public String getTone() {
        return tone;
    }

    public void setTone(String tone) {
        this.tone = tone;
    }

    public String getSources() {
        return sources;
    }

    public void setSources(String sources) {
        this.sources = sources;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }
}
