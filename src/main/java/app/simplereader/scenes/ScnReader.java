package app.simplereader.scenes;

import app.simplereader.AppConfig;
import app.simplereader.Logger;
import app.simplereader.Navegador;
import app.simplereader.interfaces.Navigable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

/**
 *
 * @author david
 */
public class ScnReader implements Navigable{
    
    private final Navegador nav;        
    
    public ScnReader(Navegador nav){
        this.nav = nav;
    }
    
    private ImageView visor;
    private List<File> imagenes;
    private int indiceactual = 0;
    
    @Override
    public Scene getScene(){
        visor = new ImageView();
        visor.setFitHeight(AppConfig.HEIGHT);
        visor.setFitWidth(AppConfig.WIDTH);
        visor.setPreserveRatio(true);
        
        imagenes = loadImages();
        
        Label lblPage = new Label("0/0");
        
        if (!imagenes.isEmpty()) {
            LoadImage();
            relbl(lblPage);
        }
        
        
        Button btnNext = new Button("->");
        Button btnBack = new Button("<-");
        Button btnReload = new Button("XD");
        Button btnBackToMenu = new Button("Menu");
        btnNext.setOnAction(e -> {
            if (indiceactual < imagenes.size() - 1) { indiceactual++; }
            LoadImage();
            relbl(lblPage);
            Logger.info("Cliqueaste siguiente!");
        });
        btnBack.setOnAction(e -> {
            if(indiceactual > 0 ) {
                indiceactual--;
            }
            LoadImage();
            relbl(lblPage);
            Logger.info("Cliqueaste volver!");
        });
        btnReload.setOnAction(e -> {
            imagenes = loadImages();
            for(int i=0;i < imagenes.size();i++) {
                Logger.info("Loaded image: "+ imagenes.get(i).getPath());
            }
            relbl(lblPage);
        });
        btnBackToMenu.setOnAction(e -> {
            nav.goTo(new ScnMainMenu(nav));
        });
        
        BorderPane layout = new BorderPane();
        layout.setCenter(visor);
        layout.setLeft(btnBack);
        layout.setRight(btnNext);
        // Crear el HBox con ambos elementos
        HBox bottom = new HBox(20, btnReload, lblPage,btnBackToMenu); // 10 = espacio entre elementos
        bottom.setAlignment(Pos.CENTER); // centrar el contenido

        // Poner el HBox en el bottom (solo una vez)
        layout.setBottom(bottom);
        
        return new Scene(layout,AppConfig.WIDTH,AppConfig.HEIGHT);
    }
    
    @Override
    public String getName() {
        return "Reader";
    }
    
    
    public List<File> loadImages() {
        String home = System.getProperty("user.home");
        File carpeta = new File(home + "/Documents/SimpleReader/mangas");
        
        if(!carpeta.exists()) {
            carpeta.mkdirs();
            Logger.info("Folder created: "+carpeta.getPath());
        }
        
        File[] archivos = carpeta.listFiles((dir, nombre) ->
            nombre.endsWith(".jpg") ||
            nombre.endsWith(".jpeg") ||
            nombre.endsWith(".png")
        );
        
        if (archivos == null || archivos.length == 0) {
            Logger.info("No hay imágenes en la carpeta");
            return new ArrayList<>();
        }
        Arrays.sort(archivos);
        return new ArrayList<>(Arrays.asList(archivos));
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
