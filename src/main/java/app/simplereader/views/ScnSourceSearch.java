package app.simplereader.views;
import app.simplereader.views.components.SideMenu;
import app.simplereader.controller.LibraryController;
import app.simplereader.controller.Logger;
import app.simplereader.controller.SceneController;
import app.simplereader.controller.SourceManager;
import app.simplereader.model.AppConfig;
import app.simplereader.model.Category;
import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import app.simplereader.repository.MangaSource;
import app.simplereader.repository.AppScene;
import app.simplereader.views.components.Buttons;
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
    public Scene getScene() {
        SideMenu lateralmenu = new SideMenu();
        Button btnBack = Buttons.getBackButton();
        
        lateralmenu.addTop(btnBack);
        btnBack.setOnAction(e-> nav.backScene());
        searchField = new TextField();
        searchField.setPromptText("Buscar en " + source.getName() + "...");
        searchField.setMaxSize(800, 30);
        searchField.setMinSize(800, 30);
        searchField.getStyleClass().add("search-bar");
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
            @Override
            protected void updateItem(Manga manga, boolean empty) {
                super.updateItem(manga, empty);
                if (empty || manga == null) {
                    setText(null);
                } else {
                    setText(manga.getTitle());
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
                nav.goTo(new ScnMangaMenu(mangaToRead));
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
        Scene scene = new Scene(root, AppConfig.get().WIDTH, AppConfig.get().HEIGHT);
        scene.getStylesheets().add(nav.getCss());
        return scene;
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
}