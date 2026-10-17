package dev.roastapp.share.api;

import dev.roastapp.common.error.GlobalExceptionHandler;
import dev.roastapp.common.error.ResourceNotFoundException;
import dev.roastapp.share.domain.ShareEntity;
import dev.roastapp.share.service.ShareService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicShareController.class)
@Import(GlobalExceptionHandler.class)
class PublicShareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShareService shareService;

    @Test
    void getSharePage_returnsSsrHtmlWithOpenGraphTags() throws Exception {
        ShareEntity share = new ShareEntity(
                "abc123xyz890",
                UUID.randomUUID(),
                "http://localhost:8080/api/v1/public/shares/abc123xyz890/card.png",
                "You listen to sad music past midnight.",
                "BRUTAL",
                "spotify"
        );
        when(shareService.getShare("abc123xyz890")).thenReturn(share);

        mockMvc.perform(get("/s/abc123xyz890"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(header().string("Cache-Control", "public, max-age=3600"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("og:image")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("abc123xyz890/card.png")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BRUTAL Roast | Roast App")));
    }

    @Test
    void getShareDetails_returnsJson() throws Exception {
        ShareEntity share = new ShareEntity(
                "abc123xyz890",
                UUID.randomUUID(),
                "http://localhost:8080/api/v1/public/shares/abc123xyz890/card.png",
                "Roast body here",
                "SARCASTIC",
                "github"
        );
        when(shareService.getShare("abc123xyz890")).thenReturn(share);

        mockMvc.perform(get("/api/v1/public/shares/abc123xyz890"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("abc123xyz890"))
                .andExpect(jsonPath("$.tone").value("SARCASTIC"));
    }

    @Test
    void getCardImage_servesPngBytes() throws Exception {
        byte[] fakePng = new byte[]{ (byte) 0x89, 'P', 'N', 'G' };
        when(shareService.getCardImage("abc123xyz890")).thenReturn(fakePng);

        mockMvc.perform(get("/api/v1/public/shares/abc123xyz890/card.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("Cache-Control", "public, max-age=31536000, immutable"));
    }

    @Test
    void getSharePage_whenRevokedOrNotFound_returns404() throws Exception {
        when(shareService.getShare("missing")).thenThrow(new ResourceNotFoundException("Share", "missing"));

        mockMvc.perform(get("/s/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void revokeShare_returnsNoContent() throws Exception {
        doNothing().when(shareService).revokeShare("abc123xyz890");

        mockMvc.perform(delete("/api/v1/public/shares/abc123xyz890"))
                .andExpect(status().isNoContent());

        verify(shareService).revokeShare("abc123xyz890");
    }
}
