package org.oogle.calender.icon;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class IconGenerator {

    public static void main(String[] args) {
        try {
            // Create calendar icon at multiple sizes for .ico
            int[] sizes = {16, 32, 48, 64, 128, 256};

            for (int size : sizes) {
                BufferedImage icon = createCalendarIcon(size);
                String filename = "calendar-icon-" + size + ".png";
                ImageIO.write(icon, "PNG", new File(filename));
                System.out.println("Created: " + filename);
            }

            System.out.println("\nNow visit: https://convertio.co/png-ico/");
            System.out.println("Upload calendar-icon-256.png and convert it to .ico");
            System.out.println("Save as: calendar-icon.ico in your project folder");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static BufferedImage createCalendarIcon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Calculate proportions
        int padding = size / 8;
        int width = size - (padding * 2);
        int height = (int)(width * 0.85);
        int x = padding;
        int y = padding + size / 16;
        int cornerRadius = size / 8;

        // Draw calendar background (blue)
        g.setColor(new Color(74, 144, 226));
        g.fillRoundRect(x, y, width, height, cornerRadius, cornerRadius);

        // Draw calendar header (darker blue)
        int headerHeight = height / 4;
        g.setColor(new Color(50, 100, 180));
        g.fillRoundRect(x, y, width, headerHeight, cornerRadius, cornerRadius);
        g.fillRect(x, y + headerHeight / 2, width, headerHeight / 2);

        // Draw binding rings (white)
        int ringSize = size / 8;
        int ringY = y - ringSize / 2;
        g.setColor(Color.WHITE);
        g.fillOval(x + width / 4 - ringSize / 2, ringY, ringSize, ringSize);
        g.fillOval(x + 3 * width / 4 - ringSize / 2, ringY, ringSize, ringSize);

        // Draw grid lines (subtle white)
        g.setColor(new Color(255, 255, 255, 80));
        int bodyY = y + headerHeight;
        int bodyHeight = height - headerHeight;
        int gridSpacing = bodyHeight / 4;

        for (int i = 1; i < 4; i++) {
            g.drawLine(x + padding, bodyY + i * gridSpacing,
                    x + width - padding, bodyY + i * gridSpacing);
        }

        for (int i = 1; i < 3; i++) {
            g.drawLine(x + i * width / 3, bodyY + padding,
                    x + i * width / 3, y + height - padding);
        }

        // Draw current day highlight (red dot)
        int dotSize = size / 8;
        g.setColor(new Color(255, 107, 107));
        g.fillOval(x + width - dotSize - padding * 2,
                y + height - dotSize - padding * 2,
                dotSize, dotSize);

        g.dispose();
        return image;
    }
}
