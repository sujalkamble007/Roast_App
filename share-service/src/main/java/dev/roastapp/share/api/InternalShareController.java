package dev.roastapp.share.api;

import dev.roastapp.share.service.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/shares")
@Tag(name = "Internal Shares", description = "Internal endpoint called by roast-service to create shareable cards")
public class InternalShareController {

    private final ShareService shareService;

    public InternalShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @PostMapping
    @Operation(summary = "Create shareable card for a roast", description = "Renders OG card, saves in DB/R2, and returns share URL.")
    public ResponseEntity<ShareDto.ShareResponse> createShare(@Valid @RequestBody ShareDto.CreateShareRequest request) {
        ShareService.ShareResult result = shareService.createShare(
                request.roastId(),
                request.roastText(),
                request.tone(),
                request.sources()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ShareDto.ShareResponse(result.id(), result.shareUrl(), result.imageUrl()));
    }
}
