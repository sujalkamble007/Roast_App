package dev.roastapp.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemStatusController {

    private final WebClient webClient;

    @Value("${IDENTITY_SERVICE_URL:http://localhost:8081}")
    private String identityUrl;

    @Value("${CONNECTOR_SERVICE_URL:http://localhost:8082}")
    private String connectorUrl;

    @Value("${ROAST_SERVICE_URL:http://localhost:8083}")
    private String roastUrl;

    @Value("${SHARE_SERVICE_URL:http://localhost:8084}")
    private String shareUrl;

    public SystemStatusController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @GetMapping("/status")
    public Mono<Map<String, Object>> getSystemStatus() {
        return Mono.zip(
                checkService("identity", identityUrl + "/actuator/health"),
                checkService("connector", connectorUrl + "/actuator/health"),
                checkService("roast", roastUrl + "/actuator/health"),
                checkService("share", shareUrl + "/actuator/health")
        ).map(tuple -> {
            Map<String, String> services = new HashMap<>();
            services.put("gateway", "UP");
            services.put("identity", tuple.getT1());
            services.put("connector", tuple.getT2());
            services.put("roast", tuple.getT3());
            services.put("share", tuple.getT4());

            long upCount = services.values().stream().filter("UP"::equals).count();

            Map<String, Object> response = new HashMap<>();
            response.put("status", upCount == 5 ? "UP" : "STARTING");
            response.put("upCount", upCount);
            response.put("totalCount", 5);
            response.put("services", services);
            return response;
        });
    }

    private Mono<String> checkService(String name, String url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .map(body -> "UP")
                .timeout(Duration.ofMillis(1500))
                .onErrorReturn("STARTING");
    }
}
