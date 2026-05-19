package app.simplereader.controller;

import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import app.simplereader.repository.MangaSource;
import app.simplereader.views.ScnMangaMenu;
import app.simplereader.views.ScnReader;
import java.util.List;

/**
 *
 * @author david
 */
public class MangaMenuController {
    
    private final ScnMangaMenu view;
    private static MangaMenuController instance;
    private final SceneController nav = SceneController.getInstance();
    private final LibraryController library = LibraryController.getInstance();
    private final MainMenuController mainMenuController = MainMenuController.getInstance();
    
    private Manga manga;
    private boolean isReversed = false;
    
    public MangaMenuController(ScnMangaMenu view){
        this.view = view;
    }
    
    public static void doInstance(ScnMangaMenu view){
        instance = new MangaMenuController(view);
    }
    
    public static MangaMenuController getInstance(){
        return instance;
    }
    
    public void init(Manga manga){
        this.manga = manga;
    }
    
    public void addToLibrary() {
        SourceManager.getInstance().saveManga(this.manga);
        library.addManga(this.manga, "Default");
        library.saveLibrary();
        mainMenuController.reloadMangas();
    }
    
    public void openChapter(Chapter chapter){
        if (!chapter.hasPages()) {
            Logger.info("Cargando páginas para: " + chapter.getTitle());
            
            MangaSource src = SourceManager.getInstance().getSource(manga.getSourceID());
            if (src != null) {
                List<String> pages = src.getPages(manga.getMangaID(), chapter.getChapterID());
                chapter.setPages(pages);
            }
        }
        
        if (chapter.hasPages()) {
            List<Chapter> chapters = manga.getChapters();
            int index = chapters != null ? chapters.indexOf(chapter) : -1;
            Logger.info("Selected: " + chapter.getTitle());
            nav.goTo(new ScnReader(nav, manga, chapter, index));
        } else {
            Logger.noPagesAlert(chapter.getTitle());
        }
    }
    
    public Chapter findFirstUnreadChapter(){
        List<Chapter> chapters = manga.getChapters();
        if (chapters == null) return null;
        
        return chapters.stream()
                .filter(c -> !c.isReaded())
                .findFirst()
                .orElse(null);
    }
    
    public List<Chapter> getChapters(){
        List<Chapter> chapters = manga.getChapters();
        return chapters != null ? chapters : List.of();
    }
    
    public String getTags(){
        if (manga.getTags() == null || manga.getTags().isEmpty()) return "";
        return String.join(", ", manga.getTags());
    }
    
    public boolean isReversed(){
        return isReversed;
    }
    
    public void toggleChapterOrder(){
        isReversed = !isReversed;
    }
    
    public Manga getManga(){
        return manga;
    }
}
