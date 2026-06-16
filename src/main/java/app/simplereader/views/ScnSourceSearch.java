package app.simplereader.views;
import app.simplereader.views.components.SideMenu;
import app.simplereader.controller.LibraryController;
import app.simplereader.service.Logger;
import app.simplereader.controller.SceneController;
import app.simplereader.controller.SourceManager;
import app.simplereader.model.AppConfig;
import app.simplereader.model.Category;
import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import app.simplereader.repository.MangaSource;
import app.simplereader.repository.AppScene;
import app.simplereader.views.components.Buttons;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.util.HashMap;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.util.List;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
public class ScnSourceSearch implements AppScene {
    private final SceneController nav = SceneController.getInstance();
    private final MangaSource source;
    
    private TextField searchField;
    private ListView<Manga> resultsList;

    private final ObservableList<Manga> results = FXCollections.observableArrayList();
    public ScnSourceSearch(MangaSource source) {
        this.source = source;
    }
    @Override
    public javafx.scene.Parent getScene() {
        SideMenu lateralmenu = new SideMenu();
        Button btnBack = Buttons.getBackButton();
        
        lateralmenu.addTop(btnBack);
        btnBack.setOnAction(e-> nav.backScene());
        searchField = new TextField();
        searchField.setPromptText("Buscar en " + source.getName() + "...");
        searchField.setMaxSize(800, 30);
        searchField.setMinSize(800, 30);
        searchField.getStyleClass().add("search-bar");
        searchField.setOnAction(e -> doSearch());
        double scale = 24.0 / 960.0;
        
        SVGPath icnSearch = new SVGPath();
        icnSearch.setContent("M784-120 532-372q-30 24-69 38t-83 14q-109 0-184.5-75.5T120-580q0-109 75.5-184.5T380-840q109 0 184.5 75.5T640-580q0 44-14 83t-38 69l252 252-56 56ZM380-400q75 0 127.5-52.5T560-580q0-75-52.5-127.5T380-760q-75 0-127.5 52.5T200-580q0 75 52.5 127.5T380-400Z");
        icnSearch.getStyleClass().add("icon");
        icnSearch.setScaleX(scale);
        icnSearch.setScaleY(scale);
        
        Group icnSearch_group = new Group(icnSearch);
        StackPane icnSearch_pane = new StackPane(icnSearch_group);
        
        Button btnSearch = new Button("",icnSearch_pane);
        btnSearch.setOnAction(e -> doSearch());
        btnSearch.setMinSize(30, 30);
        btnSearch.setMaxSize(30, 30);

        HBox searchBar = new HBox(5, searchField, btnSearch);
        searchBar.setPadding(new Insets(10));
        searchBar.setAlignment(Pos.CENTER);
        
        Label title = new Label(source.getName());
        title.getStyleClass().add("search-title");
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);
        
        VBox top = new VBox(10,title,searchBar);
        
