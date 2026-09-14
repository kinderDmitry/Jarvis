package com.jarvis.homemultitool;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.speech.*;
import android.speech.tts.UtteranceProgressListener;
import java.util.*;

/**
 * Best-effort system-wide wake listener. It works in short recognition windows because
 * Android's ordinary SpeechRecognizer is not an unrestricted always-on hotword engine.
 * When JARVIS is invoked, commands are processed in the service so another foreground app
 * does not have to be closed first. Selecting JARVIS as the Android Assistant remains the
 * most reliable system-wide invocation path.
 */
public class JarvisWakeWordService extends Service {
    private static final String CHANNEL="jarvis_wake"; private static final int NOTIFY=71;
    private SpeechRecognizer recognizer; private Intent intent; private Handler handler;
    private JarvisEngine engine; private JarvisVoiceManager voice;
    private boolean running,listening,processing; private long retry=500;

    @Override public void onCreate(){
        super.onCreate(); handler=new Handler(getMainLooper()); createChannel();
        if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){stopSelf();return;}
        if(!startForegroundCompat()||!SpeechRecognizer.isRecognitionAvailable(this))return;
        engine=new JarvisEngine(this,new JarvisEngine.Callback(){public void reply(String s){speak(s);}public void state(String s){updateNotification(s);}});
        voice=new JarvisVoiceManager(this); voice.init(null);
        intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ru-RU")
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"ru-RU")
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,false);
        running=true; createRecognizer(); if(recognizer!=null) startListening(350); else handler.postDelayed(()->{if(running){createRecognizer();startListening(100);}},1500);
    }

    private boolean startForegroundCompat(){
        try{Intent open=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);PendingIntent pi=PendingIntent.getActivity(this,72,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);b.setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("JARVIS • фоновый вызов").setContentText("Ожидание фразы «Джарвис»").setContentIntent(pi).setOngoing(true).setCategory(Notification.CATEGORY_SERVICE).setOnlyAlertOnce(true);if(Build.VERSION.SDK_INT>=29)startForeground(NOTIFY,b.build(),android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);else startForeground(NOTIFY,b.build());return true;}catch(Throwable e){stopSelf();return false;}}
    private void updateNotification(String state){try{NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);b.setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("JARVIS • фоновый вызов").setContentText(state==null?"Ожидание фразы «Джарвис»":state).setOngoing(true).setOnlyAlertOnce(true);nm.notify(NOTIFY,b.build());}catch(Throwable ignored){}}
    private void createRecognizer(){
        try{
            if(recognizer!=null)recognizer.destroy();
            if(Build.VERSION.SDK_INT>=31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this))
                recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
            else recognizer=SpeechRecognizer.createSpeechRecognizer(this);
            if(recognizer!=null)recognizer.setRecognitionListener(listener);
        }catch(Throwable e){
            try{recognizer=SpeechRecognizer.createSpeechRecognizer(this);recognizer.setRecognitionListener(listener);}catch(Throwable ignored){recognizer=null;}
        }
    }

    private final RecognitionListener listener=new RecognitionListener(){
        public void onReadyForSpeech(Bundle b){listening=true;retry=500;updateNotification("СЛУШАЮ ФРАЗУ «ДЖАРВИС»");}
        public void onBeginningOfSpeech(){listening=true;}
        public void onRmsChanged(float v){}
        public void onBufferReceived(byte[] b){}
        public void onEndOfSpeech(){listening=false;}
        public void onError(int e){listening=false;if(running)rearm(Math.min(retry,2500));retry=Math.min(retry*2,3000);}
        public void onPartialResults(Bundle b){check(b);}
        public void onResults(Bundle b){check(b);if(running&&!processing)rearm(180);}
        public void onEvent(int a,Bundle b){}
    };

    private void check(Bundle b){
        if(b==null||processing)return;ArrayList<String> rs=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(rs==null)return;
        for(String s:rs){if(s==null)continue;String x=s.toLowerCase(new Locale("ru")).replace('ё','е').trim();String compact=x.replaceAll("[^а-яa-z0-9]","");boolean wake=x.matches(".*\\b(привет\\s+)?джарвис(у|а|ом)?\\b.*")||compact.contains("джарвис")||compact.contains("djarvis")||compact.contains("jarvis");if(wake){String q=x.replaceAll("(?iu)\\bджарвис(у|а|ом)?\\b"," ").replaceAll("(?iu)\\bjarvis\\b"," ").replaceAll("\\s+"," ").trim();trigger(q);return;}}
    }

    private void trigger(String q){
        processing=true;listening=false;try{if(recognizer!=null){recognizer.cancel();recognizer.destroy();recognizer=null;}}catch(Throwable ignored){}
        if(q.isEmpty()){
            speak("Слушаю, сэр.");
            // The TTS completion callback re-arms the listener.

        }else{
            updateNotification("ОБРАБАТЫВАЮ КОМАНДУ");
            if(engine!=null)engine.handle(q); else {processing=false;createRecognizer();startListening(150);}
            // Re-arm after the spoken answer completes; this avoids microphone/TTS contention.

        }
    }

    private void speak(String s){if(voice==null){processing=false;rearm(250);return;}if(!voice.isReady()){handler.postDelayed(()->speak(s),500);return;}voice.speak(s,new UtteranceProgressListener(){public void onStart(String id){}public void onDone(String id){processing=false;rearm(350);}public void onError(String id){processing=false;rearm(350);}});}
    private void rearm(long delay){if(!running||processing)return;handler.postDelayed(()->{if(!running||processing)return;createRecognizer();startListening(100);},delay);}
    private void startListening(long delay){if(!running||processing)return;handler.postDelayed(()->{if(!running||processing||recognizer==null||listening)return;try{recognizer.cancel();recognizer.startListening(intent);}catch(Throwable e){rearm(1000);}},delay);}
    @Override public int onStartCommand(Intent i,int flags,int id){return START_STICKY;}
    @Override public void onTaskRemoved(Intent root){if(running)try{Intent i=new Intent(this,JarvisWakeWordService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}catch(Throwable ignored){}super.onTaskRemoved(root);}
    @Override public void onDestroy(){running=false;if(handler!=null)handler.removeCallbacksAndMessages(null);if(recognizer!=null)try{recognizer.destroy();}catch(Throwable ignored){}if(engine!=null)engine.shutdown();if(voice!=null)voice.shutdown();super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,"JARVIS • фоновый вызов",NotificationManager.IMPORTANCE_LOW);c.setDescription("Фоновое ожидание команды «Джарвис»");((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);}}
}
