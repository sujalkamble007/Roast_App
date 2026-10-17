package dev.roastapp.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationFilterTest {

    private final String secret = "d4cad02423833c029e7d8dc378f8fed1a883b73c70a4a0566e9ddc1c2337fe80";
    private final String cookieName = "roast_token";
    private AuthenticationFilter filter;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        this.filter = new AuthenticationFilter(secret, cookieName);
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Should block access to /internal routes with 403 Forbidden")
    void shouldBlockInternalRoutes() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/internal/v1/stats").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should return 401 Unauthorized for protected route without JWT cookie")
    void shouldRejectUnauthenticatedRequest() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/roasts").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should strip spoofed X-User-Id and inject verified subject from JWT")
    void shouldInjectVerifiedUserIdAndStripSpoof() {
        String genuineUserId = "usr_real_account_999";
        String spoofedUserId = "usr_fake_admin_hacker";

        String jwt = Jwts.builder()
                .subject(genuineUserId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/roasts")
                .header("X-User-Id", spoofedUserId)
                .cookie(new HttpCookie(cookieName, jwt))
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<String> downstreamUserId = new AtomicReference<>();

        GatewayFilterChain chain = ex -> {
            downstreamUserId.set(ex.getRequest().getHeaders().getFirst("X-User-Id"));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(downstreamUserId.get())
                .isNotNull()
                .isEqualTo(genuineUserId)
                .isNotEqualTo(spoofedUserId);
    }

    @Test
    @DisplayName("Should allow public route without authentication")
    void shouldAllowPublicRoute() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/github").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<Boolean> chainExecuted = new AtomicReference<>(false);
        GatewayFilterChain chain = ex -> {
            chainExecuted.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(chainExecuted.get()).isTrue();
    }
}
