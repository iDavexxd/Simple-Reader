package app.simplereader.views.components;

import java.util.List;
import javafx.scene.layout.TilePane;
import app.simplereader.repository.MangaInterface;

/**
 *
 * @author david
 */
public class CategoryPane{
    
    private TilePane categoryTilePane = new TilePane();
    private final String categoryName;
    private List<MangaInterface> mangas;
    
    public CategoryPane(String name, List<MangaInterface> mangas){
        this.categoryName = name;
        this.mangas = mangas;
    }
    
    public TilePane getPane(){
        return categoryTilePane;
    }
    
    
}
