package dev.roastapp.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final SecretKey key;
    private final String cookieName;

    // Public paths that do not require authentication
    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/api/v1/auth/",
            "/oauth2/",
            "/login/",
            "/s/",
            "/api/v1/public/",
            "/actuator/"
    );

    public AuthenticationFilter(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.cookie-name:roast_token}") String cookieName) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.cookieName = cookieName;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Block any direct access to internal endpoints from the outside world
        if (path.startsWith("/internal/")) {
            log.warn("Blocked external attempt to access internal route: {}", path);
            return onError(exchange, HttpStatus.FORBIDDEN);
        }

        // 2. Correlation ID: propagate or generate X-Request-Id
        String incomingRequestId = request.getHeaders().getFirst("X-Request-Id");
        final String requestId = (incomingRequestId != null && !incomingRequestId.isBlank())
                ? incomingRequestId
                : UUID.randomUUID().toString();

        // 3. Anti-Spoofing: always strip any client-supplied X-User-Id header
        ServerHttpRequest.Builder requestBuilder = request.mutate()
                .headers(httpHeaders -> {
                    httpHeaders.remove("X-User-Id");
                    httpHeaders.set("X-Request-Id", requestId);
                });

        // 4. Extract token from cookie (or Authorization header)
        String token = extractToken(request);
        String authenticatedUserId = null;

        if (token != null) {
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                authenticatedUserId = claims.getSubject();
                log.debug("Authenticated request for user {} on path {}", authenticatedUserId, path);
            } catch (Exception e) {
                log.debug("Invalid or expired JWT token: {}", e.getMessage());
            }
        }

        // 5. Check if route is protected
        boolean isPublic = PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);

        if (!isPublic) {
            if (authenticatedUserId == null) {
                log.warn("Unauthorized request rejected on path: {}", path);
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }
        }

        // 6. If authenticated, inject verified X-User-Id header for downstream microservices
        if (authenticatedUserId != null) {
            requestBuilder.header("X-User-Id", authenticatedUserId);
        }

        ServerHttpRequest modifiedRequest = requestBuilder.build();
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().set("X-Request-Id", requestId);

        return chain.filter(exchange.mutate().request(modifiedRequest).response(response).build());
    }

    private String extractToken(ServerHttpRequest request) {
        // Check cookie first
        HttpCookie cookie = request.getCookies().getFirst(cookieName);
        if (cookie != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }

        // Check Authorization: Bearer <token>
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -100; // Run early in the gateway filter chain
    }
}
