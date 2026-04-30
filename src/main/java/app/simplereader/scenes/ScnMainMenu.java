package app.simplereader.scenes;

import app.simplereader.AppConfig;
import app.simplereader.Logger;
import app.simplereader.Navegador;
import app.simplereader.interfaces.Manga;
import app.simplereader.interfaces.Navigable;
import app.simplereader.manga.MangaLoader;
import app.simplereader.manga.mdManga;
import app.simplereader.scenes.others.SideMenu;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.F7;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 *
 * @author david
 */
public class ScnMainMenu implements Navigable{
    private final Navegador nav;    
    private static List<Manga> mangas;
    private static Scene rootCache;
    
    private TilePane tilepane = new TilePane();
    private ScrollPane scroll;
    public ScnMainMenu(Navegador nav){
        this.nav = nav;
    }
    
    public VBox crearIcon(Manga manga){
        // ImageView del cover del manga
        ImageView coverView = new ImageView();
        coverView.setPreserveRatio(true);
        
        //verificar si el manga sí tenia cover
        if(manga.getCover() != null){
            Image icon = new Image(manga.getCover(), true);
            coverView.setImage(icon);
            Logger.info(manga.getTitle()+" - "+manga.getCover()+" --> Loaded");
        }else{
            coverView.setStyle("-fx-background-color: #cccccc;");
        }
        
        //Crear clip
        Rectangle recorte = new Rectangle();
        recorte.setArcWidth(20);
        recorte.setArcHeight(20);
        recorte.widthProperty().bind(coverView.fitWidthProperty());
        recorte.heightProperty().bind(coverView.layoutBoundsProperty().map(bounds -> bounds.getHeight()));
        
        coverView.setClip(recorte);
        
        //titulo del manga
        Label title = new Label(manga.getTitle());
        //salto de linea auto
        title.setWrapText(true);
        title.setMaxHeight(52); 
        title.maxWidthProperty().bind(coverView.fitWidthProperty());
        title.getStyleClass().add("manga-title");
         // Permite que el label crezca
        title.setAlignment(Pos.CENTER_LEFT);
       
        VBox iconManga = new VBox(5, coverView,title);
        iconManga.setAlignment(Pos.TOP_CENTER);
        
        //evento al hacer clic
        iconManga.setOnMouseClicked(e -> {
            nav.goTo(new ScnMangaMenu(nav,manga));
        });
        iconManga.getStyleClass().add("manga-icon");
        return iconManga;
    }
    
