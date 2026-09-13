package com.jarvis.homemultitool;

import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import java.util.*;

/** Centralized voice policy. Uses only real voices supplied by the installed Android TTS engine. */
public final class JarvisVoiceManager {
    public static final String MALE = "male";
    public static final String FEMALE = "female";
    public static final String AUTO = "auto";
    private final Context context;
    private final SharedPreferences prefs;
    private TextToSpeech tts;
    private boolean ready;

    public JarvisVoiceManager(Context c){
        context=c.getApplicationContext();
        prefs=context.getSharedPreferences("jarvis_voice",Context.MODE_PRIVATE);
    }

    public void init(final Runnable onReady){
        if(tts!=null){ if(onReady!=null) onReady.run(); return; }
        tts=new TextToSpeech(context,status->{
            ready=status==TextToSpeech.SUCCESS;
            if(ready) configure();
            if(onReady!=null) onReady.run();
        });
    }

    private void configure(){
        try{
            tts.setLanguage(new Locale("ru","RU"));
            String gender=prefs.getString("gender",AUTO);
            Voice selected=findVoice(gender);
            if(selected!=null) tts.setVoice(selected);
            float rate=prefs.getFloat("rate",0.90f);
            float pitch=prefs.getFloat("pitch",0.90f);
            if(MALE.equals(gender)){ rate=0.88f; pitch=0.82f; }
            else if(FEMALE.equals(gender)){ rate=0.94f; pitch=1.00f; }
            tts.setSpeechRate(rate);
            tts.setPitch(pitch);
        }catch(Throwable ignored){}
    }

    private Voice findVoice(String gender){
        try{
            Voice best=null; int bestScore=Integer.MIN_VALUE;
            for(Voice v:tts.getVoices()){
                Locale l=v.getLocale();
                if(l==null||!"ru".equalsIgnoreCase(l.getLanguage())) continue;
                String n=(v.getName()==null?"":v.getName()).toLowerCase(Locale.ROOT);
                int score=0;
                if(v.getQuality()>=Voice.QUALITY_HIGH) score+=45; else if(v.getQuality()>=Voice.QUALITY_NORMAL) score+=20;
                if(!v.isNetworkConnectionRequired()) score+=12; else score+=5;
                if(MALE.equals(gender)){
                    if(n.matches(".*(male|man|муж|мужск|мужчина|алекс|иван|павел|серг|андре|михаил|никол|дмитр).*")) score+=120;
                    if(n.matches(".*(female|woman|жен|женск|женщина).*")) score-=120;
                }else if(FEMALE.equals(gender)){
                    if(n.matches(".*(female|woman|жен|женск|женщина|елена|анна|мария|ольга|ирина|натал).*")) score+=120;
                    if(n.matches(".*(male|man|муж|мужск|мужчина).*")) score-=120;
                }
                if(score>bestScore){bestScore=score;best=v;}
            }
            return best;
        }catch(Throwable ignored){return null;}
    }

    public void setGender(String gender){prefs.edit().putString("gender",gender==null?AUTO:gender).apply();if(ready)configure();}
    public String getGender(){return prefs.getString("gender",AUTO);}
    public boolean isReady(){return ready&&tts!=null;}
    public List<String> availableRussianVoices(){
        List<String> out=new ArrayList<>();
        if(tts==null)return out;
        try{for(Voice v:tts.getVoices())if(v.getLocale()!=null&&"ru".equalsIgnoreCase(v.getLocale().getLanguage()))out.add(v.getName());}catch(Throwable ignored){}
        return out;
    }
    public void speak(String text, UtteranceProgressListener listener){
        if(!isReady()||text==null||text.trim().isEmpty())return;
        try{
            if(listener!=null)tts.setOnUtteranceProgressListener(listener);
            tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"jarvis_reply");
        }catch(Throwable ignored){}
    }
    public void stop(){if(tts!=null)try{tts.stop();}catch(Throwable ignored){}}
    public void shutdown(){if(tts!=null){try{tts.stop();}catch(Throwable ignored){}try{tts.shutdown();}catch(Throwable ignored){}tts=null;ready=false;}}
}
