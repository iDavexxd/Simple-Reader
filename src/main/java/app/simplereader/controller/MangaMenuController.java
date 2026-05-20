package app.simplereader.controller;

import app.simplereader.model.Category;
import app.simplereader.model.Chapter;
import app.simplereader.model.Manga;
import app.simplereader.repository.MangaSource;
import app.simplereader.views.ScnMangaMenu;
import app.simplereader.views.ScnReader;
import java.util.ArrayList;
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
    
    private final SourceManager source = SourceManager.getInstance();
    
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
    
    public void addToLibrary(String category) {
        SourceManager.getInstance().saveManga(this.manga);
        library.addManga(this.manga, category);
        library.saveLibrary();
        mainMenuController.reloadMangas();
    }
    
    public void removeManga(Manga manga, Category category){
        library.removeMangaFrom(category, manga);
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
    
    
    
    public void reloadManga(){
        // 1. Respaldamos las INSTANCIAS VIEJAS
        List<Chapter> oldChapters = this.manga.getChapters();

        source.fetchMangaData(this.manga);

        // 2. Traemos las INSTANCIAS NUEVAS
        MangaSource src = source.getSource(manga.getSourceID());
        List<Chapter> newChapters = src.getChapters(this.manga.getMangaID());
        for(Chapter chapter : newChapters){
            Logger.info("New chapter: "+chapter.getTitle());
        }
        
        // 3. Creamos una lista para meter los objetos definitivos
        List<Chapter> finalChapters = new ArrayList<>();

        if (newChapters != null) {
            for (Chapter newCap : newChapters) {
                Chapter matchingOldCap = null;

                // Buscamos manualmente si el capítulo ya existía usando el ChapterID
                if (oldChapters != null) {
                    for (Chapter oldCap : oldChapters) {
                        if (oldCap.getChapterID().equals(newCap.getChapterID())) {
                            matchingOldCap = oldCap;
                            break;
                        }
                    }
                }

                if (matchingOldCap != null) {
                    // Conservamos la INSTANCIA VIEJA. 
                    // Su estado interno (como la última página leída) se mantiene intacto.
                    finalChapters.add(matchingOldCap);
                    Logger.info("Reload chapter: "+matchingOldCap.getTitle());
                } else {
                    // Si no hubo coincidencia, es un capítulo recién salido.
                    finalChapters.add(newCap);
                    Logger.info(("Loaded NEW chapter: " + newCap.getTitle()));
                }
            }
        }

        // 4. Guardamos los capítulos actualizados en el manga
        manga.setChapters(finalChapters);
        source.saveManga(this.manga);

        // 5. IMPORTANTE: Guardar en la biblioteca para asegurar la persistencia
        if (library.onLibrary(this.manga)) {
            library.saveLibrary();
        }

        // 6. Refrescar la vista con los nuevos metadatos y la lista fusionada
        view.setTitle(manga.getTitle());
        view.setAuthor(manga.getAuthor());
        view.setDescrition(manga.getDescription());
        view.setTags(getTags());
        view.doReloadChapters();
    }
    
}
