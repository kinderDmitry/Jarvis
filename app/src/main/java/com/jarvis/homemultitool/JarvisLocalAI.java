package com.jarvis.homemultitool;

import java.util.*;
import java.util.regex.*;

/**
 * Small on-device semantic layer for JARVIS.
 * It is not a fake list of commands: it scores intents from words, stems,
 * paraphrases and dialogue context, then hands the result to the real action engine.
 * It continuously improves through JarvisMemory, which stores successful user phrasing.
 */
public final class JarvisLocalAI {
    public static final String PLAY="PLAY", PAUSE="PAUSE", NEXT="NEXT", PREVIOUS="PREVIOUS";
    public static final String MUSIC="MUSIC", TRACK="TRACK", LIKE="LIKE", DISLIKE="DISLIKE";
    public static final String WEATHER="WEATHER", SEARCH="SEARCH", NEWS="NEWS", VIDEO="VIDEO";
    public static final String TIMER="TIMER", ALARM="ALARM", VOLUME="VOLUME", TORCH="TORCH";
    public static final String CAMERA="CAMERA", SETTINGS="SETTINGS", APP="APP", CALL="CALL", SMS="SMS";
    public static final String MATH="MATH", MEMORY="MEMORY", CONVERSATION="CONVERSATION", UNKNOWN="UNKNOWN";

    public static final class Decision {
        public final String intent;
        public final double confidence;
        public final String canonical;
        public final String query;
        public final boolean contextual;
        Decision(String i,double c,String can,String q,boolean ctx){intent=i;confidence=c;canonical=can;query=q;contextual=ctx;}
    }

    private static final LinkedHashMap<String,String[]> WORDS=new LinkedHashMap<>();
    static {
        WORDS.put(PLAY,new String[]{"продолж","возобнов","сними с пауз","включи обратно","играй","воспроизвед","запусти музыку"});
        WORDS.put(PAUSE,new String[]{"пауза","приостанов","останови музыку","останови воспроизвед","замри","стоп музыку"});
        WORDS.put(NEXT,new String[]{"дальше","следующ","следом","переключи трек","следующая песн"});
        WORDS.put(PREVIOUS,new String[]{"назад","предыдущ","верни песн","верни трек"});
        WORDS.put(MUSIC,new String[]{"музык","песн","трек","плейлист","любим","понравив","лайкнут","избранн","рок","рэп","джаз","поп","электрон","классическ","новинки","в дороге","для дороги","для танцев","для спорта","для работы","для сна","для расслабления"});
        WORDS.put(TRACK,new String[]{"что сейчас играет","что играет","какая песня","название песни","текущий трек"});
        WORDS.put(LIKE,new String[]{"поставь лайк","лайкни","нравится этот трек","добавь в понравивш"});
        WORDS.put(DISLIKE,new String[]{"поставь дизлайк","дизлайкни","не нравится этот трек"});
        WORDS.put(WEATHER,new String[]{"погод","температур","осадк","дожд","снег","ветер","прогноз"});
        WORDS.put(NEWS,new String[]{"новост","событи","что произошло","последние новости"});
        WORDS.put(VIDEO,new String[]{"фильм","сериал","кино","видео","мультфильм"});
        WORDS.put(SEARCH,new String[]{"найди","ищи","поищи","что такое","кто такой","кто такая","почему","где","когда","сколько стоит","курс","цена","расскажи про"});
        WORDS.put(TIMER,new String[]{"таймер","отсчёт","обратный отсчёт"});
        WORDS.put(ALARM,new String[]{"будильник","разбуди меня"});
        WORDS.put(VOLUME,new String[]{"громче","тише","громкость","сделай громче","сделай тише"});
        WORDS.put(TORCH,new String[]{"фонарик","вспышк"});
        WORDS.put(CAMERA,new String[]{"открой камеру","запусти камеру","камера"});
        WORDS.put(SETTINGS,new String[]{"настройк","параметр","разрешени","доступ","wifi","вайфай","блютуз","bluetooth","экран"});
        WORDS.put(APP,new String[]{"открой приложение","запусти приложение","зайди в","открой","запусти"});
        WORDS.put(CALL,new String[]{"позвони","набери номер","соверши звонок"});
        WORDS.put(SMS,new String[]{"смс","сообщение","напиши сообщение","отправь сообщение"});
        WORDS.put(MATH,new String[]{"посчитай","вычисли","сколько будет","плюс","минус","умнож","подел","делить"});
        WORDS.put(MEMORY,new String[]{"запомни","сохрани","что ты помнишь","мои заметки","забудь всё","очисти память","научи"});
    }

