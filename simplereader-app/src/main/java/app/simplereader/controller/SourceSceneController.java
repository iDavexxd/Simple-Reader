package app.simplereader.controller;

import app.simplereader.repository.MangaSource;
import app.simplereader.views.ScnSourceSearch;

/**
 *
 * @author david
 */
public class SourceSceneController {
    
    private ScnSourceSearch scene;
    private MangaSource source;
    public SourceSceneController(ScnSourceSearch scene,MangaSource source){
        this.scene = scene;
        this.source = source;
    }
    
    
    public void searchManga(){
        
    }
}
