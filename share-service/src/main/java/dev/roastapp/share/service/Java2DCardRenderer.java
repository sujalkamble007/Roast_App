package dev.roastapp.share.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class Java2DCardRenderer implements CardRenderer {

    private static final Logger log = LoggerFactory.getLogger(Java2DCardRenderer.class);
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 630;

    @Override
    public byte[] renderCard(String roastText, String tone, String sources) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        try {
            // High-quality rendering hints
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            // 1. Sleek dark gradient background
            GradientPaint bgGradient = new GradientPaint(
                    0, 0, new Color(15, 23, 42), // slate-900
                    WIDTH, HEIGHT, new Color(2, 6, 23) // slate-950
            );
            g2d.setPaint(bgGradient);
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            // 2. Subtle decorative glow/accent shapes
            g2d.setPaint(new GradientPaint(
                    0, 0, new Color(249, 115, 22, 60), // fire orange
                    400, 400, new Color(139, 92, 246, 0) // purple transparent
            ));
            g2d.fillOval(-100, -100, 500, 500);

            g2d.setPaint(new GradientPaint(
                    WIDTH, HEIGHT, new Color(236, 72, 153, 50), // pink
                    WIDTH - 400, HEIGHT - 400, new Color(2, 6, 23, 0)
            ));
            g2d.fillOval(WIDTH - 400, HEIGHT - 400, 500, 500);

            // 3. Inner Card Container with rounded corners and glassmorphism border
            int margin = 50;
            int cardW = WIDTH - (margin * 2);
            int cardH = HEIGHT - (margin * 2);

            g2d.setColor(new Color(30, 41, 59, 180)); // slate-800 translucent
            g2d.fill(new RoundRectangle2D.Double(margin, margin, cardW, cardH, 32, 32));

            // Accent border
            g2d.setColor(new Color(249, 115, 22, 120));
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.draw(new RoundRectangle2D.Double(margin, margin, cardW, cardH, 32, 32));

            // 4. Header: Badge with Tone
            String toneDisplay = (tone != null ? tone.toUpperCase() : "SAVAGE") + " ROAST";
            int badgeX = margin + 40;
            int badgeY = margin + 45;

            g2d.setColor(new Color(249, 115, 22, 40));
            g2d.fillRoundRect(badgeX, badgeY, 220, 38, 19, 19);
            g2d.setColor(new Color(251, 146, 60)); // orange-400
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRoundRect(badgeX, badgeY, 220, 38, 19, 19);

            g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            g2d.setColor(new Color(254, 215, 170));
            g2d.drawString("🔥 " + toneDisplay, badgeX + 20, badgeY + 24);

            // 5. Quote Mark
            g2d.setFont(new Font(Font.SERIF, Font.BOLD, 80));
            g2d.setColor(new Color(148, 163, 184, 80));
            g2d.drawString("“", margin + 40, margin + 140);

            // 6. Roast Text with Word Wrapping
            g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
            g2d.setColor(new Color(241, 245, 249)); // slate-100
            FontMetrics fm = g2d.getFontMetrics();

            int textX = margin + 60;
            int textY = margin + 165;
            int maxTextWidth = cardW - 120;
            int lineHeight = 46;
            int maxLines = 6;

            List<String> wrappedLines = wrapText(roastText != null ? roastText : "Certified roasted.", fm, maxTextWidth);
            for (int i = 0; i < Math.min(wrappedLines.size(), maxLines); i++) {
                String line = wrappedLines.get(i);
                if (i == maxLines - 1 && wrappedLines.size() > maxLines) {
                    line = line + "...";
                }
                g2d.drawString(line, textX, textY + (i * lineHeight));
            }

            // 7. Footer: Sources & Branding
            int footerY = margin + cardH - 35;

            // Sources on bottom-left
            g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
            g2d.setColor(new Color(148, 163, 184)); // slate-400
            String sourcesLabel = "Analyzed from: " + (sources != null && !sources.isBlank() ? sources.replace(",", " • ") : "digital footprints");
            g2d.drawString(sourcesLabel, margin + 40, footerY);

            // Watermark on bottom-right
            g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            g2d.setColor(new Color(249, 115, 22)); // orange-500
            String brand = "roastapp.dev";
            int brandW = g2d.getFontMetrics().stringWidth(brand);
            g2d.drawString(brand, margin + cardW - brandW - 40, footerY);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to render card image: {}", e.getMessage(), e);
            throw new RuntimeException("Card rendering failed", e);
        } finally {
            g2d.dispose();
        }
    }

    private List<String> wrapText(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (fm.stringWidth(testLine) <= maxWidth) {
                currentLine = new StringBuilder(testLine);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }
}
