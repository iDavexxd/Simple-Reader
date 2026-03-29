package app.simplereader;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.scene.control.Label;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;


/**
 *
 * @author iDavexX
 */
public class SimpleReader extends Application{
    private ImageView visor;
    private List<File> imagenes;
    private int indiceactual = 0;
    
    private final int Width = 1280;
    
    @Override
    public void start(Stage stage) {
        
        visor = new ImageView();
        visor.setFitHeight(400);
        visor.setFitWidth(Width);
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
        btnNext.setOnAction(e -> {
            int cantidad = imagenes.size();
            if (indiceactual < imagenes.size() - 1) { indiceactual++; }
            LoadImage();
            relbl(lblPage);
            Logger.Log("Cliqueaste siguiente!");
        });
        btnBack.setOnAction(e -> {
            if(indiceactual > 0 ) {
                indiceactual--;
            }
            LoadImage();
            relbl(lblPage);
            Logger.Log("Cliqueaste volver!");
        });
        btnReload.setOnAction(e -> {
            imagenes = loadImages();
            for(int i=0;i < imagenes.size();i++) {
                Logger.Log("Loaded image: "+ imagenes.get(i).getPath());
            }
            relbl(lblPage);
        });
        
        BorderPane layout = new BorderPane();
        layout.setCenter(visor);
        layout.setLeft(btnBack);
        layout.setRight(btnNext);
        // Crear el HBox con ambos elementos
        HBox bottom = new HBox(10, btnReload, lblPage); // 10 = espacio entre elementos
        bottom.setAlignment(Pos.CENTER); // centrar el contenido

        // Poner el HBox en el bottom (solo una vez)
        layout.setBottom(bottom);
        
        Scene escena = new Scene(layout,Width,720);
               
        stage.setTitle("Simple Reader");
        stage.setScene(escena);
        stage.setResizable(false);
        stage.show();
        stage.setOnCloseRequest(e -> Platform.exit());
    }
    
    public List<File> loadImages() {
        String home = System.getProperty("user.home");
        File carpeta = new File(home + "/Documents/SimpleReader/mangas");
        
        if(!carpeta.exists()) {
            carpeta.mkdirs();
            Logger.Log("Folder created: "+carpeta.getPath());
        }
        
        File[] archivos = carpeta.listFiles((dir, nombre) ->
            nombre.endsWith(".jpg") ||
            nombre.endsWith(".jpeg") ||
            nombre.endsWith(".png")
        );
        
        if (archivos == null || archivos.length == 0) {
            Logger.Log("No hay imágenes en la carpeta");
            return new ArrayList<>();
        }
        Arrays.sort(archivos);
        return new ArrayList<>(Arrays.asList(archivos));
    }
    
    public void LoadImage() {
        File archivo = imagenes.get(indiceactual);
        Image img = new Image(archivo.toURI().toString());
        visor.setImage(img);
        Logger.Log(archivo+" - Loaded.");
    }
    
    void relbl(Label lblPage) {
        lblPage.setText((indiceactual+1)+"/"+imagenes.size());
    }

    public static void main(String[] args) {
        launch(args);
        Logger.Log("XD");
    }
}