    @Override
    @SuppressWarnings("empty-statement")
    public Scene getScene(){
        if(mangas == null){            
            mangas = MangaLoader.loadMangas();
        }
        if(rootCache != null){
            return rootCache;
        }
        
        int columns = 5;
        double hgap = 15;
        double vgap = 15;
        double padding = 15;        
        
        
        
        tilepane.setHgap(hgap);
        tilepane.setVgap(vgap);
        tilepane.setPadding(new Insets(padding));
        tilepane.setPrefColumns(columns);
        
        
        createTiles();
        
        
        scroll = new ScrollPane(tilepane); 
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        SVGPath icnReload = new SVGPath();
        icnReload.setContent("M480-160q-134 0-227-93t-93-227q0-134 93-227t227-93q69 0 132 28.5T720-690v-110h80v280H520v-80h168q-32-56-87.5-88T480-720q-100 0-170 70t-70 170q0 100 70 170t170 70q77 0 139-44t87-116h84q-28 106-114 173t-196 67Z");
        icnReload.getStyleClass().add("icon");
        double scale = 24.0 / 960.0;
        icnReload.setScaleX(scale);
        icnReload.setScaleY(scale);
        
        Group icon_reload_group = new Group(icnReload);
        StackPane icon_container = new StackPane(icon_reload_group);
        icon_container.setPrefSize(24, 24);
        icon_container.setMaxSize(24, 24);
        
        SVGPath icnImportar = new SVGPath();
        icnImportar.setContent("M440-320v-326L336-542l-56-58 200-200 200 200-56 58-104-104v326h-80ZM240-160q-33 0-56.5-23.5T160-240v-120h80v120h480v-120h80v120q0 33-23.5 56.5T720-160H240Z");
        icnImportar.getStyleClass().add("icon");
        icnImportar.setScaleX(scale);
        icnImportar.setScaleY(scale);
        
        Group icon_importar_group = new Group(icnImportar);
        StackPane icon_container_importar = new StackPane(icon_importar_group);
        
        SVGPath icnAdd = new SVGPath();
        icnAdd.setContent("M440-280h80v-160h160v-80H520v-160h-80v160H280v80h160v160ZM200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Zm0-80h560v-560H200v560Zm0-560v560-560Z");
        icnAdd.getStyleClass().add("icon");
        icnAdd.setScaleX(scale);
        icnAdd.setScaleY(scale);
        
        Group icon_add_group = new Group(icnAdd);
        StackPane icon_container_add = new StackPane(icon_add_group);
        
        Button btnAdd = new Button("",icon_container_add);
        Button btnImportar = new Button("",icon_container_importar);
        Button btnReload = new Button("",icon_container);
        btnReload.setMinSize(24, 24);
        btnReload.setMaxSize(24,24);
        btnImportar.setMinSize(24, 24);
        btnImportar.setMaxSize(24, 24);
        
        btnReload.setOnAction(e -> {       
            Logger.info("- Starting mangas reload.");
            reloadMangas(); 
        });
        btnImportar.setOnAction(e -> {
            importFolder();
        });
        btnAdd.setOnAction(e -> {
            String id = showInputDialog("Ingresar ID del manga");
            if (!id.isBlank()) {
                Manga manga = new mdManga(id);
                mangas.add(manga);
                reloadTiles();
            }
        });
                   
        
        scroll.setFitToWidth(true);
        SideMenu lateralmenu = new SideMenu();
        lateralmenu.addTop(btnReload).addBottom(btnImportar);
        lateralmenu.addBottom(btnAdd);
        BorderPane panel = new BorderPane();
        panel.setCenter(scroll);
        panel.setLeft(lateralmenu.getPane());
        scroll.widthProperty().addListener((obs, oldVal, newVal) -> {
            resizeTiles(newVal.doubleValue());
        });
        
        
        rootCache = new Scene(panel,AppConfig.get().WIDTH,AppConfig.get().HEIGHT);
        rootCache.getStylesheets().add(nav.getCss());
        rootCache.setOnKeyPressed( e -> {
            KeyCode key = e.getCode();
            switch (key){
                case F5 -> {
                    Logger.info("F5");
                    reloadMangas();
                }
                case F12 ->{
                    importFolder();
                }    
                case F7 -> {
                    nav.goTo(new TestScene(nav));
                }
            
            }
        });
        return rootCache;
    }
    private void resizeTiles(double totalWidth) {
        int columns = 5;
        double hgap = 15;
        double vgap = 15;
        double padding = 15;

        double espacioPadding = padding * 2;
        double espacioGaps = hgap * (columns - 1);
        double anchoDisponible = totalWidth - espacioPadding - espacioGaps - 5;

        double tileWidth = anchoDisponible / columns;
        double tileHeight = tileWidth * 1.5;

        tilepane.setPrefTileWidth(tileWidth);
        tilepane.setPrefTileHeight(tileHeight + 45);

        for (javafx.scene.Node node : tilepane.getChildren()) {
            if (node instanceof VBox vbox) {
                vbox.setPrefWidth(tileWidth);
                vbox.setPrefHeight(tileHeight + 45);
                if (!vbox.getChildren().isEmpty() && vbox.getChildren().get(0) instanceof ImageView iv) {
                    iv.setFitWidth(tileWidth);
                }
            }
        }
    }
    private void createTiles(){
        for(Manga manga : mangas){
            if(manga.getCover() != null){
                VBox iconManga = crearIcon(manga);
                tilepane.getChildren().add(iconManga);
            } else {
                Logger.warning(manga.getTitle()+" - no tiene una cover.");
            }
        }
    }
    
    private void reloadTiles(){
        tilepane.getChildren().clear();
        createTiles();
        resizeTiles(scroll.getWidth());
    }
    private void importFolder(){
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
    public static String showInputDialog(String titulo) {
        Stage dialog = new Stage();
        dialog.setTitle(titulo);
        dialog.initModality(Modality.APPLICATION_MODAL); // bloquea la ventana principal

        TextField textField = new TextField();
        Button btnOk = new Button("OK");

        final String[] resultado = {""};

        btnOk.setOnAction(e -> {
            resultado[0] = textField.getText();
            dialog.close();
        });

        VBox layout = new VBox(10, textField, btnOk);
        layout.setPadding(new Insets(20));

        dialog.setScene(new Scene(layout, 300, 100));
        dialog.showAndWait(); // espera a que se cierre

        return resultado[0];
    }
    private void reloadMangas(){
        mangas = null;
        rootCache = null;
        nav.goTo(new ScnMainMenu(nav));
    }
    @Override
    public String getName(){
        return "Menu";
    }
    @Override
    public String getParentName(){
        return "Main";
    }
}