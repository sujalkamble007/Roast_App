package dev.roastapp.identity.config;

import dev.roastapp.common.dto.Provider;
import dev.roastapp.identity.domain.LinkedAccountEntity;
import dev.roastapp.identity.domain.UserEntity;
import dev.roastapp.identity.repo.LinkedAccountRepository;
import dev.roastapp.identity.repo.UserRepository;
import dev.roastapp.identity.service.JwtService;
import dev.roastapp.identity.service.TokenEncryptionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final UserRepository userRepository;
    private final LinkedAccountRepository linkedAccountRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final TokenEncryptionService encryptionService;
    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;
    private final String frontendUrl;
    private final String cookieName;

    public OAuth2AuthenticationSuccessHandler(
            UserRepository userRepository,
            LinkedAccountRepository linkedAccountRepository,
            OAuth2AuthorizedClientService authorizedClientService,
            TokenEncryptionService encryptionService,
            StringRedisTemplate redisTemplate,
            JwtService jwtService,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
            @Value("${jwt.cookie-name:roast_token}") String cookieName) {
        this.userRepository = userRepository;
        this.linkedAccountRepository = linkedAccountRepository;
        this.authorizedClientService = authorizedClientService;
        this.encryptionService = encryptionService;
        this.redisTemplate = redisTemplate;
        this.jwtService = jwtService;
        this.frontendUrl = frontendUrl;
        this.cookieName = cookieName;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        Provider provider = "google".equalsIgnoreCase(registrationId)
                ? Provider.YOUTUBE
                : Provider.valueOf(registrationId.toUpperCase());
        OAuth2User oauthUser = oauthToken.getPrincipal();

        // 1. Extract provider-specific user details
        String providerUserId = extractProviderUserId(provider, oauthUser);
        String email = extractEmail(oauthUser);
        String displayName = extractDisplayName(provider, oauthUser);
        String avatarUrl = extractAvatarUrl(provider, oauthUser);

        log.info("Processing OAuth2 login for provider {} and providerUserId {}", provider, providerUserId);

        // 2. Find or create User & LinkedAccount
        UserEntity user = linkedAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(LinkedAccountEntity::getUser)
                .orElseGet(() -> {
                    String newUserId = "usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                    UserEntity newUser = new UserEntity(newUserId, email, displayName, avatarUrl);
                    userRepository.save(newUser);

                    LinkedAccountEntity linked = new LinkedAccountEntity(UUID.randomUUID(), newUser, provider, providerUserId);
                    linkedAccountRepository.save(linked);
                    log.info("Created new user {} linked with {}", newUserId, provider);
                    return newUser;
                });

        // 3. Extract and encrypt provider access token, store in Redis with 15min TTL
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                registrationId,
                authentication.getName()
        );

        if (client != null && client.getAccessToken() != null) {
            String rawToken = client.getAccessToken().getTokenValue();
            String encryptedToken = encryptionService.encrypt(rawToken);

            String redisKey = "tok:" + user.getId() + ":" + provider.name().toLowerCase();
            try {
                redisTemplate.opsForValue().set(redisKey, encryptedToken, Duration.ofMinutes(15));
                log.info("Stored encrypted access token in Redis key {} with 15min TTL", redisKey);
            } catch (Exception e) {
                log.warn("Could not save token to Redis (Redis may not be running locally yet): {}", e.getMessage());
            }
        }

        // 4. Generate JWT & set httpOnly Cookie
        String jwt = jwtService.generateToken(user.getId(), user.getEmail(), user.getDisplayName());

        Cookie cookie = new Cookie(cookieName, jwt);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // set to true in production over HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(3600); // 1 hour
        response.addCookie(cookie);

        // 5. Redirect to frontend preserving host origin (localhost vs 127.0.0.1)
        String host = request.getHeader("Host");
        String targetBaseUrl = (host != null && host.contains("127.0.0.1"))
                ? "http://127.0.0.1:5173"
                : frontendUrl;
        getRedirectStrategy().sendRedirect(request, response, targetBaseUrl + "/roast");
    }

    private String extractProviderUserId(Provider provider, OAuth2User user) {
        Map<String, Object> attrs = user.getAttributes();
        return switch (provider) {
            case GITHUB -> String.valueOf(attrs.get("id"));
            case SPOTIFY -> String.valueOf(attrs.get("id"));
            case YOUTUBE -> String.valueOf(attrs.get("sub"));
        };
    }

    private String extractEmail(OAuth2User user) {
        return (String) user.getAttributes().get("email");
    }

    private String extractDisplayName(Provider provider, OAuth2User user) {
        Map<String, Object> attrs = user.getAttributes();
        return switch (provider) {
            case GITHUB -> (String) attrs.getOrDefault("name", attrs.get("login"));
            case SPOTIFY -> (String) attrs.getOrDefault("display_name", attrs.get("id"));
            case YOUTUBE -> (String) attrs.getOrDefault("name", "User");
        };
    }

    private String extractAvatarUrl(Provider provider, OAuth2User user) {
        Map<String, Object> attrs = user.getAttributes();
        return switch (provider) {
            case GITHUB -> (String) attrs.get("avatar_url");
            case SPOTIFY -> {
                if (attrs.get("images") instanceof java.util.List<?> list && !list.isEmpty()) {
                    if (list.get(0) instanceof Map<?, ?> map) {
                        yield (String) map.get("url");
                    }
                }
                yield null;
            }
            case YOUTUBE -> (String) attrs.get("picture");
        };
    }
}
