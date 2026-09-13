package com.jarvis.homemultitool;

import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lightweight internet retrieval layer. No API key or third-party SDK is required. */
public final class WebSearchEngine {
    public interface Callback { void result(String text, String source); void state(String state); }
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void search(String query, Callback callback) {
        executor.execute(() -> {
            callback.state("ИЩУ В СЕТИ");
            String answer = wikipedia(query);
            if (answer != null) { callback.result(answer, "Wikipedia"); return; }
            answer = duckDuckGo(query);
            if (answer != null) { callback.result(answer, "Web Search"); return; }
            callback.result("Я не смог получить надёжный результат из интернета. Попробуйте сформулировать вопрос иначе.", "");
        });
    }

    private String wikipedia(String query) {
        HttpURLConnection c = null;
        try {
            String lang = "ru";
            String url = "https://" + lang + ".wikipedia.org/w/api.php?action=query&generator=search&gsrsearch="
                    + Uri.encode(query) + "&gsrlimit=3&prop=extracts&exintro=1&explaintext=1&format=json";
            c = open(url);
            JSONObject root = new JSONObject(read(c));
            JSONObject pages = root.optJSONObject("query");
            if (pages == null) return null;
            JSONObject map = pages.optJSONObject("pages");
            if (map == null) return null;
            JSONArray keys = map.names();
            if (keys == null || keys.length() == 0) return null;
            String best = null;
            for (int i=0; i<keys.length(); i++) {
                JSONObject page = map.optJSONObject(keys.optString(i));
                if (page == null) continue;
                String extract = page.optString("extract", "").trim();
                if (extract.length() > 80) { best = extract; break; }
            }
            if (best == null) return null;
            if (best.length() > 1100) best = best.substring(0, 1097) + "…";
            return best;
        } catch (Throwable ignored) { return null; }
        finally { if (c != null) c.disconnect(); }
    }

    private String duckDuckGo(String query) {
        HttpURLConnection c = null;
        try {
            String url = "https://api.duckduckgo.com/?q=" + Uri.encode(query)
                    + "&format=json&no_html=1&skip_disambig=0";
            c = open(url);
            JSONObject root = new JSONObject(read(c));
            String answer = root.optString("AbstractText", "").trim();
            if (answer.isEmpty()) answer = root.optString("Answer", "").trim();
            if (answer.isEmpty()) {
                JSONArray topics = root.optJSONArray("RelatedTopics");
                if (topics != null) {
                    for (int i=0; i<topics.length(); i++) {
                        JSONObject item = topics.optJSONObject(i);
                        if (item == null) continue;
                        String t = item.optString("Text", "").trim();
                        if (!t.isEmpty()) { answer = t; break; }
                    }
                }
            }
            if (answer.isEmpty()) return null;
            if (answer.length() > 1100) answer = answer.substring(0, 1097) + "…";
            return answer;
        } catch (Throwable ignored) { return null; }
        finally { if (c != null) c.disconnect(); }
    }

    private HttpURLConnection open(String address) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setConnectTimeout(4500); c.setReadTimeout(6500);
        c.setRequestMethod("GET"); c.setRequestProperty("User-Agent", "JARVIS/5.5 Android Assistant");
        c.setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.7");
        c.connect();
        if (c.getResponseCode() < 200 || c.getResponseCode() >= 300) throw new Exception("HTTP " + c.getResponseCode());
        return c;
    }

    private String read(HttpURLConnection c) throws Exception {
        try (InputStream in = c.getInputStream(); BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder b = new StringBuilder(); String line;
            while ((line = r.readLine()) != null) b.append(line);
            return b.toString();
        }
    }

    public void shutdown() { executor.shutdownNow(); }
}
