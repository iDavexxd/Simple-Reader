package app.simplereader.controller;

import app.simplereader.model.AppConfig;
import app.simplereader.repository.MangaSource;
import app.simplereader.views.ScnSourceMenu;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 *
 * @author david
 */
public class SourceMenuController {
    private static SourceMenuController instance;
    private final SourceManager manager = SourceManager.getInstance();
    private final ScnSourceMenu scene;
    
    public SourceMenuController(ScnSourceMenu scene){
        this.scene = scene;
    }
    
    public static void doInstance(ScnSourceMenu scene){
        instance = new SourceMenuController(scene);
    }
    
    public static SourceMenuController getInstance(){
        return instance;
    }
    
    public List<MangaSource> getAllSources(){
        return manager.getAllSources();
    }
    
    public void importPlugin(Window ownerWindow) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Plugin (.jar)");
        
        // Filtro para que solo muestre archivos .jar
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Java Archive (*.jar)", "*.jar")
        );

        // Abre la ventana y espera a que el usuario seleccione un archivo
        File selectedFile = fileChooser.showOpenDialog(ownerWindow);

        if (selectedFile != null) {
            try {
                // Carpeta destino (puedes cambiar "plugins" por la ruta que necesites)
                File destDir = new File(AppConfig.PLUGIN_FOLDER);
                
                // Crea la carpeta si no existe
                if (!destDir.exists()) {
                    destDir.mkdirs(); 
                }

                Path sourcePath = selectedFile.toPath();
                Path destPath = new File(destDir, selectedFile.getName()).toPath();

                // COPIAR el archivo a la nueva carpeta reemplazando si ya existe.
                // Nota: Usamos copy en lugar de move para no borrarle el archivo original al usuario.
                // Si quieres MOVERLO estrictamente, cambia "Files.copy" por "Files.move"
                Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);

                System.out.println("Plugin importado exitosamente a: " + destPath.toAbsolutePath());
                
                // Opcional: Aquí podrías llamar a un método para recargar la lista de sources.
                SourceManager.getInstance().reloadSources();
                refreshList();
            } catch (IOException e) {
                Logger.error("Error: "+e.getMessage());
            }
        }
    }
    
    public void refreshList() {
        if (scene.getListView() != null) {
            List<MangaSource> sources = getAllSources();
            if (sources != null) {
                ObservableList<MangaSource> observableSources = FXCollections.observableArrayList(sources);
                scene.getListView().setItems(observableSources);
            }
        }
    }
    
}
