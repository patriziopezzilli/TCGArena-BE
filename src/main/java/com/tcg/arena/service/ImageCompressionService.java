package com.tcg.arena.service;

import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ImageCompressionService {

    private static final int MAX_WIDTH = 800;
    private static final int MAX_HEIGHT = 800;

    public byte[] compressImage(byte[] imageData, String format) throws IOException {
        // Read the image
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
        BufferedImage originalImage = ImageIO.read(inputStream);

        if (originalImage == null) {
            throw new IOException("Invalid image format");
        }

        // Calculate new dimensions maintaining aspect ratio
        int[] newDimensions = calculateNewDimensions(originalImage.getWidth(), originalImage.getHeight());
        int newWidth = newDimensions[0];
        int newHeight = newDimensions[1];

        // Create resized image
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resizedImage.createGraphics();

        // Set rendering hints for better quality
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        graphics.dispose();

        // Compress and return as byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, format.toLowerCase(), outputStream);

        return outputStream.toByteArray();
    }

    private int[] calculateNewDimensions(int originalWidth, int originalHeight) {
        int newWidth = originalWidth;
        int newHeight = originalHeight;

        // Scale down if larger than max dimensions
        if (originalWidth > MAX_WIDTH || originalHeight > MAX_HEIGHT) {
            double widthRatio = (double) MAX_WIDTH / originalWidth;
            double heightRatio = (double) MAX_HEIGHT / originalHeight;
            double scaleFactor = Math.min(widthRatio, heightRatio);

            newWidth = (int) (originalWidth * scaleFactor);
            newHeight = (int) (originalHeight * scaleFactor);
        }

        return new int[]{newWidth, newHeight};
    }

    public String getImageFormat(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "JPEG";
            case "png":
                return "PNG";
            case "gif":
                return "GIF";
            default:
                return "JPEG"; // Default to JPEG
        }
    }
}