package app.simplereader.views;

import app.simplereader.repository.Manga;
import java.util.List;
import javafx.scene.layout.TilePane;

/**
 *
 * @author david
 */
public class CategoryPane{
    
    private TilePane categoryTilePane = new TilePane();
    private final String categoryName;
    private List<Manga> mangas;
    
    public CategoryPane(String name, List<Manga> mangas){
        this.categoryName = name;
        this.mangas = mangas;
    }
    
    public TilePane getPane(){
        return categoryTilePane;
    }
    
    
}
