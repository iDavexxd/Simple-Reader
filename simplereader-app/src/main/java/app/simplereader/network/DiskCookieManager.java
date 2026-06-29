package app.simplereader.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de cookies que guarda las sesiones de OkHttp en un archivo JSON local.
 */
public class DiskCookieManager implements CookieJar {

    private final File cookieDir;
    private final Map<String, List<Cookie>> cookieCache = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    public DiskCookieManager(File cookieDir) {
        this.cookieDir = cookieDir;
        if (!cookieDir.exists()) {
            cookieDir.mkdirs();
        }
        loadCookiesFromDirectory();
    }

    @Override
    public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        String host = url.host();
        List<Cookie> currentCookies = cookieCache.getOrDefault(host, new ArrayList<>());
        
        // Reemplazar cookies antiguas con el mismo nombre
        for (Cookie newCookie : cookies) {
            currentCookies.removeIf(c -> c.name().equals(newCookie.name()));
            currentCookies.add(newCookie);
        }
        
        cookieCache.put(host, currentCookies);
        saveCookiesForHost(host);
    }

    @Override
    public synchronized List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> validCookies = new ArrayList<>();
        List<Cookie> hostCookies = cookieCache.get(url.host());

        if (hostCookies != null) {
            long now = System.currentTimeMillis();
            Iterator<Cookie> iterator = hostCookies.iterator();
            boolean changed = false;

            while (iterator.hasNext()) {
                Cookie cookie = iterator.next();
                // Limpiar cookies expiradas automáticamente
                if (cookie.expiresAt() < now) {
                    iterator.remove();
                    changed = true;
                } else {
                    validCookies.add(cookie);
                }
            }
            if (changed) {
                saveCookiesForHost(url.host());
            }
        }
        return validCookies;
    }

    private void saveCookiesForHost(String host) {
        try {
            List<Cookie> cookies = cookieCache.get(host);
            if (cookies == null || cookies.isEmpty()) return;

            List<String> cookieStrings = new ArrayList<>();
            for (Cookie c : cookies) {
                cookieStrings.add(c.toString());
            }
            
            File hostFile = new File(cookieDir, host + ".json");
            String json = gson.toJson(cookieStrings);
            Files.writeString(hostFile.toPath(), json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadCookiesFromDirectory() {
        File[] files = cookieDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try {
                String host = file.getName().replace(".json", "");
                String json = Files.readString(file.toPath());
                List<String> cookieStrings = gson.fromJson(json, new TypeToken<List<String>>(){}.getType());
                
                if (cookieStrings != null) {
                    HttpUrl dummyUrl = new HttpUrl.Builder().scheme("https").host(host).build();
                    List<Cookie> cookies = new ArrayList<>();
                    
                    for (String cookieStr : cookieStrings) {
                        Cookie parsed = Cookie.parse(dummyUrl, cookieStr);
                        if (parsed != null) {
                            cookies.add(parsed);
                        }
                    }
                    cookieCache.put(host, cookies);
                }
            } catch (Exception e) {
                System.err.println("Error al cargar cookies de " + file.getName() + ": " + e.getMessage());
            }
        }
    }
}
