package com.jarvis.homemultitool;

import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** Live data/search layer. No fabricated weather, news or market values. */
public final class WebSearchEngine {
    public interface Callback { void result(String text,String source); void state(String state); }
    private final ExecutorService executor=Executors.newFixedThreadPool(2);

    public void currentWeather(final String city,final Callback cb){
        executor.execute(()->{
            cb.state("ПОЛУЧАЮ ПОГОДУ");
            try{
                Place p=geocode(city==null?"Москва":city);
                String url="https://api.open-meteo.com/v1/forecast?latitude="+p.lat+"&longitude="+p.lon+"&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,precipitation&timezone=auto";
                JSONObject cur=new JSONObject(read(open(url))).getJSONObject("current");
                StringBuilder out=new StringBuilder("Сейчас в ").append(p.name).append(": ").append(fmt(cur.optDouble("temperature_2m",Double.NaN))).append(" °C, ").append(weatherDescription(cur.optInt("weather_code",-1)));
                out.append(". Ощущается как ").append(fmt(cur.optDouble("apparent_temperature",Double.NaN))).append(" °C");
                out.append(". Влажность ").append(fmt(cur.optDouble("relative_humidity_2m",Double.NaN))).append("%.");
                out.append(" Ветер ").append(fmt(cur.optDouble("wind_speed_10m",Double.NaN))).append(" км/ч.");
                double rain=cur.optDouble("precipitation",0);if(rain>0)out.append(" Осадки сейчас: ").append(fmt(rain)).append(" мм.");
                cb.result(out.toString(),"Open-Meteo");
            }catch(Throwable e){cb.result("Не удалось получить текущую погоду. Проверьте интернет-соединение и название города.","");}
        });
    }

    public void forecastWeather(final String city,final int dayOffset,final Callback cb){
        executor.execute(()->{
            cb.state("ПОЛУЧАЮ ПРОГНОЗ");
            try{
                Place p=geocode(city==null?"Москва":city);
                String url="https://api.open-meteo.com/v1/forecast?latitude="+p.lat+"&longitude="+p.lon+"&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max&forecast_days="+Math.max(3,dayOffset+1)+"&timezone=auto";
                JSONObject d=new JSONObject(read(open(url))).getJSONObject("daily");
                int idx=Math.max(0,dayOffset);String date=d.getJSONArray("time").getString(idx);
                double max=d.getJSONArray("temperature_2m_max").getDouble(idx),min=d.getJSONArray("temperature_2m_min").getDouble(idx);
                int code=d.getJSONArray("weather_code").getInt(idx),pop=d.getJSONArray("precipitation_probability_max").getInt(idx);
                double wind=d.getJSONArray("wind_speed_10m_max").getDouble(idx);
                String day=idx==1?"завтра":idx==2?"послезавтра":date;
                cb.result("В "+p.name+" "+day+": от "+fmt(min)+" до "+fmt(max)+" °C, "+weatherDescription(code)+". Вероятность осадков "+pop+"%. Максимальный ветер около "+fmt(wind)+" км/ч.","Open-Meteo");
            }catch(Throwable e){cb.result("Не удалось получить прогноз погоды.","");}
        });
    }

    public void news(final Callback cb){
        executor.execute(()->{
            cb.state("ПОЛУЧАЮ НОВОСТИ");
            try{
                String xml=read(open("https://news.google.com/rss?hl=ru&gl=RU&ceid=RU:ru"));
                Matcher m=Pattern.compile("(?s)<item>.*?<title>(.*?)</title>.*?<pubDate>(.*?)</pubDate>.*?</item>").matcher(xml);
                StringBuilder out=new StringBuilder("Главные новости сейчас:\n");int n=0;
                while(m.find()&&n<6){String title=cleanXml(m.group(1));String date=cleanXml(m.group(2));if(title.isEmpty())continue;out.append("• ").append(title).append("\n");n++;}
                if(n==0)throw new Exception();cb.result(out.toString().trim(),"Google News RSS");
            }catch(Throwable e){cb.result("Не удалось получить актуальные новости. Проверьте интернет-соединение.","");}
        });
    }

    public void search(final String query,final Callback cb){
        executor.execute(()->{
            cb.state("ИЩУ В СЕТИ");
            String q=query==null?"":query.trim();
            if(isNews(q)){try{String xml=read(open("https://news.google.com/rss/search?q="+Uri.encode(q)+"&hl=ru&gl=RU&ceid=RU:ru"));String r=rss(xml,5);if(r!=null){cb.result(r,"Google News RSS");return;}}catch(Throwable ignored){}}
            if(isLive(q)){
                SearchResult live=duckDuckGoHtml(q);if(live!=null){cb.result(live.text,live.source);return;}
            }
            String answer=wikipedia(q);if(answer!=null){cb.result(answer,"Wikipedia");return;}
            SearchResult web=duckDuckGoHtml(q);if(web!=null){cb.result(web.text,web.source);return;}
            answer=duckDuckGoInstant(q);if(answer!=null){cb.result(answer,"DuckDuckGo");return;}
            cb.result("Не удалось получить ответ из интернета. Проверьте соединение или сформулируйте запрос точнее.","");
        });
    }

    private boolean isNews(String q){String x=q.toLowerCase(new Locale("ru"));return x.contains("новост")||x.contains("что произошло")||x.contains("событи");}
    private boolean isLive(String q){String x=q.toLowerCase(new Locale("ru"));return x.contains("сейчас")||x.contains("сегодня")||x.contains("курс")||x.contains("цена")||x.contains("котиров");}
    private String rss(String xml,int max){Matcher m=Pattern.compile("(?s)<item>.*?<title>(.*?)</title>.*?</item>").matcher(xml);StringBuilder b=new StringBuilder("Актуально сейчас:\n");int n=0;while(m.find()&&n<max){String t=cleanXml(m.group(1));if(t.isEmpty())continue;b.append("• ").append(t).append("\n");n++;}return n==0?null:b.toString().trim();}

