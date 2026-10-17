package dev.roastapp.identity.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdentitySecurityTest {

    private final String secret = "d4cad02423833c029e7d8dc378f8fed1a883b73c70a4a0566e9ddc1c2337fe80";
    private final JwtService jwtService = new JwtService(secret, 60);
    private final TokenEncryptionService encryptionService = new TokenEncryptionService(secret);

    @Test
    @DisplayName("Should generate valid JWT and parse claims correctly")
    void shouldGenerateAndValidateJwt() {
        String token = jwtService.generateToken("user-123", "test@example.com", "Test User");

        assertThat(token).isNotBlank();

        Claims claims = jwtService.validateAndParseClaims(token);
        assertThat(claims.getSubject()).isEqualTo("user-123");
        assertThat(claims.get("email")).isEqualTo("test@example.com");
        assertThat(claims.get("displayName")).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Should encrypt and decrypt tokens using AES-GCM")
    void shouldEncryptAndDecryptToken() {
        String originalToken = "gho_abc123supersecretaccess_token_from_github";

        String encrypted = encryptionService.encrypt(originalToken);
        assertThat(encrypted).isNotEqualTo(originalToken);

        String decrypted = encryptionService.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(originalToken);
    }
}
