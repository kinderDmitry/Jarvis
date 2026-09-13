package com.jarvis.homemultitool;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;

/** Optional lightweight local wake-word bridge. It listens in short recognition windows and restarts. */
public class JarvisWakeWordService extends Service {
    private static final String CHANNEL = "jarvis_wake";
    private SpeechRecognizer recognizer;
    private Intent recognizerIntent;
    private boolean running;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("JARVIS активен")
                .setContentText("Ожидание фразы «Джарвис» — локально")
                .setOngoing(true).build();
        if (Build.VERSION.SDK_INT >= 29) startForeground(71, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        else startForeground(71, n);
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { stopSelf(); return; }
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
        } else {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        }
        recognizer.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(android.os.Bundle b) {}
            public void onBeginningOfSpeech() {}
            public void onRmsChanged(float v) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() {}
            public void onError(int e) { restartLater(); }
            public void onResults(android.os.Bundle b) {
                ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (r != null) for (String text : r) {
                    String x = text.toLowerCase(new java.util.Locale("ru"));
                    if (x.contains("джарвис") || x.contains("джарвису")) { launchAssistant(extractQuery(x)); break; }
                }
                restartLater();
            }
            public void onPartialResults(android.os.Bundle b) {}
            public void onEvent(int a, android.os.Bundle b) {}
        });
        running = true;
        startListening();
    }

    private void startListening() {
        if (!running || recognizer == null) return;
        try { recognizer.startListening(recognizerIntent); } catch (Throwable ignored) { restartLater(); }
    }

    private void restartLater() {
        if (!running) return;
        new android.os.Handler(getMainLooper()).postDelayed(this::startListening, 350);
    }

    private String extractQuery(String text) {
        String x = text.replaceAll("(?i)джарвису?", " ").trim();
        x = x.replaceAll("^[,.:;\\-]+", "").trim();
        return x;
    }

    private void launchAssistant(String query) {
        Intent i = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        i.putExtra("WAKE_WORD", true);
        if (query != null && !query.isEmpty()) i.putExtra("WAKE_QUERY", query);
        i.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        startActivity(i);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "JARVIS Wake Word", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public void onDestroy() { running = false; if (recognizer != null) { try { recognizer.destroy(); } catch (Throwable ignored) {} } super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