    public Decision analyze(String raw,String normalized,String lastTopic,String lastApp,JarvisMemory memory){
        String s=normalize(normalized==null?raw:normalized);
        if(s.isEmpty())return new Decision(UNKNOWN,0,"", "",false);
        JarvisMemory.LearnedMatch lm=memory==null?new JarvisMemory.LearnedMatch("",0):memory.bestLearned(s);
        if(lm.confidence>=0.90 && !lm.action.isEmpty()) return new Decision("LEARNED",lm.confidence,lm.action,"",true);

        LinkedHashMap<String,Double> scores=new LinkedHashMap<>();
        for(Map.Entry<String,String[]> entry:WORDS.entrySet()) scores.put(entry.getKey(),0d);
        for(Map.Entry<String,String[]> e:WORDS.entrySet()){
            double score=0;
            for(String w:e.getValue()){
                if(s.contains(w)) score += w.length()>=8?0.30:0.18;
                if(s.equals(w)) score += 0.22;
            }
            scores.put(e.getKey(),score);
        }
        // High-value structural signals.
        if(s.matches(".*\\b(дальше|следующ|следом)\\b.*")) scores.put(NEXT,scores.get(NEXT)+0.65);
        if(s.matches(".*\\b(назад|предыдущ)\\b.*")) scores.put(PREVIOUS,scores.get(PREVIOUS)+0.65);
        if(s.matches(".*\\b(пауза|стоп)\\b.*") && s.contains("музык")) scores.put(PAUSE,scores.get(PAUSE)+0.55);
        if(s.matches(".*\\b(продолж|возобнов)\\b.*")) scores.put(PLAY,scores.get(PLAY)+0.55);
        if(s.matches(".*\\b(переключи|смени|измени)\\b.*") &&
                s.matches(".*\\b(в\\s+дорог|для\\s+дорог|танцев|спорт|работ|релакс|сна)\\b.*"))
            scores.put(MUSIC,scores.get(MUSIC)+0.70);
        if(s.matches(".*\\b\\d+\\s*(сек|секунд|мин|минут|час|часа|ч)\\b.*")) scores.put(TIMER,scores.get(TIMER)+0.8);
        if(s.matches(".*\\b(на|в)\\s*\\d{1,2}(:\\d{2})?.*" ) && s.contains("будильник")) scores.put(ALARM,scores.get(ALARM)+0.8);
        if(s.matches(".*\\d+.*") && (s.contains("плюс")||s.contains("минус")||s.contains("умнож")||s.contains("подел")||s.contains("сколько будет"))) scores.put(MATH,scores.get(MATH)+0.8);
        if(s.contains("новост")) scores.put(NEWS,scores.get(NEWS)+0.75);
        if(s.contains("погод")) scores.put(WEATHER,scores.get(WEATHER)+0.75);
        if(s.contains("фильм")||s.contains("сериал")||s.contains("кино")) scores.put(VIDEO,scores.get(VIDEO)+0.65);

        // Context makes short follow-ups meaningful.
        boolean contextual=false;
        if(lastTopic!=null && lastTopic.equals("weather") && (s.equals("завтра")||s.equals("послезавтра")||s.equals("а завтра")||s.equals("а послезавтра"))){
            scores.put(WEATHER,scores.get(WEATHER)+1.2); contextual=true;
        }
        if(lastApp!=null && !lastApp.isEmpty() && (s.equals("дальше")||s.equals("назад")||s.equals("продолжай")||s.equals("пауза"))){contextual=true;}

        String best=UNKNOWN; double max=0, second=0;
        for(Map.Entry<String,Double> e:scores.entrySet()){
            double v=e.getValue(); if(v>max){second=max;max=v;best=e.getKey();} else if(v>second)second=v;
        }
        double confidence=Math.min(0.99, max/(max+0.55));
        if(max<0.25) confidence=0;
        // Avoid choosing generic APP/SEARCH over a specific action.
        if(APP.equals(best) && (s.contains("музык")||s.contains("песн")||s.contains("трек"))) best=MUSIC;
        if(SEARCH.equals(best) && (s.contains("новост")||s.contains("погод")||s.contains("фильм")||s.contains("сериал"))) best=s.contains("новост")?NEWS:s.contains("погод")?WEATHER:VIDEO;
        String query=extractQuery(raw==null?s:raw,best);
        return new Decision(best,confidence,canonical(best,s),query,contextual || confidence>0.70);
    }

    private String canonical(String intent,String s){
        if(NEXT.equals(intent))return "дальше";
        if(PREVIOUS.equals(intent))return "назад";
        if(PAUSE.equals(intent))return "пауза";
        if(PLAY.equals(intent))return "продолжай";
        return s;
    }
    private String extractQuery(String raw,String intent){
        String q=raw==null?"":raw.trim();
        q=q.replaceFirst("(?iu)^\\s*(привет\\s+)?(джарвис|жарвис|дарвис)\\s*[,.:;-]?\\s*","");
        if(MUSIC.equals(intent)||VIDEO.equals(intent)||SEARCH.equals(intent)){
            q=q.replaceFirst("(?iu).*?(найди|ищи|поищи|включи|поставь|запусти|проиграй|сыграй|покажи|расскажи про)\\s*"," ").trim();
        }
        return q.trim();
    }
    private String normalize(String s){return s==null?"":s.toLowerCase(new Locale("ru")).replace('ё','е').replaceAll("[^а-яa-z0-9 ]"," ").replaceAll("\\s+"," ").trim();}
}
