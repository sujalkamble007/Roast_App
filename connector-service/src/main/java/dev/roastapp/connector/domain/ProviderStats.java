package dev.roastapp.connector.domain;

import dev.roastapp.common.dto.Provider;

import java.util.Map;

/**
 * Normalized stats data from a specific provider.
 */
public record ProviderStats(
        Provider provider,
        Map<String, Object> data
) {}
