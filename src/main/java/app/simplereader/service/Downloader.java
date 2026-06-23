package app.simplereader.service;

import app.simplereader.controller.SourceManager;
import app.simplereader.model.AppConfig;
import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Downloader {

    private static final Downloader INSTANCE = new Downloader();
    private static final DownloadTask POISON = new DownloadTask(null, null, null);

    private final BlockingQueue<DownloadTask> queue = new LinkedBlockingQueue<>();
    private final HashSet<String> downloading = new HashSet<>();
    private final ExecutorService workers;

    private final String DOWNLOADS_FOLDER = AppConfig.DATA_FOLDER;
    private final SourceManager sourceManager = SourceManager.getInstance();

    // Observable list of active downloads for the UI
    private final ObservableList<DownloadInfo> activeDownloads = FXCollections.observableArrayList();

    /**
     * Represents a single active download visible to the UI.
     */
    public static class DownloadInfo {
        private final String chapterTitle;
        private final String mangaTitle;
        private final String chapterID;
        private final DoubleProperty progress = new SimpleDoubleProperty(0.0);
        private final StringProperty status = new SimpleStringProperty("En cola");

        public DownloadInfo(String chapterTitle, String mangaTitle, String chapterID) {
            this.chapterTitle = chapterTitle;
            this.mangaTitle = mangaTitle;
            this.chapterID = chapterID;
        }

        public String getChapterTitle() { return chapterTitle; }
        public String getMangaTitle() { return mangaTitle; }
        public String getChapterID() { return chapterID; }

        public double getProgress() { return progress.get(); }
        public DoubleProperty progressProperty() { return progress; }

        public String getStatus() { return status.get(); }
        public StringProperty statusProperty() { return status; }
    }

    private static class DownloadTask {
        final Chapter chapter;
        final Manga manga;
        final Map<String, String> headers;
        DownloadInfo info;

        DownloadTask(Chapter chapter, Manga manga, Map<String, String> headers) {
            this.chapter = chapter;
            this.manga = manga;
            this.headers = headers;
        }
    }

    private volatile boolean cancelled = false;

    private Downloader() {
        int threads = 2;
        workers = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "downloader");
            t.setDaemon(true);
            return t;
        });
        for (int i = 0; i < threads; i++) {
            workers.submit(this::workerLoop);
        }
    }

    public static Downloader getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the observable list of active downloads. Bind a ListView to this.
     */
    public ObservableList<DownloadInfo> getActiveDownloads() {
        return activeDownloads;
    }

    public void enqueue(Chapter chapter, Manga manga) {
        enqueue(chapter, manga, null);
    }

    public void enqueue(Chapter chapter, Manga manga, Map<String, String> headers) {
        if (chapter == null || manga == null || manga.getSourceID().equals("local")) return;

        synchronized (downloading) {
            if (downloading.contains(chapter.getChapterID())) {
                Logger.error("Ya estas descargando ese capitulo.");
                return;
            }
            downloading.add(chapter.getChapterID());
        }

        DownloadInfo info = new DownloadInfo(
            chapter.getTitle() != null ? chapter.getTitle() : chapter.getChapterID(),
            manga.getTitle() != null ? manga.getTitle() : manga.getMangaID(),
            chapter.getChapterID()
        );

        DownloadTask task = new DownloadTask(chapter, manga, headers);
        task.info = info;

        Platform.runLater(() -> activeDownloads.add(info));

        queue.add(task);
        Logger.info("Encolado: " + chapter.getTitle());
    }

    public void cancelAll() {
        cancelled = true;
        queue.clear();

        synchronized (downloading) {
            downloading.clear();
        }

        Platform.runLater(() -> activeDownloads.clear());

        // Restart workers
        cancelled = false;
        for (int i = 0; i < 2; i++) {
            workers.submit(this::workerLoop);
        }

        Logger.info("Todas las descargas han sido canceladas.");
    }

    private void workerLoop() {
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        while (!cancelled) {
            DownloadTask task;
            try {
                task = queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            if (task == POISON) {
                queue.add(POISON);
                break;
            }

            if (cancelled) break;

            Platform.runLater(() -> task.info.status.set("Descargando..."));
            downloadChapter(task, httpClient);

            synchronized (downloading) {
                downloading.remove(task.chapter.getChapterID());
            }

            // Remove from active downloads when done
            Platform.runLater(() -> activeDownloads.remove(task.info));
        }
    }

    private void downloadChapter(DownloadTask task, HttpClient httpClient) {
        Chapter chapter = task.chapter;
        Manga manga = task.manga;
        DownloadInfo info = task.info;

        try {
            File folder = new File(DOWNLOADS_FOLDER + manga.getSourceID() + "/downloads/"
                    + manga.getMangaID() + "/" + chapter.getChapterID());
            if (!folder.exists()) folder.mkdirs();

            List<String> pagesURL = sourceManager.getSource(manga.getSourceID())
                    .getPages(manga.getMangaID(), chapter.getChapterID());

            int totalPages = pagesURL.size();

            File[] existingFiles = folder.listFiles();
            if (existingFiles != null && existingFiles.length >= totalPages) {
                Logger.info("El capítulo ya está descargado por completo.");
                Platform.runLater(() -> {
                    chapter.setDownloaded(true);
                    info.progress.set(1.0);
                    info.status.set("Completado");
                });
                return;
            }

            int pageNumber = 1;
            for (String urlString : pagesURL) {
                if (cancelled) {
                    Platform.runLater(() -> info.status.set("Cancelado"));
                    return;
                }

                String extension = ".jpg";
                int lastDot = urlString.lastIndexOf('.');
                if (lastDot > 0 && lastDot < urlString.length() - 1) {
                    String potentialExt = urlString.substring(lastDot);
                    if (potentialExt.contains("?")) potentialExt = potentialExt.split("\\?")[0];
                    if (potentialExt.length() <= 5) extension = potentialExt;
                }

                String fileName = String.format("%03d%s", pageNumber, extension);
                File outputFile = new File(folder, fileName);

                if (outputFile.exists() && outputFile.length() > 0) {
                    Logger.info("Página ya existe, omitiendo: " + fileName);
                    final int currentPage = pageNumber;
                    Platform.runLater(() -> {
                        info.progress.set((double) currentPage / totalPages);
                        info.status.set("Página " + currentPage + "/" + totalPages);
                    });
                    pageNumber++;
                    continue;
                }

                boolean downloaded = false;
                for (int attempt = 1; attempt <= 3 && !downloaded; attempt++) {
                    if (cancelled) return;
                    try {
                        if (attempt > 1) {
                            pagesURL = sourceManager.getSource(manga.getSourceID())
                                    .getPages(manga.getMangaID(), chapter.getChapterID());
                            urlString = pagesURL.get(pageNumber - 1);
                            fileName = String.format("%03d%s", pageNumber, extension);
                            outputFile = new File(folder, fileName);
                        }

                        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                                .uri(URI.create(urlString))
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                                .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                                .timeout(Duration.ofSeconds(15))
                                .GET();

                        if (task.headers != null) {
                            task.headers.forEach(reqBuilder::header);
                        } else {
                            reqBuilder.header("Referer", "https://mangadex.org/");
                        }

                        HttpResponse<InputStream> response = httpClient.send(
                                reqBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());

                        if (response.statusCode() == 200) {
                            try (InputStream in = response.body()) {
                                Files.copy(in, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                Logger.info("Downloaded: " + fileName);
                                downloaded = true;
                            }
                        } else {
                            throw new IOException("HTTP " + response.statusCode());
                        }
                    } catch (IOException | InterruptedException e) {
                        Logger.error("Error (intento " + attempt + "/3) página " + pageNumber + ": " + urlString);
                        if (attempt < 3) {
                            try { Thread.sleep(1000); } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        } else {
                            e.printStackTrace();
                        }
                    }
                }

                if (!downloaded) {
                    Platform.runLater(() -> info.status.set("Error"));
                    break;
                }

                final int currentPage = pageNumber;
                Platform.runLater(() -> {
                    info.progress.set((double) currentPage / totalPages);
                    info.status.set("Página " + currentPage + "/" + totalPages);
                });

                pageNumber++;
            }

            File[] downloadedFiles = folder.listFiles();
            if (downloadedFiles != null && downloadedFiles.length >= pagesURL.size()) {
                Platform.runLater(() -> {
                    chapter.setDownloaded(true);
                    info.progress.set(1.0);
                    info.status.set("Completado");
                });
                Logger.info("Proceso de descarga finalizado para " + chapter.getTitle() + ".");
            }
        } catch (Exception e) {
            Logger.error("Error descargando " + chapter.getTitle() + ": " + e.getMessage());
            Platform.runLater(() -> info.status.set("Error"));
        }
    }
}
