package dev.roastapp.roast.api;

import dev.roastapp.roast.service.RoastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roasts")
@Tag(name = "Roasts", description = "Endpoints for initiating, polling, and managing roasts")
public class RoastController {

    private final RoastService roastService;

    public RoastController(RoastService roastService) {
        this.roastService = roastService;
    }

    @PostMapping
    @Operation(summary = "Initiate a roast", description = "Returns 202 Accepted. The roast is generated asynchronously.")
    public ResponseEntity<RoastDto.RoastResponse> createRoast(
            @RequestHeader(value = "X-User-Id", defaultValue = "user-dev-demo") String userId,
            @Valid @RequestBody RoastDto.CreateRoastRequest request) {

        RoastDto.RoastResponse response = roastService.initiateRoast(userId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a roast by ID", description = "Used by frontend client to poll for roast readiness.")
    public ResponseEntity<RoastDto.RoastResponse> getRoast(
            @RequestHeader(value = "X-User-Id", defaultValue = "user-dev-demo") String userId,
            @PathVariable UUID id) {

        return ResponseEntity.ok(roastService.getRoast(id, userId));
    }

    @GetMapping
    @Operation(summary = "List user roasts", description = "Returns history of all roasts generated for the user.")
    public ResponseEntity<List<RoastDto.RoastResponse>> listRoasts(
            @RequestHeader(value = "X-User-Id", defaultValue = "user-dev-demo") String userId) {

        return ResponseEntity.ok(roastService.listRoasts(userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a roast", description = "Deletes a roast from the user's history.")
    public ResponseEntity<Void> deleteRoast(
            @RequestHeader(value = "X-User-Id", defaultValue = "user-dev-demo") String userId,
            @PathVariable UUID id) {

        roastService.deleteRoast(id, userId);
        return ResponseEntity.noContent().build();
    }
}
