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
import app.simplereader.views.components.SvgIcons;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
public class ScnSourceSearch implements AppScene {
    private final SceneController nav = SceneController.getInstance();
    private final app.simplereader.repository.AppExtension extension;
    private MangaSource currentSource;
    private final SvgIcons icons = SvgIcons.get();
    private static final java.util.concurrent.ExecutorService networkExecutor = java.util.concurrent.Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });
    
    private TextField searchField;
    private ListView<Manga> resultsList;

    private BorderPane loadingPane;
    private boolean loadingPane_visible = false;
    private Image loadingGif;
    
    private BorderPane infoBorderPane;
    private boolean infoMenuVisible = false;

    private final ObservableList<Manga> results = FXCollections.observableArrayList();
    public ScnSourceSearch(app.simplereader.repository.AppExtension extension) {
        this.extension = extension;
        if (extension.getSources() != null && !extension.getSources().isEmpty()) {
            this.currentSource = extension.getSources().get(0);
        }
        try {
            loadingGif = new Image(getClass().getResource("/icons/koruko.gif").toExternalForm());
        } catch (Exception e) {
            Logger.error("No se pudo cargar el gif de carga: " + e.getMessage());
        }
    }
    
    public void doConfigLoadingPane(){
        ImageView gifView = new ImageView(loadingGif);
        gifView.setFitWidth(150);
        gifView.setFitHeight(150);
        gifView.setPreserveRatio(true);
        
        Label loadingLabel = new Label("Cargando...");
        loadingLabel.getStyleClass().add("reader-loading-label");
        
        VBox loadingOverlay = new VBox(15, gifView, loadingLabel);
        loadingOverlay.setAlignment(Pos.CENTER);
        
        loadingPane = new BorderPane();
        loadingPane.setCenter(loadingOverlay);
        loadingPane.getStyleClass().add("menu-background");
        loadingPane.setVisible(loadingPane_visible);
    }

    private void doShowLoadingPane(){
        loadingPane_visible = true;
        if(loadingPane != null) loadingPane.setVisible(loadingPane_visible);
    }

    private void doHideLoadingPane(){
        loadingPane_visible = false;
        if(loadingPane != null) loadingPane.setVisible(loadingPane_visible);
    }
    
    public void doCreateInfoPane(){
        // top
        Label menuTitle = new Label();
        menuTitle.setText("Info");
        
        Button btnClose = new Button("",icons.getCloseIcon());
        btnClose.setMinSize(30, 30);
        btnClose.setMaxSize(30, 30);
        btnClose.setOnAction(e -> doHideInfoMenu());
        
        javafx.scene.layout.Region topSpacer = new javafx.scene.layout.Region();
        HBox.setHgrow(topSpacer, javafx.scene.layout.Priority.ALWAYS);
        
        HBox topcontent = new HBox(menuTitle,topSpacer,btnClose);
        
        // center
        Label lblTitle = new Label(extension.getName());
        lblTitle.getStyleClass().add("info-label");
        if(lblTitle.getStyleClass().isEmpty()) {} // avoid issues if style missing
        Label lblVersion = new Label("Version: " + extension.getVersion());
        lblVersion.getStyleClass().add("info-label");
        
        Label lblAuthor = new Label("Author: " + extension.getAuthor());
        lblAuthor.getStyleClass().add("info-label");
        
        VBox centercontent = new VBox(15, lblTitle, lblVersion, lblAuthor);
        centercontent.setAlignment(Pos.CENTER_LEFT);
        
        VBox allContent = new VBox(20, topcontent, centercontent);
        
        StackPane infoMenu = new StackPane(allContent);
        infoMenu.getStyleClass().add("source-menu");
        infoMenu.setPadding(new Insets(15));

        infoMenu.setMaxSize(300, 450);
        infoMenu.setMinHeight(450);

        infoBorderPane = new BorderPane();
        infoBorderPane.getStyleClass().add("menu-background");
        infoBorderPane.setCenter(infoMenu);
        infoBorderPane.setVisible(infoMenuVisible);
    }
    
    private void doShowInfoMenu(){
        infoMenuVisible = true;
        if(infoBorderPane != null) infoBorderPane.setVisible(infoMenuVisible);
    }
    
    private void doHideInfoMenu(){
        infoMenuVisible = false;
        if(infoBorderPane != null) infoBorderPane.setVisible(infoMenuVisible);
    }

    @Override
    public javafx.scene.Parent getScene() {
        SideMenu lateralmenu = new SideMenu();
        Button btnBack = Buttons.getBackButton();
        
        Button btnInfo = new Button("", icons.getInfoIcon());
        btnInfo.setMinSize(24, 24);
        btnInfo.setMaxSize(24, 24);
        btnInfo.setOnAction(e -> doShowInfoMenu());
        
        lateralmenu.addTop(btnBack);
        lateralmenu.addBottom(btnInfo);
        btnBack.setOnAction(e-> nav.backScene());
        searchField = new TextField();
        searchField.setPromptText("Buscar en " + extension.getName() + "...");
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

        javafx.scene.control.ComboBox<String> cbLang = new javafx.scene.control.ComboBox<>();
        if (extension.getSources() != null) {
            for (MangaSource s : extension.getSources()) {
                String lang = s.getLang();
                cbLang.getItems().add(lang == null || lang.isEmpty() ? "ALL" : lang.toUpperCase());
            }
            if (!cbLang.getItems().isEmpty()) {
                cbLang.getSelectionModel().selectFirst();
            }
        }
        cbLang.getStyleClass().add("reader-combobox");
        cbLang.setOnAction(e -> {
            int index = cbLang.getSelectionModel().getSelectedIndex();
            if (index >= 0 && index < extension.getSources().size()) {
                currentSource = extension.getSources().get(index);
                if (!searchField.getText().isBlank()) doSearch();
            }
        });

        HBox searchBar = new HBox(5, searchField, btnSearch, cbLang);
        searchBar.setPadding(new Insets(10));
        searchBar.setAlignment(Pos.CENTER);
        
        Label title = new Label(extension.getName());
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
                
                final Manga mangaToLoad = mangaToRead;
                doShowLoadingPane();
                
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    // 3. Cargar capítulos frescos desde el Source
                    List<Chapter> newChapters = currentSource.getChapters(mangaToLoad.getMangaID());
                    
                    // 4. CRUCIAL: Fusionar el progreso de lectura (leído/página) a los nuevos capítulos
                    if (mangaToLoad.getChapters() != null) {
                        Map<String, Chapter> oldChaptersMap = new HashMap<>();
                        for (Chapter oldCh : mangaToLoad.getChapters()) {
                            oldChaptersMap.put(oldCh.getChapterID(), oldCh);
                        }
                        
                        for (Chapter newCh : newChapters) {
                            Chapter oldCh = oldChaptersMap.get(newCh.getChapterID());
                            if (oldCh != null) {
                                if (oldCh.isReaded()) newCh.markAsReaded();
                                newCh.setLastRead(oldCh.getLastRead());
                            }
                            newCh.setManga(mangaToLoad); // Enlazar al padre
                        }
                    } else {
                        for (Chapter ch : newChapters) {
                            ch.setManga(mangaToLoad);
                        }
                    }
                    
                    // 5. Asignar la lista fusionada y cargar metadatos
                    mangaToLoad.setChapters(newChapters);
                    SourceManager.getInstance().fetchMangaData(mangaToLoad);
                    
                    // 6. Navegar con la instancia correcta
                    javafx.application.Platform.runLater(() -> {
                        doHideLoadingPane();
                        ScnMangaMenu menu = ScnMangaMenu.getInstance();
                        menu.updateManga(mangaToLoad);
                        nav.goTo(menu);
                    });
                }, networkExecutor).exceptionally(ex -> {
                    Logger.error("Error en CompletableFuture: " + ex.getMessage());
                    ex.printStackTrace();
                    javafx.application.Platform.runLater(() -> doHideLoadingPane());
                    return null;
                });
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
                if(loadingPane_visible) return;
                nav.backScene();
                e.consume();
            }
        });
        
        doConfigLoadingPane();
        doCreateInfoPane();
        StackPane rootStack = new StackPane(root, loadingPane, infoBorderPane);
        if(loadingPane_visible) loadingPane.setVisible(true);
        else loadingPane.setVisible(false);
        
        return rootStack;
    }
    
    private void doSearch() {
        String query = searchField.getText();
        if (query.isBlank()) return;
        
        results.clear();
        searchField.setDisable(true);
        doShowLoadingPane();
        
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            List<Manga> found = SourceManager.getInstance().searchManga(currentSource, query);
            
            javafx.application.Platform.runLater(() -> {
                doHideLoadingPane();
                if (found != null) {
                    results.addAll(found);
                }
                searchField.setDisable(false);
            });
        }, networkExecutor);
    }
    
    @Override
    public String getName() { return "Search - " + extension.getName(); }
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
                java.net.URLConnection conn = app.simplereader.service.Http.getConnection(coverURL, currentSource);
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
        }, networkExecutor).thenAccept(img -> {
            if (img != null) {
                javafx.application.Platform.runLater(() -> {
                    SEARCH_IMAGE_CACHE.put(coverURL, img);
                    callback.accept(img);
                });
            }
        });
    }
}