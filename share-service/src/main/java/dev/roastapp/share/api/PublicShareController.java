package dev.roastapp.share.api;

import dev.roastapp.share.domain.ShareEntity;
import dev.roastapp.share.service.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Public Shares", description = "Public endpoints for OpenGraph preview cards and SPA redirect")
public class PublicShareController {

    private final ShareService shareService;

    public PublicShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @GetMapping(value = "/s/{id}", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "SSR HTML for OpenGraph preview and redirect to SPA", description = "Provides og:image tags for WhatsApp/X/LinkedIn bots, then redirects browser to /r/{id}")
    public ResponseEntity<String> getSharePage(@PathVariable String id) {
        ShareEntity share = shareService.getShare(id);

        String toneLabel = (share.getTone() != null ? share.getTone().toUpperCase() : "SAVAGE");
        String safeRoastText = escapeHtml(share.getRoastText());
        String safeImageUrl = escapeHtml(share.getImageUrl());
        String spaRedirectUrl = "/r/" + id;

        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s Roast | Roast App</title>
                    <meta name="description" content="%s">
                    
                    <!-- OpenGraph / Facebook / WhatsApp / LinkedIn -->
                    <meta property="og:type" content="website">
                    <meta property="og:title" content="%s Roast | Roast App">
                    <meta property="og:description" content="%s">
                    <meta property="og:image" content="%s">
                    <meta property="og:image:width" content="1200">
                    <meta property="og:image:height" content="630">
                    
                    <!-- Twitter -->
                    <meta name="twitter:card" content="summary_large_image">
                    <meta name="twitter:title" content="%s Roast | Roast App">
                    <meta name="twitter:description" content="%s">
                    <meta name="twitter:image" content="%s">
                    
                    <!-- Instant client redirect to SPA route -->
                    <meta http-equiv="refresh" content="0;url=%s">
                    <script>
                        window.location.replace("%s");
                    </script>
                    <style>
                        body {
                            background-color: #0f172a;
                            color: #f8fafc;
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            height: 100vh;
                            margin: 0;
                        }
                        a { color: #f97316; }
                    </style>
                </head>
                <body>
                    <p>Redirecting to roast... If you are not redirected, <a href="%s">click here</a>.</p>
                </body>
                </html>
                """.formatted(
                toneLabel, safeRoastText,
                toneLabel, safeRoastText, safeImageUrl,
                toneLabel, safeRoastText, safeImageUrl,
                spaRedirectUrl, spaRedirectUrl, spaRedirectUrl
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(html);
    }

    @GetMapping("/api/v1/public/shares/{id}")
    @Operation(summary = "Get public roast share details for SPA", description = "Returns roast data and card image URL.")
    public ResponseEntity<ShareDto.ShareDetails> getShareDetails(@PathVariable String id) {
        ShareEntity share = shareService.getShare(id);
        ShareDto.ShareDetails details = new ShareDto.ShareDetails(
                share.getId(),
                share.getRoastId(),
                share.getImageUrl(),
                share.getRoastText(),
                share.getTone(),
                share.getSources(),
                share.getCreatedAt()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=60")
                .body(details);
    }

    @GetMapping(value = "/api/v1/public/shares/{id}/card.png", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Get the 1200x630 OG card image", description = "Serves the high-resolution generated OG preview card.")
    public ResponseEntity<byte[]> getCardImage(@PathVariable String id) {
        byte[] imageBytes = shareService.getCardImage(id);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .body(imageBytes);
    }

    @DeleteMapping("/api/v1/public/shares/{id}")
    @Operation(summary = "Revoke a public share link", description = "Marks the share as revoked. Subsequent requests will return 404.")
    public ResponseEntity<Void> revokeShare(@PathVariable String id) {
        shareService.revokeShare(id);
        return ResponseEntity.noContent().build();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
