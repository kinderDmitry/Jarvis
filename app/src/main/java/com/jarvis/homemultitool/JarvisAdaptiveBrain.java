package com.jarvis.homemultitool;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/**
 * Adaptive semantic layer for JARVIS.
 *
 * It is intentionally small and dependency-free: it does not pretend to retrain a
 * foundation model. Instead it builds a persistent user-specific semantic model:
 * aliases, successful/failed interpretations, preferences, and cached knowledge.
 * The model is updated from real interactions and can use fresh web answers through
 * JarvisEngine. It never executes instructions found on the web.
 */
public final class JarvisAdaptiveBrain {
    private static final String PREF = "jarvis_adaptive_brain";
    private static final int MAX_KNOWLEDGE = 80;
    private static final int MAX_PATTERNS = 180;
    private final SharedPreferences p;

    public JarvisAdaptiveBrain(Context c) {
        p = c.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static final class Match {
        public final String intent;
        public final double confidence;
        public final boolean learned;
        Match(String i, double c, boolean l) { intent=i; confidence=c; learned=l; }
    }

    /** Learns a paraphrase only after a real action has completed successfully. */
    public synchronized void learn(String phrase, String intent) {
        String k = normalize(phrase);
        if (k.length() < 4 || k.length() > 180 || intent == null || intent.trim().isEmpty()) return;
        try {
            JSONObject root = new JSONObject(p.getString("patterns", "{}"));
            JSONObject item = root.optJSONObject(k);
            if (item == null) item = new JSONObject();
            item.put("intent", intent.trim());
            item.put("success", item.optInt("success", 0) + 1);
            item.put("fail", item.optInt("fail", 0));
            item.put("updated", System.currentTimeMillis());
            root.put(k, item);
            trim(root, MAX_PATTERNS);
            p.edit().putString("patterns", root.toString()).apply();
        } catch (Throwable ignored) { }
    }

    /** Negative feedback weakens the interpretation instead of reinforcing it. */
    public synchronized void reject(String phrase) {
        String k = normalize(phrase);
        if (k.isEmpty()) return;
        try {
            JSONObject root = new JSONObject(p.getString("patterns", "{}"));
            JSONObject item = root.optJSONObject(k);
            if (item != null) {
                item.put("fail", item.optInt("fail", 0) + 1);
                item.put("updated", System.currentTimeMillis());
                root.put(k, item);
                p.edit().putString("patterns", root.toString()).apply();
            }
        } catch (Throwable ignored) { }
    }

    public synchronized Match predict(String phrase) {
        String q = normalize(phrase);
        if (q.isEmpty()) return new Match("", 0, false);
        try {
            JSONObject root = new JSONObject(p.getString("patterns", "{}"));
            JSONArray names = root.names();
            if (names == null) return new Match("", 0, false);
            String bestIntent=""; double best=0;
            for (int i=0;i<names.length();i++) {
                String key=names.optString(i);
                JSONObject item=root.optJSONObject(key);
                if(item==null) continue;
                double score=similarity(q,key);
                int ok=item.optInt("success",0), bad=item.optInt("fail",0);
                score += Math.min(0.10, ok*0.008);
                score -= Math.min(0.18, bad*0.018);
                if(score>best){best=score;bestIntent=item.optString("intent","");}
            }
            return new Match(bestIntent, Math.max(0, Math.min(0.99,best)), true);
        } catch(Throwable e) { return new Match("",0,false); }
    }

    /** Stores a successful web answer so repeated questions can be answered quickly. */
    public synchronized void cacheKnowledge(String query, String answer, String source) {
        String q=normalize(query), a=answer==null?"":answer.trim();
        if(q.length()<4 || a.length()<10) return;
        try {
            JSONObject root=new JSONObject(p.getString("knowledge","{}"));
            JSONObject item=new JSONObject();
            item.put("answer",a); item.put("source",source==null?"":source); item.put("time",System.currentTimeMillis());
            root.put(q,item); trim(root,MAX_KNOWLEDGE);
            p.edit().putString("knowledge",root.toString()).apply();
        } catch(Throwable ignored) { }
    }

    /** Returns a fresh enough cached answer for a semantically similar question. */
    public synchronized String cachedKnowledge(String query, boolean volatileInfo) {
        String q=normalize(query); if(q.isEmpty()) return "";
        long ttl=(volatileInfo?6L*60*60:30L*24*60*60)*1000L, now=System.currentTimeMillis();
        try {
            JSONObject root=new JSONObject(p.getString("knowledge","{}")); JSONArray names=root.names();
            if(names==null)return ""; String best=""; double score=0;
            for(int i=0;i<names.length();i++){
                String key=names.optString(i); JSONObject item=root.optJSONObject(key); if(item==null)continue;
                if(now-item.optLong("time",0)>ttl)continue;
                double s=similarity(q,key); if(s>score){score=s;best=item.optString("answer","");}
            }
            return score>=0.86?best:"";
        }catch(Throwable e){return "";}
    }

    public int learnedCount() {
        try { return new JSONObject(p.getString("patterns","{}")).length(); } catch(Throwable e){ return 0; }
    }
    public int knowledgeCount() {
        try { return new JSONObject(p.getString("knowledge","{}")).length(); } catch(Throwable e){ return 0; }
    }
    public void clear() { p.edit().clear().apply(); }

    private void trim(JSONObject root,int max) {
        try {
            JSONArray names=root.names(); if(names==null||names.length()<=max)return;
            ArrayList<String> keys=new ArrayList<>();
            for(int i=0;i<names.length();i++)keys.add(names.optString(i));
            keys.sort((a,b)->Long.compare(root.optJSONObject(a)==null?0:root.optJSONObject(a).optLong("updated",root.optJSONObject(a).optLong("time",0)), root.optJSONObject(b)==null?0:root.optJSONObject(b).optLong("updated",root.optJSONObject(b).optLong("time",0))));
            while(keys.size()>max){root.remove(keys.remove(0));}
        }catch(Throwable ignored){}
    }

    private double similarity(String a,String b){
        Set<String>x=new HashSet<>(Arrays.asList(a.split(" "))); Set<String>y=new HashSet<>(Arrays.asList(b.split(" ")));
        x.remove("");y.remove("");if(x.isEmpty()||y.isEmpty())return 0;
        Set<String> inter=new HashSet<>(x);inter.retainAll(y);
        double token=(2.0*inter.size())/(x.size()+y.size());
        double edit=1.0-((double)levenshtein(a,b)/Math.max(1,Math.max(a.length(),b.length())));
        double ngram=charNgram(a,b);
        return Math.max(token,Math.max(edit*.82,ngram));
    }
    private double charNgram(String a,String b){
        Set<String>x=ngrams(a),y=ngrams(b);if(x.isEmpty()||y.isEmpty())return 0;Set<String>i=new HashSet<>(x);i.retainAll(y);return (2.0*i.size())/(x.size()+y.size());
    }
    private Set<String> ngrams(String s){Set<String>r=new HashSet<>();String q=" "+s+" ";for(int i=0;i+2<q.length();i++)r.add(q.substring(i,i+3));return r;}
    private int levenshtein(String a,String b){int[]prev=new int[b.length()+1],cur=new int[b.length()+1];for(int j=0;j<=b.length();j++)prev[j]=j;for(int i=1;i<=a.length();i++){cur[0]=i;for(int j=1;j<=b.length();j++){int c=a.charAt(i-1)==b.charAt(j-1)?0:1;cur[j]=Math.min(Math.min(cur[j-1]+1,prev[j]+1),prev[j-1]+c);}int[]t=prev;prev=cur;cur=t;}return prev[b.length()];}
    public static String normalize(String s){return s==null?"":s.toLowerCase(new Locale("ru")).replace('ё','е').replaceAll("[^а-яa-z0-9 ]"," ").replaceAll("\\s+"," ").trim();}
}
