package dev.roastapp.common.dto;

import java.util.List;
import java.util.Map;

/**
 * Normalized stats model produced by connector-service.
 */
public record UserStatsResponse(
        String userId,
        Map<String, Object> stats,
        List<String> failedProviders
) {}
