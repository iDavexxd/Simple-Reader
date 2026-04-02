package app.simplereader.scenes;

import app.simplereader.AppConfig;
import app.simplereader.Logger;
import app.simplereader.Navegador;
import app.simplereader.interfaces.Navigable;
import app.simplereader.manga.Manga;
import app.simplereader.manga.MangaLoader;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/**
 *
 * @author david
 */
public class ScnMainMenu implements Navigable{
    private final Navegador nav;    
    private static List<Manga> mangas;
    private static Scene rootCache;
    
    public ScnMainMenu(Navegador nav){
        this.nav = nav;
    }
    
    public VBox crearIcon(Manga manga){
        // ImageView del cover del manga
        ImageView coverView = new ImageView();
        coverView.setPreserveRatio(true);
        
        //verificar si el manga sí tenia cover
        if(manga.getCover() != null){
            Image icon = new Image(manga.getCover().toURI().toString(), true);
            coverView.setImage(icon);
            Logger.info(manga.getTitle()+" - "+manga.getCover().getName()+" --> Loaded");
        }else{
            coverView.setStyle("-fx-background-color: #cccccc;");
        }
         
        //Crear clip
        Rectangle recorte = new Rectangle();
        recorte.setArcWidth(30);
        recorte.setArcHeight(30);
        
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
            Logger.info("Cliqueaste: "+manga.getTitle());
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
        
        
        TilePane tilepane = new TilePane();
        tilepane.setHgap(hgap);
        tilepane.setVgap(vgap);
        tilepane.setPadding(new Insets(padding));
        tilepane.setPrefColumns(columns);
        
        
        for(Manga manga : mangas){
            if(manga.getCover() != null){
                VBox iconManga = crearIcon(manga);
                tilepane.getChildren().add(iconManga);
            } else {
                Logger.warning(manga.getTitle()+" - no tiene una cover.");
            }
        }
        
        
        ScrollPane scroll = new ScrollPane(tilepane); 
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        Button btnImportar = new Button("");
        Button btnReload = new Button("");
        
        btnReload.setOnAction(e -> {       
            Logger.info("- Starting mangas reload.");
            rootCache = null;
            mangas = null;
            nav.goTo(new ScnMainMenu(nav)); 
        });
        AnchorPane lateralmenu = new AnchorPane();
        lateralmenu.getStyleClass().add("side-menu");
        lateralmenu.setPrefWidth(45);
        lateralmenu.setMinWidth(45);
        lateralmenu.setMaxWidth(45);

        // Anclamos Importar al techo
        AnchorPane.setTopAnchor(btnReload, 10.0);
        AnchorPane.setLeftAnchor(btnReload, 0.0);
        AnchorPane.setRightAnchor(btnReload, 0.0);

        // Anclamos Reload al suelo
        AnchorPane.setBottomAnchor(btnImportar, 10.0);
        AnchorPane.setLeftAnchor(btnImportar, 0.0);
        AnchorPane.setRightAnchor(btnImportar, 0.0);

        lateralmenu.getChildren().addAll(btnImportar, btnReload);      
                   
        
        scroll.setFitToWidth(true);
        
        BorderPane panel = new BorderPane();
        panel.setCenter(scroll);
        panel.setLeft(lateralmenu);
        scroll.widthProperty().addListener((obs, oldVal, newVal) -> {
            double totalWidth = newVal.doubleValue();

            // 1. Bajamos un poco el padding visual
            double espacioPadding = padding * 2; 
            double espacioGaps = hgap * (columns - 1);

            // 2. Usamos un margen de error pequeño (5px) en lugar de los 25px de antes
            // Ya que si ocultas la barra, no necesitas reservar tanto espacio.
            double anchoDisponible = totalWidth - espacioPadding - espacioGaps - 5;

            double tileWidth = anchoDisponible / columns;
            double tileHeight = tileWidth * 1.5;

            tilepane.setPrefTileWidth(tileWidth);
            tilepane.setPrefTileHeight(tileHeight + 45); // Bajé de 40 a 25 para que no haya tanto hueco abajo

            for (javafx.scene.Node node : tilepane.getChildren()) {
                if (node instanceof VBox vbox) {
                    vbox.setPrefWidth(tileWidth);
                    vbox.setPrefHeight(tileHeight + 45);

                    // 🔹 Buscamos la ImageView dentro del VBox para redimensionarla
                    if (!vbox.getChildren().isEmpty() && vbox.getChildren().get(0) instanceof ImageView iv) {
                        iv.setFitWidth(tileWidth); 
                    }
                }
            }
        });
        
        
        rootCache = new Scene(panel,AppConfig.get().WIDTH,AppConfig.get().HEIGHT);
        rootCache.getStylesheets().add(nav.getCss());
        rootCache.setOnKeyPressed( e -> {
            KeyCode key = e.getCode();
            switch (key){
                case F5 -> {
                    Logger.info("F5");
                    mangas = null;
                    rootCache = null;
                    nav.goTo(new ScnMainMenu(nav));
                }
            }
        });
        return rootCache;
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