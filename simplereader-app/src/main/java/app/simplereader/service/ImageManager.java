package app.simplereader.service;

import javafx.scene.image.Image;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;
import app.simplereader.model.AppConfig;
import app.simplereader.repository.MangaSource;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class ImageManager {
    private static ImageManager instance;
    private Image appIcon;
    private Image loadingGif;
    private Image errorGif;
    
    private ImageManager() {
        try {
            appIcon = new Image(getClass().getResourceAsStream("/icons/app_icon.png"));
            loadingGif = new Image(getClass().getResource("/icons/koruko.gif").toExternalForm());
            errorGif = new Image(getClass().getResource("/icons/bochi.gif").toExternalForm());
        } catch (Exception e) {
            Logger.error("Failed to load base UI images: " + e.getMessage());
        }
    }
    
    public static ImageManager getInstance() {
        if (instance == null) instance = new ImageManager();
        return instance;
    }
    
    public Image getAppIcon() { return appIcon; }
    public Image getLoadingGif() { return loadingGif; }
    public Image getErrorGif() { return errorGif; }
    
    public void loadCoverAsync(String url, boolean highQuality, Consumer<Image> callback) {
        if (url == null) return;
        Logger.info("Loading cover image: " + url);
        
        com.github.benmanes.caffeine.cache.Cache<String, Image> targetCache = highQuality ? 
            Cache.getInstance().getHdCoverCache() : Cache.getInstance().getSharedCache();
            
        Image cached = targetCache.getIfPresent(url);
        if (cached != null) {
            callback.accept(cached);
            return;
        }
        
        int LOAD_SIZE = highQuality ? 0 : 200;
        
        if (url.toLowerCase().contains(".webp")) {
            loadCoverWithImageIO(url, targetCache, LOAD_SIZE, callback);
        } else {
            Image nativeLoad = (LOAD_SIZE > 0) ? 
                new Image(url, LOAD_SIZE, LOAD_SIZE, true, true, true) : 
                new Image(url, true);
            if (nativeLoad.isError()) {
                loadCoverWithImageIO(url, targetCache, LOAD_SIZE, callback);
            } else if (nativeLoad.getProgress() >= 1.0) {
                Platform.runLater(() -> {
                    targetCache.put(url, nativeLoad);
                    callback.accept(nativeLoad);
                });
            } else {
                nativeLoad.progressProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> obs, Number old, Number progress) {
                        if (progress.doubleValue() >= 1.0) {
                            nativeLoad.progressProperty().removeListener(this);
                            if (!nativeLoad.isError()) {
                                Platform.runLater(() -> {
                                    targetCache.put(url, nativeLoad);
                                    callback.accept(nativeLoad);
                                });
                            }
                        }
                    }
                });
                nativeLoad.errorProperty().addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> obs, Boolean old, Boolean isError) {
                        if (isError) {
                            nativeLoad.errorProperty().removeListener(this);
                            loadCoverWithImageIO(url, targetCache, LOAD_SIZE, callback);
                        }
                    }
                });
            }
        }
    }
    
    private void loadCoverWithImageIO(String url, com.github.benmanes.caffeine.cache.Cache<String, Image> targetCache, int loadSize, Consumer<Image> callback) {
        CompletableFuture.supplyAsync(() -> {
            try {
                java.awt.image.BufferedImage bimg = null;
                InputStream in1 = null;
                try {
                    in1 = openCoverStream(url);
                    javax.imageio.stream.ImageInputStream iis = ImageIO.createImageInputStream(in1);
                    if (iis != null) {
                        java.util.Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReaders(iis);
                        if (readers.hasNext()) {
                            javax.imageio.ImageReader reader = readers.next();
                            reader.setInput(iis, true, true);
                            int w = reader.getWidth(0);
                            int h = reader.getHeight(0);
                            javax.imageio.ImageReadParam param = reader.getDefaultReadParam();
                            
                            if (loadSize > 0 && (w > loadSize || h > loadSize)) {
                                int scale = Math.max(w / loadSize, h / loadSize);
                                if (scale < 1) scale = 1;
                                param.setSourceSubsampling(scale, scale, 0, 0);
                            }
                            bimg = reader.read(0, param);
                            reader.dispose();
                        }
                        iis.close();
                    }
                } finally {
                    if (in1 != null) in1.close();
                }
                
                if (bimg == null) {
                    InputStream in2 = null;
                    try {
                        in2 = openCoverStream(url);
                        bimg = ImageIO.read(in2);
                    } finally {
                        if (in2 != null) in2.close();
                    }
                }
                
                if (bimg != null) {
                    if (loadSize > 0 && (bimg.getWidth() > loadSize || bimg.getHeight() > loadSize)) {
                        int newWidth = bimg.getWidth();
                        int newHeight = bimg.getHeight();
                        if (newWidth > newHeight) {
                            newHeight = (int) (newHeight * ((double) loadSize / newWidth));
                            newWidth = loadSize;
                        } else {
                            newWidth = (int) (newWidth * ((double) loadSize / newHeight));
                            newHeight = loadSize;
                        }
                        
                        java.awt.image.BufferedImage scaledBimg = new java.awt.image.BufferedImage(newWidth, newHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                        java.awt.Graphics2D g2d = scaledBimg.createGraphics();
                        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2d.drawImage(bimg, 0, 0, newWidth, newHeight, null);
                        g2d.dispose();
                        bimg.flush();
                        bimg = scaledBimg;
                    }
                    Image fxImg = SwingFXUtils.toFXImage(bimg, null);
                    bimg.flush();
                    return fxImg;
                }
            } catch (Exception e) {
                Logger.error("Error decoding cover with ImageIO: " + e.getMessage());
            }
            return null;
        }).thenAccept(img -> {
            if (img != null) {
                Platform.runLater(() -> {
                    targetCache.put(url, img);
                    callback.accept(img);
                });
            }
        });
    }

    private InputStream openCoverStream(String url) throws Exception {
        if (url.startsWith("file:")) {
            return new java.net.URL(url).openStream();
        } else if (url.startsWith("http://") || url.startsWith("https://")) {
            okhttp3.Response response = app.simplereader.service.Http.getOkHttpResponse(url, null);
            if (response.body() != null) {
                return response.body().byteStream();
            }
            throw new IOException("Empty response body from: " + url);
        } else {
            return new java.io.FileInputStream(new File(url));
        }
    }

    public Image loadPage(String url, MangaSource source, int index, Consumer<Double> progressCallback) {
        Logger.info("Loading page [" + index + "] image: " + url);
        if (url.startsWith("http")) {
            try (okhttp3.Response response = app.simplereader.service.Http.getOkHttpResponse(url, source)) {
                if (!response.isSuccessful()) {
                    throw new java.io.IOException("HTTP " + response.code());
                }
                
                long totalBytes = response.body().contentLength();
                try (InputStream in = response.body().byteStream();
                     java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream()) {
                     
                    byte[] chunk = new byte[8192];
                    long bytesRead = 0;
                    int n;
                    long lastUpdate = 0;
                    
                    while ((n = in.read(chunk)) != -1) {
                        buffer.write(chunk, 0, n);
                        bytesRead += n;
                        
                        if (totalBytes > 0 && progressCallback != null) {
                            long now = System.currentTimeMillis();
                            if (now - lastUpdate > 50) {
                                lastUpdate = now;
                                double progress = (double) bytesRead / totalBytes;
                                Platform.runLater(() -> progressCallback.accept(progress));
                            }
                        }
                    }
                    
                    if (totalBytes > 0 && progressCallback != null) {
                        Platform.runLater(() -> progressCallback.accept(1.0));
                    }
                    
                    try (java.io.ByteArrayInputStream bin = new java.io.ByteArrayInputStream(buffer.toByteArray())) {
                        if (url.toLowerCase().contains(".webp")) {
                            if (WebPDirectDecoder.isAvailable()) {
                                int maxH = AppConfig.get().limitPageQuality ? 1080 : 0;
                                Image directImg = WebPDirectDecoder.decode(buffer.toByteArray(), maxH);
                                if (directImg != null) return directImg;
                            }
                            
                            BufferedImage bimg = ImageIO.read(bin);
                            if (bimg != null) {
                                if (AppConfig.get().limitPageQuality && bimg.getHeight() > 1080) {
                                    int newHeight = 1080;
                                    int newWidth = (int) (bimg.getWidth() * (1080.0 / bimg.getHeight()));
                                    java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(newWidth, newHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                                    java.awt.Graphics2D g2d = scaled.createGraphics();
                                    g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                                    g2d.drawImage(bimg, 0, 0, newWidth, newHeight, null);
                                    g2d.dispose();
                                    bimg.flush();
                                    bimg = scaled;
                                }
                                Image fxImg = SwingFXUtils.toFXImage(bimg, null);
                                bimg.flush();
                                return fxImg;
                            }
                        } else {
                            if (AppConfig.get().limitPageQuality) {
                                return new Image(bin, 0, 1080, true, true);
                            }
                            return new Image(bin);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Error loading page: " + e.getMessage(), e);
            }
            return null;
        } else {
            if (url.toLowerCase().contains(".webp")) {
                try {
                    if (WebPDirectDecoder.isAvailable()) {
                        byte[] bytes;
                        if (url.startsWith("file:")) {
                            try (InputStream fileIn = new java.net.URL(url).openStream()) {
                                bytes = fileIn.readAllBytes();
                            }
                        } else {
                            bytes = java.nio.file.Files.readAllBytes(new File(url).toPath());
                        }
                        int maxH = AppConfig.get().limitPageQuality ? 1080 : 0;
                        Image directImg = WebPDirectDecoder.decode(bytes, maxH);
                        if (directImg != null) return directImg;
                    }

                    BufferedImage bimg;
                    if (url.startsWith("file:")) {
                        bimg = ImageIO.read(new java.net.URL(url));
                    } else {
                        bimg = ImageIO.read(new File(url));
                    }
                    
                    if (bimg != null) {
                        if (AppConfig.get().limitPageQuality && bimg.getHeight() > 1080) {
                            int newHeight = 1080;
                            int newWidth = (int) (bimg.getWidth() * (1080.0 / bimg.getHeight()));
                            java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(newWidth, newHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                            java.awt.Graphics2D g2d = scaled.createGraphics();
                            g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                            g2d.drawImage(bimg, 0, 0, newWidth, newHeight, null);
                            g2d.dispose();
                            bimg.flush();
                            bimg = scaled;
                        }
                        Image fxImg = SwingFXUtils.toFXImage(bimg, null);
                        bimg.flush();
                        return fxImg;
                    }
                } catch (Exception e) {
                    Logger.error("Error decoding local webp. Using native fallback.");
                }
                return new Image(url, false);
            } else {
                if (AppConfig.get().limitPageQuality) {
                    return new Image(url, 0, 720, true, true, false);
                }
                return new Image(url, false);
            }
        }
    }
    
    public Image loadPagev2(String url){
        Image page = new Image(url, true);
        return page;
    }
}
