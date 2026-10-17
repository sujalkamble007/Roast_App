package dev.roastapp.share.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.roastapp.share.service.ShareService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalShareController.class)
class InternalShareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShareService shareService;

    @Test
    void createShare_returnsCreatedWithShareUrlAndImageUrl() throws Exception {
        UUID roastId = UUID.randomUUID();
        ShareDto.CreateShareRequest request = new ShareDto.CreateShareRequest(
                roastId, "You code past midnight.", "BRUTAL", "github,spotify"
        );

        when(shareService.createShare(roastId, "You code past midnight.", "BRUTAL", "github,spotify"))
                .thenReturn(new ShareService.ShareResult(
                        "abc123xyz890",
                        "/s/abc123xyz890",
                        "http://localhost:8080/api/v1/public/shares/abc123xyz890/card.png"
                ));

        mockMvc.perform(post("/internal/v1/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("abc123xyz890"))
                .andExpect(jsonPath("$.shareUrl").value("/s/abc123xyz890"))
                .andExpect(jsonPath("$.imageUrl").value("http://localhost:8080/api/v1/public/shares/abc123xyz890/card.png"));
    }
}
