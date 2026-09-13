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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Real internet retrieval without a bundled fake knowledge base or API key. */
public final class WebSearchEngine {
    public interface Callback { void result(String text, String source); void state(String state); }
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void search(final String query, final Callback callback) {
        executor.execute(() -> {
            callback.state("ИЩУ В СЕТИ");
            String answer = wikipedia(query);
            if (answer != null) { callback.result(answer, "Wikipedia"); return; }

            SearchResult web = duckDuckGoHtml(query);
            if (web != null) { callback.result(web.text, web.source); return; }

            answer = duckDuckGoInstant(query);
            if (answer != null) { callback.result(answer, "DuckDuckGo"); return; }

            callback.result("Не удалось получить ответ из интернета. Проверьте соединение или сформулируйте запрос точнее.", "");
        });
    }

    private String wikipedia(String query) {
        HttpURLConnection c = null;
        try {
            String url = "https://ru.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch="
                    + Uri.encode(query) + "&gsrlimit=3&prop=extracts&exintro=1&explaintext=1&format=json";
            c = open(url);
            JSONObject root = new JSONObject(read(c));
            JSONObject pages = root.optJSONObject("query");
            if (pages == null) return null;
            JSONObject map = pages.optJSONObject("pages");
            if (map == null) return null;
            JSONArray keys = map.names();
            if (keys == null || keys.length() == 0) return null;
            for (int i=0;i<keys.length();i++) {
                JSONObject page = map.optJSONObject(keys.optString(i));
                if (page == null) continue;
                String extract = clean(page.optString("extract", ""));
                if (extract.length() > 100) return trim(extract, 1200);
            }
        } catch (Throwable ignored) { }
        finally { if (c != null) c.disconnect(); }
        return null;
    }

    /** DDG HTML provides actual result pages/snippets, unlike the instant-answer endpoint. */
    private SearchResult duckDuckGoHtml(String query) {
        HttpURLConnection c = null;
        try {
            String url = "https://html.duckduckgo.com/html/?q=" + Uri.encode(query);
            c = open(url);
            String html = read(c);
            Pattern result = Pattern.compile("(?s)<a[^>]+class=\\\"result__a\\\"[^>]*>(.*?)</a>.*?<a[^>]+class=\\\"result__snippet\\\"[^>]*>(.*?)</a>");
            Matcher m = result.matcher(html);
            StringBuilder out = new StringBuilder();
            String firstUrl = "";
            int count = 0;
            while (m.find() && count < 3) {
                String title = cleanHtml(m.group(1));
                String snippet = cleanHtml(m.group(2));
                if (snippet.length() < 25) continue;
                if (out.length() > 0) out.append("\n\n");
                out.append("• ").append(title).append("\n").append(snippet);
                if (firstUrl.isEmpty()) {
                    Matcher href = Pattern.compile("href=\\\"([^\\\"]+)\\\"").matcher(m.group(0));
                    if (href.find()) firstUrl = href.group(1);
                }
                count++;
            }
            if (out.length() == 0) return null;
            return new SearchResult(trim(out.toString(), 1500), firstUrl.isEmpty() ? "DuckDuckGo Web" : firstUrl);
        } catch (Throwable ignored) { return null; }
        finally { if (c != null) c.disconnect(); }
    }

    private String duckDuckGoInstant(String query) {
        HttpURLConnection c = null;
        try {
            String url = "https://api.duckduckgo.com/?q=" + Uri.encode(query) + "&format=json&no_html=1&skip_disambig=0";
            c = open(url);
            JSONObject root = new JSONObject(read(c));
            String answer = clean(root.optString("AbstractText", ""));
            if (answer.isEmpty()) answer = clean(root.optString("Answer", ""));
            if (answer.isEmpty()) {
                JSONArray topics = root.optJSONArray("RelatedTopics");
                if (topics != null) for (int i=0;i<topics.length();i++) {
                    JSONObject item = topics.optJSONObject(i);
                    if (item == null) continue;
                    answer = clean(item.optString("Text", ""));
                    if (!answer.isEmpty()) break;
                }
            }
            return answer.isEmpty() ? null : trim(answer, 1200);
        } catch (Throwable ignored) { return null; }
        finally { if (c != null) c.disconnect(); }
    }

    private HttpURLConnection open(String address) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(address).openConnection();
        c.setConnectTimeout(6000); c.setReadTimeout(9000); c.setRequestMethod("GET");
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) JARVIS/5.7");
        c.setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.7"); c.connect();
        if (c.getResponseCode() < 200 || c.getResponseCode() >= 300) throw new Exception("HTTP " + c.getResponseCode());
        return c;
    }

    private String read(HttpURLConnection c) throws Exception {
        try (InputStream in=c.getInputStream(); BufferedReader r=new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder b=new StringBuilder(); String line; while((line=r.readLine())!=null)b.append(line); return b.toString();
        }
    }
    private String cleanHtml(String s){ return clean(s.replaceAll("<[^>]+>"," ").replace("&amp;","&").replace("&quot;","\"").replace("&#x27;","'").replace("&lt;","<").replace("&gt;",">")); }
    private String clean(String s){ return s==null?"":s.replaceAll("\\s+"," ").trim(); }
    private String trim(String s,int n){ return s.length()>n?s.substring(0,n-1)+"…":s; }
    private static final class SearchResult { final String text,source; SearchResult(String t,String s){text=t;source=s;} }
    public void shutdown(){ executor.shutdownNow(); }
}
