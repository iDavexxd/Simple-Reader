package app.simplereader.manga.chapter;

import app.simplereader.Logger;
import app.simplereader.interfaces.Chapter;
import app.simplereader.interfaces.Manga;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.image.Image;

/**
 *
 * @author david
 */
public class mdChapter implements Chapter{
    
    private final String chapterID;
    private final List<String> pageUrls = new ArrayList<>();
    private final int Number;
    private final String name;
    private final Manga manga;
    private int lastRead;
    public mdChapter(Manga manga,String chapterID, String name, int Number){
        this.chapterID = chapterID;
        this.name = name;
        this.Number = Number;
        this.manga = manga;
    }
    
    private void loadChapterData(){
        try {
            // Cliente http
            HttpClient client = HttpClient.newHttpClient();
            
            // Request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mangadex.org/at-home/server/" + chapterID))
                .GET()
                .build();
            
            // Enviar request y obtener respuesta
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Convertir el json a objeto
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            
            // Leer datos
            String baseUrl = json.get("baseUrl").getAsString();

            JsonObject chapter = json.getAsJsonObject("chapter");
            String hash = chapter.get("hash").getAsString();

            JsonArray data = chapter.getAsJsonArray("data");
            
            // 6. Construir URLs
            for (int i = 0; i < data.size(); i++) {
                String fileName = data.get(i).getAsString();
                String url = baseUrl + "/data/" + hash + "/" + fileName;
                pageUrls.add(url);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private boolean loaded = false;
    
    private synchronized void ensureLoaded() {
        if (!loaded) {
            loadChapterData();
            loaded = true;
        }
    }
    
    @Override
    public int getPageCount() {
        ensureLoaded();
        return pageUrls.size();
    }

    @Override
    public Image getPage(int index) {
        ensureLoaded();
        if (index < 0 || index >= pageUrls.size()) return null;

        String url = pageUrls.get(index);
        return new Image(url, true);
    }

    @Override
    public boolean hasPages() {
        ensureLoaded();
        return !pageUrls.isEmpty();
    }

    @Override
    public String getName() {
        if (name == null || name.isBlank()) {
            return "Cap." + Number;
        }
        return "Cap." + Number + " - " + name;
    }

    @Override
    public int getNum() {
        return Number;
    }
    
    @Override
    public boolean isReaded(){
        return manga.getReadedChapters().contains(this.name);
    }
    
    @Override
    public void markAsReaded(){
        if(!isReaded()){
            manga.getReadedChapters().add(this.name);
            Logger.info(this.getName()+" - Leido.");
        }
    }
    
    @Override
    public Integer getLastRead(){
        return lastRead;
    }
    
    @Override
    public void setLastRead(int s){
        this.lastRead = s;
        this.manga.getChapterLastPage().put(this.getName(), lastRead);
    }
    
}
