package app.simplereader.controller;

import app.simplereader.views.MangaTile;
import app.simplereader.views.ScnMainMenu;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import app.simplereader.repository.MangaInterface;

/**
 *
 * @author david
 */
public class MainMenuController {
    
    private SceneController nav;
    private CategoryController manager;
    private ScnMainMenu mainMenu;
    private List<MangaInterface> mangas = new ArrayList<>();
    
    private static String actualCategory = "Default";
    
    private MangaController MangaControler;
    
    public MainMenuController(SceneController nav, CategoryController manager, ScnMainMenu mainMenu){
        this.nav = nav;
        this.manager = manager;
        this.mainMenu = mainMenu;
        this.MangaControler = new MangaController(mainMenu, manager);
    }
    
    public void resizeTiles(double totalWidth) {
         // Obtener el pane activo en lugar de siempre DefaultPane
        TilePane activePane = (TilePane) mainMenu.getScroll().getContent();
        if (activePane == null) return;

        int columns = 5;
        double hgap = 15, vgap = 15, padding = 15;
        double tileWidth = (totalWidth - padding * 2 - hgap * (columns - 1) - 5) / columns;
        double tileHeight = tileWidth * 1.5;

        activePane.setPrefTileWidth(tileWidth);
        activePane.setPrefTileHeight(tileHeight + 45);

        for (javafx.scene.Node node : activePane.getChildren()) {
            if (node instanceof VBox vbox) {
                vbox.setPrefWidth(tileWidth);
                vbox.setPrefHeight(tileHeight + 45);
                if (!vbox.getChildren().isEmpty() && vbox.getChildren().get(0) instanceof StackPane container) {
                    container.setPrefWidth(tileWidth);
                    container.setPrefHeight(tileHeight);
                }
            }
        }
    }
    
    public void loadMangas(){

        if (mangas == null || mangas.isEmpty()) {
            // Muestra un estado de carga mientras espera
//            Label lblCargando = new Label("Cargando mangas...");
//            lblCargando.getStyleClass().add("loading-label");
//            DefaultPane.getChildren().add(lblCargando);

            Task<List<MangaInterface>> tareaCargar = new Task<>() {
                @Override
                protected List<MangaInterface> call() {
                    return MangaControler.loadMangas(); // Se ejecuta en hilo de fondo
                }
            };

            tareaCargar.setOnSucceeded(e -> {
                // Esto se ejecuta de vuelta en el Application Thread
                mangas = tareaCargar.getValue();
//                DefaultPane.getChildren().clear();
                createTiles();
                //resizeTiles(scroll.getWidth());
            });

            tareaCargar.setOnFailed(e -> {
                Logger.error("Error cargando mangas: " + tareaCargar.getException().getMessage());
//                DefaultPane.getChildren().clear();
//                DefaultPane.getChildren().add(new Label("Error al cargar mangas."));
            });

            Thread hilo = new Thread(tareaCargar);
            hilo.setDaemon(true);
            hilo.start();
        }
    }
    
    private void createTiles(){
        for(MangaInterface manga : mangas){
            if(manga.getCover() != null){
                Platform.runLater(() -> {
                    VBox iconManga = MangaTile.create(manga,mainMenu.getSceneController());
                    //DefaultPane.getChildren().add(iconManga);
                    manager.getCategories().get(manga.getCategory()).getPane().getPane().getChildren().add(iconManga);
                });
                Platform.runLater(() -> resizeTiles(mainMenu.getScroll().getWidth()));

            } else {
                Logger.warning(manga.getTitle()+" - no tiene una cover.");
            }
        }
    }
    
    public void reloadMangas(){
        manager.getCategories().get("Default").getMangas().clear();
        manager.getCategories().get("Default").getPane().getPane().getChildren().clear();
        for(String name : manager.getNameList()){
            manager.getCategories().get(name).getMangas().clear();
            manager.getCategories().get(name).getPane().getPane().getChildren().clear();
        }
        mangas.clear();
        loadMangas();
    }
    
    public void showCategory(String Name) {
        TilePane pane = manager.getCategories().get(Name).getPane().getPane();

        // Configurar el pane si no está configurado aún
        pane.setHgap(15);
        pane.setVgap(15);
        pane.setPadding(new Insets(15));
        pane.setPrefColumns(5);

        mainMenu.getScroll().setContent(pane);
        resizeTiles(mainMenu.getScroll().getWidth());
        actualCategory = Name;
    }
    
    public String getActualCategory(){
        return actualCategory;
    }
    
    public void importFolder(){
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Importar manga");
        
        File selectedDirectory = dirChooser.showDialog(nav.getStage());
        
        if(selectedDirectory != null){
            Logger.info("Carpeta seleccionada: " + selectedDirectory.getAbsolutePath());
            copytoMangaFolder(selectedDirectory);
        }
        
        reloadMangas();
    }
    
    private void copytoMangaFolder(File source){
        String home = System.getProperty("user.home");
        
        Path mangafolder = Paths.get(home + "/Documents/SimpleReader/mangas");
        Path sourcePath = source.toPath();
        Path targetPath = mangafolder.resolve(source.getName());
        
        try {
            if (source.isDirectory()) {
                // Copiar directorio (requiere Java 8+)
                // Usamos walk para copiar el contenido recursivamente
                Files.walk(sourcePath).forEach(sourceItem -> {
                    Path destination = targetPath.resolve(sourcePath.relativize(sourceItem));
                    try {
                        Files.copy(sourceItem, destination, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        Logger.info("Error copiando el archivo: " + sourceItem);
                        e.printStackTrace();
                    }
                });
                Logger.info("Carpeta importada exitosamente a: " + targetPath);
            } else {
                // Copiar archivo simple (.zip o .cbz)
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                Logger.info("Archivo importado exitosamente a: " + targetPath);
            }

            // ¡IMPORTANTE! Aquí debes llamar al método que recarga la lista de mangas en tu menú principal
            // Por ejemplo: reloadMangaList(); 

        } catch (IOException e) {
            Logger.info("Error al importar: " + e.getMessage());
        }
        
    }
}