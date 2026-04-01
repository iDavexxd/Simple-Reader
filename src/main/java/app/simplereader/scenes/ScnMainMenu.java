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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
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
    public ScnMainMenu(Navegador nav){
        this.nav = nav;
    }
    
    public VBox crearIcon(Manga manga){
        // ImageView del cover del manga
        ImageView coverView = new ImageView();
        coverView.setPreserveRatio(true);
        
        //verificar si el manga sí tenia cover
        if(manga.getCover() != null){
            Image icon = new Image(manga.getCover().toURI().toString());
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
    public Parent getParent(){
        if(mangas == null){            
            mangas = MangaLoader.loadMangas();;
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
            VBox iconManga = crearIcon(manga);
            tilepane.getChildren().add(iconManga);
        }
        
        ScrollPane scroll = new ScrollPane(tilepane); 
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox lateralmenu = new VBox();
        
        Button btnImportar = new Button("Importar Manga");
        Button btnReload = new Button("");
        
        btnReload.setOnAction(e -> {
            mangas = null;
            reloadMangas();
            nav.goTo(new ScnMainMenu(nav));
        });
        btnImportar.getStyleClass().add("menu-button");
        btnReload.getStyleClass().add("menu-button");
        int lmsize = 20;
        
        lateralmenu.getStyleClass().add("side-menu");
        lateralmenu.getChildren().add(btnImportar);
        lateralmenu.getChildren().add(btnReload);
        btnImportar.setAlignment(Pos.CENTER);
        btnReload.setAlignment(Pos.BOTTOM_CENTER);
        lateralmenu.setPrefWidth(lmsize);
        lateralmenu.setMaxWidth(lmsize);        
                   
        
        
        
        
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
            tilepane.setPrefTileHeight(tileHeight + 39); // Bajé de 40 a 25 para que no haya tanto hueco abajo

            for (javafx.scene.Node node : tilepane.getChildren()) {
                if (node instanceof VBox vbox) {
                    vbox.setPrefWidth(tileWidth);
                    vbox.setPrefHeight(tileHeight + 40);

                    // 🔹 Buscamos la ImageView dentro del VBox para redimensionarla
                    if (!vbox.getChildren().isEmpty() && vbox.getChildren().get(0) instanceof ImageView iv) {
                        iv.setFitWidth(tileWidth); 
                    }
                }
            }
        });
        return panel;
    }
    
    private void reloadMangas(){
        List<Manga> newmangas = MangaLoader.loadMangas();
        mangas.clear();
        mangas = newmangas;
    }
    
    @Override
    public String getName(){
        return "Menu";
    }
}