package dev.roastapp.share.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class CardRendererTest {

    private final CardRenderer renderer = new Java2DCardRenderer();

    @Test
    void renderCard_createsValid1200x630Png() throws IOException {
        String roast = "You have 37 abandoned git repositories and 78% of your Spotify listening happens past midnight. Truly tragic.";
        byte[] pngBytes = renderer.renderCard(roast, "BRUTAL", "github,spotify");

        assertThat(pngBytes).isNotEmpty();

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes));
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(1200);
        assertThat(image.getHeight()).isEqualTo(630);
    }
}
