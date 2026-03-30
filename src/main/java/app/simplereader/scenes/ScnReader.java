package app.simplereader.scenes;

import app.simplereader.AppConfig;
import app.simplereader.Logger;
import app.simplereader.Navegador;
import app.simplereader.interfaces.Navigable;
import app.simplereader.manga.Chapter;
import app.simplereader.manga.Manga;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 *
 * @author david
 */
public class ScnReader implements Navigable{
    
    private final Navegador nav;
    private final Chapter chapter;
    private final int chapternum;
    private final Manga manga;
    public ScnReader(Navegador nav,Manga manga, Chapter chapter, int indice){
        this.nav = nav;
        this.chapter = chapter;
        this.chapternum = indice;
        this.manga = manga;
        
        Logger.info("Loaded "+chapter.getChName()+" "+chapternum);
    }
    
    private ImageView visor;
    private List<File> imagenes;
    private int indiceactual = 0;
    
    @Override
    public Parent getParent(){
        Label lblPage = new Label("0/0");
        visor = new ImageView();
        visor.setFitHeight(AppConfig.get().HEIGHT);
        visor.setFitWidth(AppConfig.get().WIDTH);
        visor.setPreserveRatio(true);
        visor.setSmooth(true);
        visor.setFitWidth(0);
        visor.setFitHeight(0);
        
        visor.setScaleX(1.0);
        visor.setScaleY(1.0);
        StackPane visorpanel = new StackPane(visor);
        ScrollPane scrollVisor = new ScrollPane();
        scrollVisor.setContent(visorpanel);
        
        scrollVisor.setPannable(true);
        scrollVisor.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollVisor.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        scrollVisor.setFitToWidth(true); 
        scrollVisor.setFitToHeight(true);
        
        scrollVisor.setOnScroll(event -> {
            if (event.isControlDown()) {
                double deltaY = event.getDeltaY();
                // Factor de sensibilidad (ajusta este 0.001 a tu gusto)
                double zoomFactor = Math.exp(deltaY * 0.001); 

                double newScaleX = visor.getScaleX() * zoomFactor;
                double newScaleY = visor.getScaleY() * zoomFactor;

                // Límites razonables
                if (newScaleX > 0.1 && newScaleX < 10) {
                    visor.setScaleX(newScaleX);
                    visor.setScaleY(newScaleY);
                }
                event.consume();
            }
        });
        visor.setFitWidth(scrollVisor.getWidth() - 10); 
        visor.setFitHeight(scrollVisor.getHeight() - 10);
        imagenes = loadImages();
        if (!imagenes.isEmpty()) {
            LoadImage();
            relbl(lblPage);
        }
        //Botones
        Button btnNext = new Button("->");
        Button btnBack = new Button("<-");
        Button btnNextCh = new Button("Next");
        Button btnBackCh = new Button("Back");
        Button btnReload = new Button("Reload");
        Button btnBackToMenu = new Button("Menu");
        btnNext.setOnAction(e -> {
            if (indiceactual < imagenes.size() - 1) { indiceactual++; }
            LoadImage();
            resetZoom();
            relbl(lblPage);
            Logger.info("Now in: "+imagenes.get(indiceactual).getName());
        });
        btnBack.setOnAction(e -> {
            if(indiceactual > 0 ) {
                indiceactual--;
            }
            LoadImage();
            resetZoom();
            relbl(lblPage);
            Logger.info("Now in: "+imagenes.get(indiceactual).getName());
        });
        btnReload.setOnAction(e -> {
            imagenes = chapter.reloadPages();
            for(int i=0;i < imagenes.size();i++) {
                Logger.info("Loaded image: "+ imagenes.get(i).getPath());
            }
            resetZoom();
            relbl(lblPage);
        });
        btnBackToMenu.setOnAction(e -> {
            nav.goTo(new ScnMangaMenu(nav,manga));
        });
        btnNextCh.setOnAction(e -> {
            
            if(this.chapternum < this.manga.getChapters().size() - 1){
                Chapter nextchapter = this.manga.getChapters().get(this.chapternum+1);
                if(nextchapter.hasPages()){
                    nav.goTo(new ScnReader(nav,this.manga,nextchapter,chapternum+1));
                    Logger.info("Now in: "+nextchapter.getChName());
                } else {
                    Logger.noPagesAlert(nextchapter);
                }
            } else{
                Logger.info(this.chapter.getChName()+" - This is the last chapter.");
            }
        });
        btnBackCh.setOnAction(e -> {
            if(this.chapternum > 0){
                Chapter lastchapter = this.manga.getChapters().get(this.chapternum-1);
                if(lastchapter.hasPages()){
                    nav.goTo(new ScnReader(nav,this.manga,lastchapter,chapternum-1));
                    Logger.info("Now in: "+lastchapter.getChName());
                } else {
                    Logger.noPagesAlert(lastchapter);
                }
            } else{
                Logger.info(this.chapter.getChName()+" - This is the first chapter.");
            }
        }); 
        
        //Panel de la izquierda
        VBox leftpanel = new VBox(10,btnBack,btnBackCh); 
        //Panel de la derecha
        VBox rightpanel = new VBox(10,btnNext,btnNextCh);
        //Panel del top
        HBox toppanel = new HBox(20,btnReload,lblPage,btnBackToMenu);
        //Desaparecer los botones
        btnBackCh.setDisable(chapternum == 0);
        btnNextCh.setDisable(chapternum >= manga.getChapters().size() - 1);
                       
        //Todo
        BorderPane layout = new BorderPane();
        
        layout.setCenter(scrollVisor);
        layout.setLeft(leftpanel);
        layout.setRight(rightpanel);
        leftpanel.setAlignment(Pos.CENTER);
        rightpanel.setAlignment(Pos.CENTER);
        layout.setTop(toppanel);
        toppanel.setAlignment(Pos.CENTER);
        
        return layout;
    }
    
    public void resetZoom() {
        if(visor != null) {
            visor.setScaleX(1.0);
            visor.setScaleY(1.0);
        }
    }
    
    @Override
    public String getName() {
        return manga.getTitle()+" - Chapter "+((int) chapternum+1);
    }
    
    
    public List<File> loadImages() {
        List<File> pages = chapter.getPages();
        if (pages.isEmpty()){
            Logger.warning("No hay paginas en: "+chapter.getChName());
            return new ArrayList<>();
        } else {
            return pages;
        }            
    }
    
    
    public void LoadImage() {
        File archivo = imagenes.get(indiceactual);
        Image img = new Image(archivo.toURI().toString());
        visor.setImage(img);
        Logger.info(archivo+" - Selected.");
    }
    
    void relbl(Label lblPage) {
        lblPage.setText((indiceactual+1)+"/"+imagenes.size());
    }
}
