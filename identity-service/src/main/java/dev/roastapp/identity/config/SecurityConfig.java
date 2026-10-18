package dev.roastapp.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SecurityConfig.class);

    private final OAuth2AuthenticationSuccessHandler successHandler;
    private final String frontendUrl;

    public SecurityConfig(
            OAuth2AuthenticationSuccessHandler successHandler,
            @org.springframework.beans.factory.annotation.Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.successHandler = successHandler;
        this.frontendUrl = frontendUrl;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/me",
                                "/actuator/**",
                                "/oauth2/**",
                                "/login/**",
                                "/login"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(successHandler)
                        .failureHandler((request, response, exception) -> {
                            log.error("OAuth2 Login failed: {}", exception.getMessage(), exception);
                            String redirectUrl = frontendUrl + "/?auth_error=" + java.net.URLEncoder.encode(exception.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
                            response.sendRedirect(redirectUrl);
                        })
                );

        return http.build();
    }
}
