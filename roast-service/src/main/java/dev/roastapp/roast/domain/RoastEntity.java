package dev.roastapp.roast.domain;

import dev.roastapp.common.dto.RoastStatus;
import dev.roastapp.common.dto.Tone;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "roasts")
public class RoastEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private RoastStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "tone", nullable = false, length = 32)
    private Tone tone;

    @Column(name = "sources", nullable = false)
    private String sources;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "stats_json", nullable = false, columnDefinition = "jsonb")
    private String statsJson;

    @Column(name = "stats_hash", nullable = false, length = 64)
    private String statsHash;

    @Column(name = "roast_text", columnDefinition = "text")
    private String roastText;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "prompt_version", nullable = false, length = 32)
    private String promptVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public RoastEntity() {
    }

    public RoastEntity(UUID id, String userId, RoastStatus status, Tone tone, String sources,
                       String statsJson, String statsHash, String promptVersion) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.tone = tone;
        this.sources = sources;
        this.statsJson = statsJson;
        this.statsHash = statsHash;
        this.promptVersion = promptVersion;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public RoastStatus getStatus() {
        return status;
    }

    public void setStatus(RoastStatus status) {
        this.status = status;
    }

    public Tone getTone() {
        return tone;
    }

    public void setTone(Tone tone) {
        this.tone = tone;
    }

    public String getSources() {
        return sources;
    }

    public void setSources(String sources) {
        this.sources = sources;
    }

    public String getStatsJson() {
        return statsJson;
    }

    public void setStatsJson(String statsJson) {
        this.statsJson = statsJson;
    }

    public String getStatsHash() {
        return statsHash;
    }

    public void setStatsHash(String statsHash) {
        this.statsHash = statsHash;
    }

    public String getRoastText() {
        return roastText;
    }

    public void setRoastText(String roastText) {
        this.roastText = roastText;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
