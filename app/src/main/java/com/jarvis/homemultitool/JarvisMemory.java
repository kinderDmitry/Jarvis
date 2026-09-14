package com.jarvis.homemultitool;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/**
 * Local adaptive memory. It never claims to retrain a language model: it learns
 * user-specific aliases, preferences, successful commands and recent dialogue.
 */
public final class JarvisMemory {
    private static final String PREF = "jarvis_memory";
    private final SharedPreferences p;
    public JarvisMemory(Context c){ p=c.getApplicationContext().getSharedPreferences(PREF,Context.MODE_PRIVATE); }

    public void remember(String key,String value){ if(key==null||value==null||key.trim().isEmpty()) return; p.edit().putString(key.trim(),value.trim()).apply(); }
    public String get(String key,String def){ return p.getString(key,def); }

    public synchronized void learnAlias(String phrase,String action){
        if(phrase==null||action==null) return;
        String k=normalize(phrase); if(k.isEmpty()||action.trim().isEmpty()) return;
        try{
            JSONObject o=new JSONObject(p.getString("aliases","{}"));
            JSONObject item=o.optJSONObject(k);
            if(item==null) item=new JSONObject();
            item.put("action",action.trim());
            item.put("uses",item.optInt("uses",0));
            item.put("updated",System.currentTimeMillis());
            o.put(k,item);
            p.edit().putString("aliases",o.toString()).apply();
        }catch(Throwable ignored){}
    }

    /** Exact or high-confidence token-overlap lookup for learned commands. */
    public synchronized String alias(String phrase){
        String k=normalize(phrase);
        try{
            JSONObject o=new JSONObject(p.getString("aliases","{}"));
            JSONObject exact=o.optJSONObject(k);
            if(exact!=null){ recordAliasUse(k,o,exact); return exact.optString("action",""); }
            String bestKey=""; double best=0;
            org.json.JSONArray names=o.names();
            if(names!=null) for(int idx=0;idx<names.length();idx++){ String key=names.optString(idx);
                JSONObject item=o.optJSONObject(key); if(item==null)continue;
                double s=similarity(k,key);
                if(s>best){best=s;bestKey=key;}
            }
            if(best>=0.78){JSONObject item=o.optJSONObject(bestKey);String action=item==null?"":item.optString("action","");if(!action.isEmpty()){recordAliasUse(bestKey,o,item);return action;}}
        }catch(Throwable ignored){}
        return "";
    }

    private void recordAliasUse(String key,JSONObject root,JSONObject item){
        try{item.put("uses",item.optInt("uses",0)+1);root.put(key,item);p.edit().putString("aliases",root.toString()).apply();}catch(Throwable ignored){}
    }

    private static Iterable<String> jsonKeys(JSONObject o){
        ArrayList<String> keys=new ArrayList<>();
        JSONArray n=o.names();
        if(n!=null) for(int i=0;i<n.length();i++) keys.add(n.optString(i));
        return keys;
    }

    private double similarity(String a,String b){
        Set<String> x=new HashSet<>(Arrays.asList(a.split(" "))); Set<String> y=new HashSet<>(Arrays.asList(b.split(" ")));
        x.remove(""); y.remove(""); if(x.isEmpty()||y.isEmpty())return 0;
        Set<String> i=new HashSet<>(x); i.retainAll(y);
        double overlap=(2.0*i.size())/(x.size()+y.size());
        if(a.contains(b)||b.contains(a)) overlap=Math.max(overlap,0.82);
        return overlap;
    }

    public synchronized void addTurn(String user,String assistant){
        try{
            JSONArray a=new JSONArray(p.getString("turns","[]"));
            JSONObject x=new JSONObject();x.put("u",user==null?"":user);x.put("a",assistant==null?"":assistant);x.put("t",System.currentTimeMillis());a.put(x);
            while(a.length()>40)a.remove(0);
            p.edit().putString("turns",a.toString()).apply();
        }catch(Throwable ignored){}
    }

    public String recentContext(){
        try{
            JSONArray a=new JSONArray(p.getString("turns","[]"));
            StringBuilder b=new StringBuilder();
            for(int i=Math.max(0,a.length()-8);i<a.length();i++){
                JSONObject x=a.getJSONObject(i); b.append("Пользователь: ").append(x.optString("u")).append("\n");
                b.append("JARVIS: ").append(x.optString("a")).append("\n");
            }
            return b.toString().trim();
        }catch(Throwable e){return "";}
    }

    public int learnedCount(){
        try{return new JSONObject(p.getString("aliases","{}")).length();}catch(Throwable e){return 0;}
    }
    public void clear(){p.edit().clear().apply();}
    private String normalize(String s){return s==null?"":s.toLowerCase(new Locale("ru")).replace('ё','е').replaceAll("[^а-яa-z0-9 ]"," ").replaceAll("\\s+"," ").trim();}
}
