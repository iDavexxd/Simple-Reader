package app.simplereader.controller;

import app.simplereader.model.mdManga;
import app.simplereader.model.LocalManga;
import app.simplereader.model.Category;
import app.simplereader.controller.CategoryController;
import app.simplereader.controller.Logger;
import app.simplereader.model.LocalSource;
import app.simplereader.model.MangadexSource;
import app.simplereader.views.ScnMainMenu;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import app.simplereader.repository.MangaInterface;


/**
 *
 * @author david
 */
public class MangaController {
    
    private static ScnMainMenu mainMenu;
    private static CategoryController manager;
    
    public MangaController(ScnMainMenu mainMenu,CategoryController manager){
        this.mainMenu = mainMenu;
        this.manager = manager;        
    }
    
    public static List<MangaInterface> loadMangas() {
        List<MangaInterface> lista = new ArrayList<>();
        //lista.addAll(loadLocalMangas());
        LocalSource local = new LocalSource(manager);
        lista.addAll(local.loadMangas());
        MangadexSource md = new MangadexSource(manager);
        lista.addAll(md.loadMangas());
        return lista;
    }

    public static void saveMangas(){
        
    }

}
