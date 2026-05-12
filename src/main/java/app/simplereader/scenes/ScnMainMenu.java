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
import javafx.concurrent.Task;
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
import javafx.scene.text.TextAlignment;
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
    private final MangaLoader MangaLaoder;
    
    
    private static TilePane tilepane = new TilePane();
    private static ScrollPane scroll;
    public ScnMainMenu(Navegador nav){
        this.nav = nav;
        this.MangaLaoder = new MangaLoader(this);
        nav.getStage().setResizable(false);
    }
    
    public TilePane getTilePane(){
        return tilepane;
    }
    
    public ScrollPane getScroll(){
        return scroll;
    }
    
    public VBox crearIcon(Manga manga){
        ImageView coverView = new ImageView();
        coverView.setPreserveRatio(true);
        coverView.setManaged(false);

        StackPane coverContainer = new StackPane(coverView);
        coverContainer.setMaxSize(250, Double.MAX_VALUE);

        if(manga.getCover() != null){

            Image icon = new Image(manga.getCover(), true);
            coverView.setImage(icon);

            icon.progressProperty().addListener((obs, old, progress) -> {

                if(progress.doubleValue() >= 1.0){

                    double imageRatio = icon.getWidth() / icon.getHeight();

                    // ratio del contenedor (2:3)
                    double containerRatio = 2.0 / 3.0;

                    // Limpiar bindings anteriores
                    coverView.fitWidthProperty().unbind();
                    coverView.fitHeightProperty().unbind();

                    if(imageRatio < containerRatio){

                        // Imagen muy angosta
                        // llenar ancho y recortar arriba/abajo
                        coverView.fitWidthProperty().bind(
                            coverContainer.widthProperty()
                        );

                    } else {

                        // Imagen normal o ancha
                        // llenar alto y recortar lados
                        coverView.fitHeightProperty().bind(
                            coverContainer.heightProperty()
                        );
                    }

                    // Centrar imagen
                    coverView.layoutXProperty().bind(
                        javafx.beans.binding.Bindings.createDoubleBinding(
                            () -> (
                                coverContainer.getWidth()
                                - coverView.getBoundsInLocal().getWidth()
                            ) / 2.0,
                            coverContainer.widthProperty(),
                            coverView.boundsInLocalProperty()
                        )
                    );

                    coverView.layoutYProperty().bind(
                        javafx.beans.binding.Bindings.createDoubleBinding(
                            () -> (
                                coverContainer.getHeight()
                                - coverView.getBoundsInLocal().getHeight()
                            ) / 2.0,
                            coverContainer.heightProperty(),
                            coverView.boundsInLocalProperty()
                        )
                    );
                }
            });

            Logger.info(manga.getTitle()+" - "+manga.getCover()+" --> Loaded");
        }

        // Clip redondeado
        Rectangle recorte = new Rectangle();
        recorte.setArcWidth(20);
        recorte.setArcHeight(20);

        recorte.widthProperty().bind(coverContainer.widthProperty());
        recorte.heightProperty().bind(coverContainer.heightProperty());

        coverContainer.setClip(recorte);

        // Título
        Label title = new Label(manga.getTitle());

        title.setWrapText(true);
        title.setTextAlignment(TextAlignment.CENTER);
        title.setAlignment(Pos.TOP_CENTER);

        title.setMinHeight(50);
        title.setMaxHeight(50);

        title.setMaxWidth(Double.MAX_VALUE);

        title.getStyleClass().add("manga-title");

        VBox iconManga = new VBox(5, coverContainer, title);

        iconManga.setFillWidth(true);

        title.prefWidthProperty().bind(iconManga.widthProperty());

        iconManga.setAlignment(Pos.TOP_LEFT);

        iconManga.setOnMouseClicked(
            e -> nav.goTo(new ScnMangaMenu(nav, manga))
        );

        iconManga.getStyleClass().add("manga-icon");

        return iconManga;
    }
    
    @Override
    @SuppressWarnings("empty-statement")
    public Scene getScene(){
        if (mangas == null) {
            // Muestra un estado de carga mientras espera
//            Label lblCargando = new Label("Cargando mangas...");
//            lblCargando.getStyleClass().add("loading-label");
//            tilepane.getChildren().add(lblCargando);

            Task<List<Manga>> tareaCargar = new Task<>() {
                @Override
                protected List<Manga> call() {
                    return MangaLoader.loadMangas(); // Se ejecuta en hilo de fondo
                }
            };

            tareaCargar.setOnSucceeded(e -> {
                // Esto se ejecuta de vuelta en el Application Thread
                mangas = tareaCargar.getValue();
                //tilepane.getChildren().clear();
                //createTiles();
                //resizeTiles(scroll.getWidth());
            });

            tareaCargar.setOnFailed(e -> {
                Logger.error("Error cargando mangas: " + tareaCargar.getException().getMessage());
                tilepane.getChildren().clear();
                tilepane.getChildren().add(new Label("Error al cargar mangas."));
            });

            Thread hilo = new Thread(tareaCargar);
            hilo.setDaemon(true);
            hilo.start();
        }
        if(rootCache != null){
            return rootCache;
        }
        
        int columns = 5;
        double hgap = 15;
        double vgap = 15;
        double padding = 15;        
        
        
        // Panel con las tiles
        tilepane.setHgap(hgap);
        tilepane.setVgap(vgap);
        tilepane.setPadding(new Insets(padding));
        tilepane.setPrefColumns(columns);
        
        
        
        // Scroll
        scroll = new ScrollPane(tilepane); 
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        
        // Iconos
        
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
        
        // Botones
        
        Button btnAdd = new Button("",icon_container_add);
        Button btnImportar = new Button("",icon_container_importar);
        btnImportar.setMinSize(24, 24);
        btnImportar.setMaxSize(24, 24);
        Button btnReload = new Button("",icon_container);
        btnReload.setMinSize(24, 24);
        btnReload.setMaxSize(24,24);
        
        Button btnConfig = new Button("");
        btnConfig.setOnAction(e -> { 
            nav.goTo(new ScnConfig(this.nav));
        });
        
        
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
                manga.saveData();
                reloadMangas();
            }
        });
                   
        
        scroll.setFitToWidth(true);
        
        // Menu lateral
        
        SideMenu lateralmenu = new SideMenu();
        lateralmenu.addTop(btnReload).addBottom(btnImportar);
        lateralmenu.addBottom(btnAdd);
        lateralmenu.addBottom(btnConfig);

        BorderPane panel = new BorderPane();
        panel.setCenter(scroll);
        panel.setLeft(lateralmenu.getPane());
        scroll.widthProperty().addListener((obs, oldVal, newVal) -> {
            resizeTiles(newVal.doubleValue());
        });
        
        // Panel con todo
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
    public void resizeTiles(double totalWidth) {
        int columns = 5;
        double hgap = 15, vgap = 15, padding = 15;

        double tileWidth = (totalWidth - padding * 2 - hgap * (columns - 1) - 5) / columns;
        double tileHeight = tileWidth * 1.5; // ratio 2:3 típico de portadas

        tilepane.setPrefTileWidth(tileWidth);
        tilepane.setPrefTileHeight(tileHeight + 45);

        for (javafx.scene.Node node : tilepane.getChildren()) {
            if (node instanceof VBox vbox) {
                vbox.setPrefWidth(tileWidth);
                vbox.setPrefHeight(tileHeight + 45);

                // Ahora buscamos el StackPane, no el ImageView
                if (!vbox.getChildren().isEmpty() && vbox.getChildren().get(0) instanceof StackPane container) {
                    container.setPrefWidth(tileWidth);
                    container.setPrefHeight(tileHeight); // ← aquí limitas el alto de la imagen
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
        tilepane.getChildren().clear();
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