package app.simplereader.service;

import dev.matrixlab.webp4j.WebPCodec;
import javafx.scene.image.Image;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class WebPDirectDecoder {

    /**
     * Comprueba si webp4j y sus librerías nativas están disponibles en el sistema.
     */
    public static boolean isAvailable() {
        try {
            return WebPCodec.isAvailable();
        } catch (Throwable e) {
            Logger.error("WebP4j no disponible nativamente: " + e.getMessage());
            return false;
        }
    }

    /**
     * Decodifica bytes de un archivo WebP a una imagen de JavaFX utilizando
     * Direct ByteBuffer y PixelBuffer para minimizar allocaciones en Heap (Zero-Copy pipeline).
     */
    public static Image decode(byte[] webpData, int maxHeight) {
        if (webpData == null || webpData.length == 0) return null;

        try {
            BufferedImage bimg = WebPCodec.decodeImage(webpData);
            if (bimg == null) return null;
            return toDirectImage(bimg, maxHeight);
        } catch (Exception e) {
            Logger.error("Error decodificando WebP con webp4j: " + e.getMessage());
            return null;
        }
    }

    /**
     * Convierte cualquier BufferedImage a una Image de JavaFX utilizando
     * Direct ByteBuffer y PixelBuffer para minimizar memoria en el Heap (Off-Heap).
     */
    public static Image toDirectImage(BufferedImage bimg, int maxHeight) {
        if (bimg == null) return null;

        try {
            int width = bimg.getWidth();
            int height = bimg.getHeight();
            int targetWidth = width;
            int targetHeight = height;

            if (maxHeight > 0 && height > maxHeight) {
                targetHeight = maxHeight;
                targetWidth = (int) (width * ((double) maxHeight / height));

                BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g2d = scaled.createGraphics();
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(bimg, 0, 0, targetWidth, targetHeight, null);
                g2d.dispose();
                bimg.flush();
                bimg = scaled;
            }

            int bufferSize = targetWidth * targetHeight * 4;
            ByteBuffer directBuffer = ByteBuffer.allocateDirect(bufferSize);

            int[] pixels = new int[targetWidth * targetHeight];
            bimg.getRGB(0, 0, targetWidth, targetHeight, pixels, 0, targetWidth);
            bimg.flush();

            for (int pixel : pixels) {
                byte a = (byte) ((pixel >> 24) & 0xFF);
                byte r = (byte) ((pixel >> 16) & 0xFF);
                byte g = (byte) ((pixel >> 8) & 0xFF);
                byte b = (byte) (pixel & 0xFF);

                directBuffer.put(b);
                directBuffer.put(g);
                directBuffer.put(r);
                directBuffer.put(a);
            }
            directBuffer.rewind();

            PixelFormat<ByteBuffer> pixelFormat = PixelFormat.getByteBgraPreInstance();
            PixelBuffer<ByteBuffer> pixelBuffer = new PixelBuffer<>(targetWidth, targetHeight, directBuffer, pixelFormat);

            return new WritableImage(pixelBuffer);
        } catch (Exception e) {
            Logger.error("Error convirtiendo BufferedImage a PixelBuffer: " + e.getMessage());
            return null;
        }
    }

    public static Image decode(byte[] webpData) {
        return decode(webpData, 0);
    }

    public static Image decode(File file, int maxHeight) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        return decode(bytes, maxHeight);
    }
}
