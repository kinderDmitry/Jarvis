package com.jarvis.homemultitool;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.Locale;

/** Foreground wake-word bridge. The user explicitly enables it in Settings. */
public class JarvisWakeWordService extends Service {
    private static final String CHANNEL="jarvis_wake";
    private SpeechRecognizer recognizer; private Intent intent; private Handler handler;
    private boolean running,triggered,listening;

    @Override public void onCreate(){
        super.onCreate(); handler=new Handler(getMainLooper()); createChannel();
        if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){stopSelf();return;}
        startForegroundCompat();
        if(!SpeechRecognizer.isRecognitionAvailable(this)){stopSelf();return;}
        intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ru-RU")
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);
        try{recognizer=(Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this))?SpeechRecognizer.createOnDeviceSpeechRecognizer(this):SpeechRecognizer.createSpeechRecognizer(this);}catch(Throwable e){recognizer=null;}
        if(recognizer==null){stopSelf();return;}
        recognizer.setRecognitionListener(listener); running=true; startListening(300);
    }

    private void startForegroundCompat(){
        Notification n=new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("JARVIS • фоновый вызов").setContentText("Ожидание фразы «Джарвис»")
                .setOngoing(true).setCategory(Notification.CATEGORY_SERVICE).build();
        try{if(Build.VERSION.SDK_INT>=29)startForeground(71,n,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);else startForeground(71,n);}catch(Throwable e){stopSelf();}
    }

    private final RecognitionListener listener=new RecognitionListener(){
        public void onReadyForSpeech(Bundle b){listening=true;}
        public void onBeginningOfSpeech(){listening=true;}
        public void onRmsChanged(float v){}
        public void onBufferReceived(byte[] b){}
        public void onEndOfSpeech(){listening=false;}
        public void onError(int e){listening=false;startListening(650);}
        public void onPartialResults(Bundle b){check(b);}
        public void onResults(Bundle b){check(b);listening=false;startListening(450);}
        public void onEvent(int a,Bundle b){}
    };

    private void check(Bundle b){
        if(triggered||b==null)return; ArrayList<String> results=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION); if(results==null)return;
        for(String s:results){if(s==null)continue;String x=s.toLowerCase(new Locale("ru")).trim();
            if(x.matches(".*\\bджарвис(у|а|ом)?\\b.*")){
                triggered=true; String q=x.replaceAll("(?i)\\bджарвис(у|а|ом)?\\b"," ").replaceAll("\\s+"," ").trim(); launch(q); return;
            }
        }
    }

    private void launch(String q){
        try{
            if(recognizer!=null)recognizer.cancel();
            Intent i=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            i.putExtra("WAKE_WORD",true); if(!q.isEmpty())i.putExtra("WAKE_QUERY",q);
            i.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            startActivity(i);
        }catch(Throwable ignored){}
        handler.postDelayed(()->{triggered=false;if(running)startListening(500);},1800);
    }

    private void startListening(long delay){
        if(!running||recognizer==null)return; handler.postDelayed(()->{
            if(!running||recognizer==null||listening)return;
            try{recognizer.cancel();recognizer.startListening(intent);}catch(Throwable e){startListening(1200);}
        },delay);
    }

    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,"JARVIS • фоновый вызов",NotificationManager.IMPORTANCE_LOW);c.setDescription("Фоновое ожидание команды «Джарвис»");((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);}}
    @Override public int onStartCommand(Intent i,int flags,int id){return START_STICKY;}
    @Override public void onDestroy(){running=false;if(handler!=null)handler.removeCallbacksAndMessages(null);if(recognizer!=null)try{recognizer.destroy();}catch(Throwable ignored){}super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
