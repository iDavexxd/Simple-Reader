import java.net.*;
import java.io.*;
import java.util.*;

public class TestHttp {
    public static void main(String[] args) throws Exception {
        String urlStr = "https://uploads.mangadex.org/covers/67e7453b-9ee5-4ae5-9316-215b03e4a71d/534baa34-19e6-45f1-93f2-e173476fe368.jpg";
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        conn.setRequestProperty("Referer", "https://mangadex.org/");
        
        System.out.println("Response Code: " + conn.getResponseCode());
    }
}
