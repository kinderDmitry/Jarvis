package com.jarvis.homemultitool;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Real VoiceInteractionSession UI. It deliberately does not launch MainActivity,
 * so invoking JARVIS from the keyguard shows only the assistant surface instead
 * of replacing the lock screen with the full application.
 */
public class JarvisVoiceSession extends android.service.voice.VoiceInteractionSession {
    private SpeechRecognizer recognizer;
    private JarvisEngine engine;
    private JarvisVoiceManager voice;
    private TextView status, transcript, answer;
    private boolean listening;
    private boolean active;

    public JarvisVoiceSession(Context context) {
        super(context);
        try { setTheme(android.R.style.Theme_DeviceDefault_NoActionBar); } catch(Throwable ignored) {}
    }

    @Override public View onCreateContentView() {
        LinearLayout box=new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(48,36,48,36);
        box.setBackgroundColor(Color.rgb(2,7,12));
        TextView title=new TextView(getContext()); title.setText("J A R V I S"); title.setTextColor(Color.rgb(82,214,255)); title.setTextSize(24); title.setGravity(Gravity.CENTER); box.addView(title,new LinearLayout.LayoutParams(-1,60));
        status=new TextView(getContext()); status.setText("ГОТОВ"); status.setTextColor(Color.WHITE); status.setTextSize(13); status.setGravity(Gravity.CENTER); box.addView(status,new LinearLayout.LayoutParams(-1,50));
        transcript=new TextView(getContext()); transcript.setText(""); transcript.setTextColor(Color.LTGRAY); transcript.setTextSize(16); transcript.setGravity(Gravity.CENTER); box.addView(transcript,new LinearLayout.LayoutParams(-1,80));
        answer=new TextView(getContext()); answer.setText(""); answer.setTextColor(Color.WHITE); answer.setTextSize(17); answer.setGravity(Gravity.CENTER); box.addView(answer,new LinearLayout.LayoutParams(-1,110));
        TextView hint=new TextView(getContext()); hint.setText("Голосовой режим • экран приложения не открывается"); hint.setTextColor(Color.rgb(110,140,160)); hint.setTextSize(11); hint.setGravity(Gravity.CENTER); box.addView(hint,new LinearLayout.LayoutParams(-1,45));
        return box;
    }

    @Override public void onShow(Bundle args,int flags){
        super.onShow(args,flags);
        engine=new JarvisEngine(getContext(),new JarvisEngine.Callback(){public void reply(String s){if(answer!=null)answer.setText(s);if(voice!=null&&voice.isReady()) voice.speak(s,new android.speech.tts.UtteranceProgressListener(){public void onStart(String id){}public void onDone(String id){if(active) postListen();}public void onError(String id){if(active) postListen();}}); else postListen();}public void state(String s){if(status!=null)status.setText(s==null?"ГОТОВ":s);}});
        voice=new JarvisVoiceManager(getContext()); voice.init(null);
        active=true; startRecognition();
    }

    private void postListen(){
        if(!active)return;
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->{
            if(active&&!listening) startRecognition();
        },450);
    }

    private void startRecognition(){
        if(android.os.Build.VERSION.SDK_INT>=23 && getContext().checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){status.setText("НУЖЕН ДОСТУП К МИКРОФОНУ");return;}
        try{
            if(recognizer!=null)recognizer.destroy();
            recognizer=(android.os.Build.VERSION.SDK_INT>=31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(getContext()))?SpeechRecognizer.createOnDeviceSpeechRecognizer(getContext()):SpeechRecognizer.createSpeechRecognizer(getContext());
            recognizer.setRecognitionListener(new RecognitionListener(){
                public void onReadyForSpeech(Bundle b){listening=true;status.setText("СЛУШАЮ");}
                public void onBeginningOfSpeech(){status.setText("СЛУШАЮ");}
                public void onRmsChanged(float v){} public void onBufferReceived(byte[] b){} public void onEndOfSpeech(){listening=false;status.setText("ОБРАБОТКА");}
                public void onError(int e){listening=false;status.setText("ГОТОВ");}
                public void onResults(Bundle b){ArrayList<String> r=b==null?null:b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty()){String q=r.get(0);transcript.setText(q);engine.handle(q);}listening=false;}
                public void onPartialResults(Bundle b){} public void onEvent(int a,Bundle b){}
            });
            Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ru-RU").putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5).putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);
            recognizer.startListening(i);
        }catch(Throwable e){if(status!=null)status.setText("РЕЧЬ НЕДОСТУПНА");}
    }

    @Override public void onHide(){
        active=false;
        try{if(recognizer!=null)recognizer.cancel();}catch(Throwable ignored){}
        try{if(recognizer!=null)recognizer.destroy();}catch(Throwable ignored){}
        recognizer=null; if(engine!=null)engine.shutdown(); if(voice!=null)voice.shutdown(); super.onHide();
    }
}
