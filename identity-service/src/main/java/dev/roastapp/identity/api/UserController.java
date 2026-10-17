package dev.roastapp.identity.api;

import dev.roastapp.common.error.ResourceNotFoundException;
import dev.roastapp.identity.domain.UserEntity;
import dev.roastapp.identity.repo.UserRepository;
import dev.roastapp.identity.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final String cookieName;

    public UserController(
            UserRepository userRepository,
            JwtService jwtService,
            @Value("${jwt.cookie-name:roast_token}") String cookieName) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.cookieName = cookieName;
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<IdentityDto.UserProfileResponse> getMe(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            HttpServletRequest request) {

        String userId = resolveUserId(headerUserId, request);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        List<String> connectedProviders = user.getLinkedAccounts().stream()
                .map(a -> a.getProvider().name().toLowerCase())
                .toList();

        IdentityDto.UserProfileResponse profile = new IdentityDto.UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                connectedProviders,
                user.getCreatedAt()
        );

        return ResponseEntity.ok(profile);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        log.info("Cleared auth cookie on logout");
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    @Transactional
    public ResponseEntity<Void> deleteMe(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            HttpServletRequest request,
            HttpServletResponse response) {

        String userId = resolveUserId(headerUserId, request);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        userRepository.findById(userId).ifPresent(userRepository::delete);
        log.info("Deleted account for user {}", userId);

        logout(response);
        return ResponseEntity.noContent().build();
    }

    private String resolveUserId(String headerUserId, HttpServletRequest request) {
        if (headerUserId != null && !headerUserId.isBlank()) {
            return headerUserId;
        }

        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(c -> cookieName.equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .map(token -> {
                        try {
                            Claims claims = jwtService.validateAndParseClaims(token);
                            return claims.getSubject();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .orElse(null);
        }

        return null;
    }
}