        resultsList = new ListView<>(results);
        resultsList.getStyleClass().add("search-list");
        // Configurar cómo se ve cada item
        resultsList.setCellFactory(lv -> new ListCell<>() {
            private final ImageView coverView = new ImageView();
            private final StackPane coverContainer = new StackPane(coverView);
            private final Label titleLabel = new Label();
            private final HBox box = new HBox(15, coverContainer, titleLabel);
            private String currentUrl = null;
            private final Runnable updateImageFit;

            {
                box.setAlignment(Pos.CENTER_LEFT);
                box.getStyleClass().add("search-list-cell-box");
                
                coverContainer.setMinSize(50, 75);
                coverContainer.setMaxSize(50, 75);
                
                coverView.setPreserveRatio(true);
                
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(50, 75);
                clip.setArcWidth(10);
                clip.setArcHeight(10);
                coverContainer.setClip(clip);
                
                updateImageFit = () -> {
                    Image img = coverView.getImage();
                    if (img == null || img.isError()) return;
                    
                    double imgW = img.getWidth();
                    double imgH = img.getHeight();
                    if (imgW == 0 || imgH == 0) return;
                    
                    double imageRatio = imgW / imgH;
                    double containerRatio = 50.0 / 75.0;
                    
                    if (imageRatio < containerRatio) {
                        coverView.setFitWidth(50);
                        coverView.setFitHeight(0);
                    } else {
                        coverView.setFitHeight(75);
                        coverView.setFitWidth(0);
                    }
                };
                
                titleLabel.getStyleClass().add("search-list-title");
                titleLabel.setWrapText(true);
            }

            @Override
            protected void updateItem(Manga manga, boolean empty) {
                super.updateItem(manga, empty);
                if (empty || manga == null) {
                    setText(null);
                    setGraphic(null);
                    currentUrl = null;
                    coverView.setImage(null);
                } else {
                    setText(null);
                    titleLabel.setText(manga.getTitle());
                    setGraphic(box);
                    
                    String url = manga.getCoverURL();
                    if (url == null || url.equals(currentUrl)) return;
                    currentUrl = url;
                    coverView.setImage(null);
                    
                    Image cached = SEARCH_IMAGE_CACHE.getIfPresent(url);
                    if (cached != null) {
                        coverView.setImage(cached);
                        updateImageFit.run();
                        return;
                    }
                    
                    loadScaledCoverAsync(url, img -> {
                        if (url.equals(currentUrl)) {
                            coverView.setImage(img);
                            updateImageFit.run();
                        }
                    });
                }
            }
        });
        resultsList.setOnMouseClicked(e -> {
            if (e.getButton() != javafx.scene.input.MouseButton.PRIMARY) return;
            if (e.getClickCount() < 2) return;
            Manga selected = resultsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Logger.info("Cargando detalles de: " + selected.getTitle());
                
                LibraryController lib = LibraryController.getInstance();
                Manga mangaToRead = null;
                // 1. Buscar si ya existe en la biblioteca
                for (Category cat : lib.getAllCategories()) {
                    for (Manga m : cat.getMangas().values()) {
                        if (m.getMangaID().equals(selected.getMangaID()) && 
                            m.getSourceID().equals(selected.getSourceID())) {
                            mangaToRead = m; 
                            break;
                        }
                    }
                    if (mangaToRead != null) break;
                }
                // 2. Si no existe, usamos el objeto de la búsqueda
                if (mangaToRead == null) {
                    mangaToRead = selected;
                }
                // 3. Cargar capítulos frescos desde el Source
                List<Chapter> newChapters = source.getChapters(mangaToRead.getMangaID());
                
                // 4. CRUCIAL: Fusionar el progreso de lectura (leído/página) a los nuevos capítulos
                if (mangaToRead.getChapters() != null) {
                    Map<String, Chapter> oldChaptersMap = new HashMap<>();
                    for (Chapter oldCh : mangaToRead.getChapters()) {
                        oldChaptersMap.put(oldCh.getChapterID(), oldCh);
                    }
                    
                    for (Chapter newCh : newChapters) {
                        Chapter oldCh = oldChaptersMap.get(newCh.getChapterID());
                        if (oldCh != null) {
                            if (oldCh.isReaded()) newCh.markAsReaded();
                            newCh.setLastRead(oldCh.getLastRead());
                        }
                        newCh.setManga(mangaToRead); // Enlazar al padre
                    }
                } else {
                    for (Chapter ch : newChapters) {
                        ch.setManga(mangaToRead);
                    }
                }
                
                // 5. Asignar la lista fusionada y cargar metadatos
                mangaToRead.setChapters(newChapters);
                SourceManager.getInstance().fetchMangaData(mangaToRead);
                
                // 6. Navegar con la instancia correcta
                ScnMangaMenu menu = ScnMangaMenu.getInstance();
                menu.updateManga(mangaToRead);
                nav.goTo(menu);
            }
        });
        VBox.setVgrow(resultsList, javafx.scene.layout.Priority.ALWAYS);
        VBox.setMargin(resultsList, new Insets(10, 20, 20, 20));
        BorderPane root = new BorderPane();
        VBox cosas = new VBox(top,resultsList);
        cosas.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
            javafx.scene.Node target = (javafx.scene.Node) e.getTarget();
            javafx.scene.Node p = target;
            boolean inList = false;
            while (p != null) {
                if (p == resultsList) { inList = true; break; }
                p = p.getParent();
            }
            if (!inList) resultsList.getSelectionModel().clearSelection();
        });
        root.setCenter(cosas);
        root.setLeft(lateralmenu.getPane());
        
        root.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                nav.backScene();
                e.consume();
            }
        });
        
        return root;
    }
    
    private void doSearch() {
        String query = searchField.getText();
        if (query.isBlank()) return;
        
        results.clear(); // Limpiar anteriores
        // Buscar y añadir a la lista observable
        List<Manga> found = SourceManager.getInstance().searchManga(source, query);
        results.addAll(found);
    }
    
    @Override
    public String getName() { return "Search - " + source.getName(); }
    @Override
    public String getParentName() { return "Source"; }
    
    private static final Cache<String, Image> SEARCH_IMAGE_CACHE = Caffeine.newBuilder()
        .maximumSize(100)
        .build();

    private void loadScaledCoverAsync(String coverURL, java.util.function.Consumer<Image> callback) {
        if (coverURL != null && coverURL.toLowerCase().contains(".webp")) {
            loadWithImageIO(coverURL, callback);
        } else {
            Image originalImage = new Image(coverURL, 100, 150, true, true, true);
            if (originalImage.isError()) {
                loadWithImageIO(coverURL, callback);
            } else if (originalImage.getProgress() >= 1.0) {
                javafx.application.Platform.runLater(() -> {
                    SEARCH_IMAGE_CACHE.put(coverURL, originalImage);
                    callback.accept(originalImage);
                });
            } else {
                originalImage.progressProperty().addListener(new javafx.beans.value.ChangeListener<Number>() {
                    @Override
                    public void changed(javafx.beans.value.ObservableValue<? extends Number> obs, Number old, Number progress) {
                        if (progress.doubleValue() >= 1.0) {
                            originalImage.progressProperty().removeListener(this);
                            if (!originalImage.isError()) {
                                javafx.application.Platform.runLater(() -> {
                                    SEARCH_IMAGE_CACHE.put(coverURL, originalImage);
                                    callback.accept(originalImage);
                                });
                            }
                        }
                    }
                });
                originalImage.errorProperty().addListener(new javafx.beans.value.ChangeListener<Boolean>() {
                    @Override
                    public void changed(javafx.beans.value.ObservableValue<? extends Boolean> obs, Boolean old, Boolean isError) {
                        if (isError) {
                            originalImage.errorProperty().removeListener(this);
                            loadWithImageIO(coverURL, callback);
                        }
                    }
                });
            }
        }
    }

    private void loadWithImageIO(String coverURL, java.util.function.Consumer<Image> callback) {
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                java.net.URL urlObj = new java.net.URL(coverURL);
                java.net.URLConnection conn = urlObj.openConnection();
                if (coverURL.startsWith("http")) {
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                }
                java.awt.image.BufferedImage bimg;
                try (java.io.InputStream in = conn.getInputStream()) {
                    bimg = javax.imageio.ImageIO.read(in);
                }
                if (bimg != null) {
                    int COVER_MAX_SIZE = 150;
                    if (bimg.getWidth() > COVER_MAX_SIZE || bimg.getHeight() > COVER_MAX_SIZE) {
                        int newWidth = bimg.getWidth();
                        int newHeight = bimg.getHeight();
                        if (newWidth > newHeight) {
                            newHeight = (int) (newHeight * ((double) COVER_MAX_SIZE / newWidth));
                            newWidth = COVER_MAX_SIZE;
                        } else {
                            newWidth = (int) (newWidth * ((double) COVER_MAX_SIZE / newHeight));
                            newHeight = COVER_MAX_SIZE;
                        }
                        java.awt.Image tmp = bimg.getScaledInstance(newWidth, newHeight, java.awt.Image.SCALE_SMOOTH);
                        java.awt.image.BufferedImage scaledBimg = new java.awt.image.BufferedImage(newWidth, newHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                        java.awt.Graphics2D g2d = scaledBimg.createGraphics();
                        g2d.drawImage(tmp, 0, 0, null);
                        g2d.dispose();
                        bimg = scaledBimg;
                    }
                    return javafx.embed.swing.SwingFXUtils.toFXImage(bimg, null);
                }
            } catch (Exception e) {
                Logger.error("Error decodificando cover de búsqueda con ImageIO: " + e.getMessage());
            }
            return null;
        }).thenAccept(img -> {
            if (img != null) {
                javafx.application.Platform.runLater(() -> {
                    SEARCH_IMAGE_CACHE.put(coverURL, img);
                    callback.accept(img);
                });
            }
        });
    }
}