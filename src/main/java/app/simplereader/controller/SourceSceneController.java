package app.simplereader.controller;

import app.simplereader.repository.MangaSource;
import app.simplereader.views.ScnSource;

/**
 *
 * @author david
 */
public class SourceSceneController {
    
    private ScnSource scene;
    private MangaSource source;
    public SourceSceneController(ScnSource scene,MangaSource source){
        this.scene = scene;
        this.source = source;
    }
    
    
    public void searchManga(){
        
    }
}
