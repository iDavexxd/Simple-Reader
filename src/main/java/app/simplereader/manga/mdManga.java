package app.simplereader.manga;

import app.simplereader.Sorter;
import app.simplereader.interfaces.Chapter;
import app.simplereader.interfaces.Manga;
import app.simplereader.manga.chapter.mdChapter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author david
 */
public class mdManga implements Manga{
    
    private final String mangaID;
    private String title;
    private String author;
    private String description;
    private String coverURL;
    private final List<String> tags = new ArrayList<>();
    private final List<Chapter> chapters = new ArrayList<>();
    private final Set<String> readedChapters = new HashSet<>();
    private final HashMap<String, Integer> lastChapterPage = new HashMap<>();
    
    public mdManga(String mangaID){
        this.mangaID = mangaID;
        loadMangaData();
        loadMangaMeta();
    }
    
    
    private void loadMangaData(){
    try {
        HttpClient client = HttpClient.newHttpClient();
        int offset = 0;
        int total;

        do {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://api.mangadex.org/manga/" 
                            + mangaID 
                            + "/feed?limit=100&offset=" + offset
                            + "&translatedLanguage[]=es&translatedLanguage[]=es-la"
                    ))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            total = json.get("total").getAsInt();
            JsonArray data = json.getAsJsonArray("data");

            for (JsonElement el : data) {
                JsonObject chapterObj = el.getAsJsonObject();
                String chapterID = chapterObj.get("id").getAsString();
                JsonObject attributes = chapterObj.getAsJsonObject("attributes");

                int number = 0;
                if (attributes.has("chapter") && !attributes.get("chapter").isJsonNull()) {
                    try {
                        number = (int) Double.parseDouble(attributes.get("chapter").getAsString());
                    } catch (Exception e) {
                        number = 0;
                    }
                }

                String name = "";
                if (attributes.has("title") && !attributes.get("title").isJsonNull()) {
                    name = attributes.get("title").getAsString();
                }

                chapters.add(createChapter(chapterID, name, number));
            }

            offset += 100;
        } while (offset < total);

        // el sort va aquí, después del do-while
        chapters.sort((a, b) -> {
                String n1Str = a.getName() != null ? a.getName() : "";
                String n2Str = b.getName() != null ? b.getName() : "";

                double n1 = a.getNum();
                double n2 = b.getNum();

                boolean valid1 = n1 >= 0;
                boolean valid2 = n2 >= 0;

                // Ambos tienen número
                if (valid1 && valid2) {
                    int cmp = Double.compare(n1, n2);
                    if (cmp != 0) return cmp;

                    // mismo número → fallback (1a, 1b)
                    return Sorter.compare(n1Str, n2Str);
                }

                // Solo uno tiene número
                if (valid1) return -1;
                if (valid2) return 1;

                // Ninguno → string
                return Sorter.compare(n1Str, n2Str);
            });     

    } catch (Exception e) {
        e.printStackTrace();
    }
}
      
    private void loadMangaMeta(){
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                    "https://api.mangadex.org/manga/" 
                    + mangaID 
                    + "?includes[]=author&includes[]=cover_art"
                    ))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            JsonObject data = json.getAsJsonObject("data");
            JsonObject attributes = data.getAsJsonObject("attributes");

            //Título
            JsonObject titleObj = attributes.getAsJsonObject("title");
            this.title = titleObj.has("en")
                    ? titleObj.get("en").getAsString()
                    : titleObj.entrySet().iterator().next().getValue().getAsString();

            // Descripción
            JsonObject descObj = attributes.getAsJsonObject("description");
            if (descObj.has("en")) {
                this.description = descObj.get("en").getAsString();
            }

            //Tags
            JsonArray tagsArray = attributes.getAsJsonArray("tags");
            for (JsonElement tagEl : tagsArray) {
                JsonObject tagAttr = tagEl.getAsJsonObject()
                                         .getAsJsonObject("attributes");
                JsonObject nameObj = tagAttr.getAsJsonObject("name");

                if (nameObj.has("en")) {
                    tags.add(nameObj.get("en").getAsString());
                }
            }

            //Relationships (autor + cover)
            JsonArray relationships = data.getAsJsonArray("relationships");

            for (JsonElement relEl : relationships) {
                JsonObject rel = relEl.getAsJsonObject();
                String type = rel.get("type").getAsString();

                if (type.equals("author")) {
                    this.author = rel.getAsJsonObject("attributes")
                                     .get("name").getAsString();
                }

                if (type.equals("cover_art")) {
                    String fileName = rel.getAsJsonObject("attributes")
                                         .get("fileName").getAsString();

                    this.coverURL = "https://uploads.mangadex.org/covers/"
                            + mangaID + "/" + fileName;
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private Chapter createChapter(String chapterID, String name, int Number){
        return new mdChapter(this,chapterID, name, Number);
    }
    
    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getAuthor() {
        return author;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public List<String> getTags() {
        return tags;
    }

    @Override
    public List<Chapter> getChapters() {
        return chapters;
    }

    @Override
    public String getCover() {
        return coverURL;
    }
    
    @Override
    public void saveData(){
        // Guardar data en json
    }
    
    @Override
    public Set<String> getReadedChapters(){
        return readedChapters;
    }
    
    @Override
    public HashMap<String, Integer> getChapterLastPage(){
        return lastChapterPage;
    }
    
}
