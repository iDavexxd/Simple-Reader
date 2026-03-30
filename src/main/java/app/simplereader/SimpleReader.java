package app.simplereader;


import app.simplereader.manga.Manga;
import app.simplereader.manga.MangaLoader;
import app.simplereader.scenes.ScnMainMenu;
import java.util.List;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;


/**
 *
 * @author iDavexX
 */
public class SimpleReader extends Application{
          
    
    @Override
    public void start(Stage stage) {
        List<Manga> mangas = MangaLoader.loadMangas();
        for (Manga manga : mangas) {
            Logger.info("Manga cargado: " + manga.getTitle());
            Logger.info("Capitulos: " + manga.getChapters().size());
        }
        
        Navegador nav = new Navegador(stage);
        nav.goTo(new ScnMainMenu(nav));
        
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> Platform.exit());
        stage.show();
    }
        

    public static void main(String[] args) {
        launch(args);
        Logger.info("XD");
    }


}