    private Place geocode(String city)throws Exception{
        String u="https://geocoding-api.open-meteo.com/v1/search?name="+Uri.encode(city)+"&count=1&language=ru&format=json";
        JSONObject root=new JSONObject(read(open(u)));JSONArray a=root.optJSONArray("results");if(a==null||a.length()==0)throw new Exception("city");JSONObject p=a.getJSONObject(0);return new Place(p.getString("name"),p.getDouble("latitude"),p.getDouble("longitude"));
    }
    private String wikipedia(String query){HttpURLConnection c=null;try{String u="https://ru.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch="+Uri.encode(query)+"&gsrlimit=3&prop=extracts&exintro=1&explaintext=1&format=json";c=open(u);JSONObject root=new JSONObject(read(c));JSONObject pages=root.optJSONObject("query");if(pages==null)return null;JSONObject map=pages.optJSONObject("pages");if(map==null)return null;JSONArray keys=map.names();if(keys==null)return null;for(int i=0;i<keys.length();i++){JSONObject page=map.optJSONObject(keys.optString(i));if(page==null)continue;String e=clean(page.optString("extract",""));if(e.length()>100)return trim(e,1200);}}catch(Throwable ignored){}finally{if(c!=null)c.disconnect();}return null;}
    private SearchResult duckDuckGoHtml(String query){HttpURLConnection c=null;try{String u="https://html.duckduckgo.com/html/?q="+Uri.encode(query);c=open(u);String html=read(c);Pattern p=Pattern.compile("(?s)<a[^>]+class=\\\"result__a\\\"[^>]*>(.*?)</a>.*?<a[^>]+class=\\\"result__snippet\\\"[^>]*>(.*?)</a>");Matcher m=p.matcher(html);StringBuilder out=new StringBuilder();String first="";int n=0;while(m.find()&&n<3){String title=cleanHtml(m.group(1)),snip=cleanHtml(m.group(2));if(snip.length()<20)continue;if(out.length()>0)out.append("\n\n");out.append("• ").append(title).append("\n").append(snip);if(first.isEmpty()){Matcher h=Pattern.compile("href=\\\"([^\\\"]+)\\\"").matcher(m.group(0));if(h.find())first=h.group(1);}n++;}return out.length()==0?null:new SearchResult(trim(out.toString(),1500),first.isEmpty()?"DuckDuckGo Web":first);}catch(Throwable ignored){return null;}finally{if(c!=null)c.disconnect();}}
    private String duckDuckGoInstant(String query){HttpURLConnection c=null;try{String u="https://api.duckduckgo.com/?q="+Uri.encode(query)+"&format=json&no_html=1&skip_disambig=0";c=open(u);JSONObject root=new JSONObject(read(c));String a=clean(root.optString("AbstractText",""));if(a.isEmpty())a=clean(root.optString("Answer",""));if(a.isEmpty()){JSONArray t=root.optJSONArray("RelatedTopics");if(t!=null)for(int i=0;i<t.length();i++){JSONObject o=t.optJSONObject(i);if(o==null)continue;a=clean(o.optString("Text",""));if(!a.isEmpty())break;}}return a.isEmpty()?null:trim(a,1200);}catch(Throwable ignored){return null;}finally{if(c!=null)c.disconnect();}}
    private HttpURLConnection open(String address)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();c.setConnectTimeout(6000);c.setReadTimeout(9000);c.setRequestMethod("GET");c.setRequestProperty("User-Agent","Mozilla/5.0 (Android) JARVIS/5.16");c.setRequestProperty("Accept-Language","ru-RU,ru;q=0.9,en;q=0.7");c.connect();int code=c.getResponseCode();if(code<200||code>=300)throw new IOException("HTTP "+code);return c;}
    private String read(HttpURLConnection c)throws Exception{try(InputStream in=c.getInputStream();BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null)b.append(line);return b.toString();}}
    private String cleanHtml(String s){return clean(s.replaceAll("<[^>]+>"," ").replace("&amp;","&").replace("&quot;","\"").replace("&#x27;","'").replace("&lt;","<").replace("&gt;",">"));}
    private String cleanXml(String s){return clean(s.replaceAll("<!\\[CDATA\\[|\\]\\]>","").replace("&amp;","&").replace("&quot;","\"").replace("&apos;","'").replace("&lt;","<").replace("&gt;",">"));}
    private String clean(String s){return s==null?"":s.replaceAll("\\s+"," ").trim();}
    private String trim(String s,int n){return s.length()>n?s.substring(0,n-1)+"…":s;}
    private String fmt(double v){return Double.isNaN(v)?"—":String.format(Locale.US,"%.1f",v).replace('.',',');}
    private String weatherDescription(int c){if(c==0)return"ясно";if(c<=3)return"переменная облачность";if(c==45||c==48)return"туман";if(c>=51&&c<=57)return"морось";if(c>=61&&c<=67)return"дождь";if(c>=71&&c<=77)return"снег";if(c>=80&&c<=82)return"ливень";if(c==85||c==86)return"снегопад";if(c>=95&&c<=99)return"гроза";return"переменная погода";}
    public void shutdown(){executor.shutdownNow();}
    private static final class Place{final String name;final double lat,lon;Place(String n,double a,double b){name=n;lat=a;lon=b;}}
    private static final class SearchResult{final String text,source;SearchResult(String t,String s){text=t;source=s;}}
}
