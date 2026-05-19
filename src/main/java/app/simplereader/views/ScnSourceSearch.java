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
public class ScnSourceSearch implements AppScene {
    private final SceneController nav = SceneController.getInstance();
    private final MangaSource source;
    
    private TextField searchField;
    private ListView<Manga> resultsList;
    // Lista observable vinculada al ListView
    private ObservableList<Manga> results = FXCollections.observableArrayList();
    public ScnSourceSearch(MangaSource source) {
        this.source = source;
    }
    @Override
    public Scene getScene() {
        SideMenu lateralmenu = new SideMenu();
        Button btnBack = new Button("<-");
        
        lateralmenu.addTop(btnBack);
        btnBack.setOnAction(e-> nav.backScene());
        searchField = new TextField();
        searchField.setPromptText("Buscar en " + source.getName() + "...");
        
        Button btnSearch = new Button("Buscar");
        btnSearch.setOnAction(e -> doSearch());
        
        HBox topBar = new HBox(10, searchField, btnSearch);
        topBar.setPadding(new Insets(10));
        resultsList = new ListView<>(results);
        
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
        // Al hacer click en un manga
        resultsList.setOnMouseClicked(e -> {
            Manga selected = resultsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Logger.info("Cargando detalles de: " + selected.getTitle());
                
                LibraryController lib = LibraryController.getInstance();
                Manga mangaToRead = null;
                // 1. Buscar si ya existe en la biblioteca
                for (Category cat : lib.getAllCategories()) {
                    for (Manga m : cat.getMangas()) {
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
        VBox vboxconto = new VBox(10, topBar, resultsList);
        VBox.setVgrow(resultsList, javafx.scene.layout.Priority.ALWAYS);
        BorderPane root = new BorderPane();
        root.setLeft(lateralmenu.getPane());
        root.setCenter(vboxconto);
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