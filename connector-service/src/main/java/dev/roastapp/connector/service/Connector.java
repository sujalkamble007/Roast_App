package dev.roastapp.connector.service;

import dev.roastapp.common.dto.Provider;
import dev.roastapp.connector.domain.ProviderStats;

/**
 * Pluggable connector interface.
 * Each provider (GitHub, Spotify, YouTube) implements this interface.
 */
public interface Connector {

    Provider provider();

    ProviderStats fetch(String accessToken);
}
