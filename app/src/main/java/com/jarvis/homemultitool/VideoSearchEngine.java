package com.jarvis.homemultitool;

import android.net.Uri;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** Finds a requested movie/series across public web sources without pretending that one catalog contains everything. */
public final class VideoSearchEngine {
    public interface Callback { void result(String text); void state(String state); }
    private final java.util.concurrent.ExecutorService ex=java.util.concurrent.Executors.newSingleThreadExecutor();
    public void search(String title,Callback cb){
        final String q=title==null?"":title.trim();
        ex.execute(()->{
            cb.state("ИЩУ ВИДЕО");
            try{
                String html=read(open("https://html.duckduckgo.com/html/?q="+Uri.encode(q+" смотреть онлайн фильм сериал")));
                Pattern p=Pattern.compile("(?s)<a[^>]+class=\\\"result__a\\\"[^>]*href=\\\"([^\\\"]+)\\\"[^>]*>(.*?)</a>.*?<a[^>]+class=\\\"result__snippet\\\"[^>]*>(.*?)</a>");
                Matcher m=p.matcher(html);StringBuilder out=new StringBuilder("Нашёл варианты для «").append(q).append("»:\n\n");
                Set<String> domains=new LinkedHashSet<>();int n=0;
                while(m.find()&&n<8){String url=decode(m.group(1));String titleText=clean(m.group(2));String snip=clean(m.group(3));String domain=domain(url);if(domain.isEmpty())continue;if(!domains.add(domain))continue;n++;out.append(n).append(". ").append(domain).append("\n   ").append(titleText);if(!snip.isEmpty())out.append(" — ").append(snip);out.append("\n   ").append(url).append("\n\n");}
                if(n==0)cb.result("Не нашёл надёжных источников по этому названию. Уточните фильм, сезон или год."); else cb.result(out.toString().trim());
            }catch(Throwable e){cb.result("Не удалось выполнить поиск видео. Проверьте интернет-соединение.");}
        });
    }
    private HttpURLConnection open(String u)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(6000);c.setReadTimeout(9000);c.setRequestProperty("User-Agent","Mozilla/5.0 (Android) JARVIS");c.connect();if(c.getResponseCode()<200||c.getResponseCode()>=300)throw new IOException();return c;}
    private String read(HttpURLConnection c)throws Exception{try(InputStream in=c.getInputStream();BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){StringBuilder b=new StringBuilder();String l;while((l=r.readLine())!=null)b.append(l);return b.toString();}}
    private String decode(String s){try{return URLDecoder.decode(s,"UTF-8");}catch(Exception e){return s;}}
    private String clean(String s){return s==null?"":s.replaceAll("<[^>]+>"," ").replace("&amp;","&").replace("&quot;","\"").replace("&#x27;","'").replaceAll("\\s+"," ").trim();}
    private String domain(String u){try{return new URL(u).getHost().replaceFirst("^www\\.","");}catch(Exception e){return "";}}
    public void shutdown(){ex.shutdownNow();}
}
