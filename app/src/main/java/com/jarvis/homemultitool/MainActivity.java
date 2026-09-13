package com.jarvis.homemultitool;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.Gravity;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Premium JARVIS surface: centered voice-first HUD, readable conversation and
 * one-tap actions. No placeholder controls; every visible action is wired.
 */
public class MainActivity extends Activity {
    private static final int BG = Color.rgb(1, 6, 12);
    private static final int CARD = Color.rgb(5, 15, 26);
    private static final int CARD2 = Color.rgb(7, 20, 34);
    private static final int CYAN = Color.rgb(83, 218, 255);
    private static final int BLUE = Color.rgb(24, 137, 220);
    private static final int WHITE = Color.WHITE;
    private static final int MUTED = Color.rgb(130, 160, 187);
    private static final int GREEN = Color.rgb(69, 235, 177);
    private static final int RED = Color.rgb(255, 103, 116);
    private static final int REQ_MIC = 42;

    private TextView status, userLine, jarvisLine, online, modeLabel, micLabel;
    private EditText input;
    private SpeechRecognizer recognizer;
    private Intent recognizerIntent;
    private TextToSpeech tts;
    private JarvisCoreView core;
    private JarvisEngine engine;
    private LinearLayout content;
    private boolean speaking;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        boolean lock = getIntent().getBooleanExtra("LOCKSCREEN_ASSIST", false)
                || getIntent().getBooleanExtra("WAKE_WORD", false);
        if (lock) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        buildUi();
        engine = new JarvisEngine(this, new JarvisEngine.Callback() {
            @Override public void reply(String s) { speak(s); }
            @Override public void state(String s) { setState(s); }
        });
        initTts();
        initSpeech();

        String wakeQuery = getIntent().getStringExtra("WAKE_QUERY");
        if (wakeQuery != null && !wakeQuery.trim().isEmpty()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> command(wakeQuery), 260);
        } else if (lock) {
            new Handler(Looper.getMainLooper()).postDelayed(this::listen, 500);
        }
    }

    private TextView text(String s, float size) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextColor(WHITE); v.setTextSize(size);
        return v;
    }

    private TextView micro(String s) {
        TextView v = text(s, 10);
        v.setTextColor(MUTED);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setLetterSpacing(.12f);
        return v;
    }

    private GradientDrawable shape(int stroke, int fill, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill); g.setCornerRadius(radius);
        if (stroke != 0) g.setStroke(1, stroke);
        return g;
    }

    private LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(l, t, r, b); return p;
    }

    private void buildUi() {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        root.setPadding(16, 18, 16, 22);

        // Centered content column prevents the HUD from looking glued to the status bar.
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(0, 0, 0, 0);
        root.addView(content, lp(-1, -2, 0, 0, 0, 0));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("JARVIS", 25);
        brand.setTextColor(CYAN); brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(brand, lp(0, 38, 0, 0, 0, 0));
        ((LinearLayout.LayoutParams) brand.getLayoutParams()).weight = 1;
        modeLabel = micro("PERSONAL AI  •  WEB AGENT");
        modeLabel.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        header.addView(modeLabel, lp(0, 38, 0, 0, 8, 0));
        TextView settings = text("⚙", 21);
        settings.setGravity(Gravity.CENTER);
        settings.setTextColor(WHITE);
        settings.setBackground(shape(0xFF16324A, 0xFF07131F, 20));
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        header.addView(settings, lp(44, 44, 0, 0, 0, 0));
        content.addView(header, lp(-1, 48, 0, 0, 0, 6));

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        status = text("ГОТОВ", 11); status.setTextColor(CYAN);
        status.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        statusRow.addView(status, lp(0, 30, 0, 0, 0, 0));
        ((LinearLayout.LayoutParams) status.getLayoutParams()).weight = 1;
        online = micro("●  ПРОВЕРКА СЕТИ"); online.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        statusRow.addView(online, lp(0, 30, 0, 0, 0, 0));
        ((LinearLayout.LayoutParams) online.getLayoutParams()).weight = 1;
        content.addView(statusRow, lp(-1, 30, 0, 0, 0, 3));
        updateNetworkState();

        // Hero card: the visual focus of the application.
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(10, 10, 10, 6);
        hero.setBackground(shape(0xFF16466A, 0xCC040D17, 28));

        TextView heroTitle = micro("J A R V I S   C O R E");
        heroTitle.setTextColor(MUTED); heroTitle.setGravity(Gravity.CENTER);
        hero.addView(heroTitle, lp(-1, 24, 0, 0, 0, 0));

        FrameLayout coreFrame = new FrameLayout(this);
        core = new JarvisCoreView(this);
        core.setState("ГОТОВ");
        core.setOnClickListener(v -> listen());
        core.startPulse();
        coreFrame.addView(core, new FrameLayout.LayoutParams(-1, -1));
        hero.addView(coreFrame, lp(-1, 280, 0, 0, 0, 0));

        micLabel = text("НАЖМИТЕ НА ЯДРО ИЛИ СКАЖИТЕ «ДЖАРВИС»", 10);
        micLabel.setTextColor(MUTED); micLabel.setGravity(Gravity.CENTER);
        micLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        hero.addView(micLabel, lp(-1, 28, 0, 0, 0, 0));
        content.addView(hero, lp(-1, 342, 0, 0, 0, 9));

        // Conversation card with distinct user/assistant hierarchy.
        LinearLayout conversation = new LinearLayout(this);
        conversation.setOrientation(LinearLayout.VERTICAL);
        conversation.setPadding(16, 12, 16, 13);
        conversation.setBackground(shape(0xFF173B58, CARD, 22));

        userLine = micro("ГОТОВ К ЗАПРОСУ");
        conversation.addView(userLine, lp(-1, 21, 0, 0, 0, 3));
        jarvisLine = text("Сэр, я готов. Спросите что-нибудь или дайте команду.", 14);
        jarvisLine.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        jarvisLine.setMaxLines(4);
        conversation.addView(jarvisLine, lp(-1, -2, 0, 0, 0, 0));
        content.addView(conversation, lp(-1, -2, 0, 0, 0, 8));

        // Text input is intentionally compact; voice remains primary.
        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        input = new EditText(this);
        input.setSingleLine(true); input.setTextColor(WHITE); input.setHintTextColor(MUTED);
        input.setTextSize(14); input.setHint("Спросить JARVIS…"); input.setPadding(17, 0, 12, 0);
        input.setBackground(shape(0xFF1B5378, CARD2, 26)); input.setImeOptions(6);
        inputRow.addView(input, lp(0, 52, 0, 0, 8, 0));
        ((LinearLayout.LayoutParams) input.getLayoutParams()).weight = 1;
        TextView send = text("➤", 23); send.setTextColor(CYAN); send.setGravity(Gravity.CENTER);
        send.setBackground(shape(0xFF1A5B82, 0xFF071B2B, 26)); send.setOnClickListener(v -> sendText());
        inputRow.addView(send, lp(52, 52, 0, 0, 0, 0));
        content.addView(inputRow, lp(-1, 52, 0, 0, 0, 7));

        LinearLayout voiceRow = new LinearLayout(this);
        voiceRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView mic = text("●  ГОВОРИТЬ", 13); mic.setTextColor(CYAN); mic.setGravity(Gravity.CENTER);
        mic.setTypeface(Typeface.DEFAULT, Typeface.BOLD); mic.setBackground(shape(0xFF1A567C, 0xFF071927, 24));
        mic.setOnClickListener(v -> listen());
        voiceRow.addView(mic, lp(0, 50, 0, 0, 7, 0)); ((LinearLayout.LayoutParams) mic.getLayoutParams()).weight = 1;
        TextView stop = text("■", 17); stop.setTextColor(WHITE); stop.setGravity(Gravity.CENTER);
        stop.setBackground(shape(0xFF173A54, 0xFF07131F, 24)); stop.setOnClickListener(v -> stopAll());
        voiceRow.addView(stop, lp(50, 50, 0, 0, 0, 0));
        content.addView(voiceRow, lp(-1, 50, 0, 0, 0, 7));

        LinearLayout quick = new LinearLayout(this); quick.setGravity(Gravity.CENTER);
        addQuick(quick, "Время", "который час"); addQuick(quick, "Погода", "погода");
        addQuick(quick, "Новости", "новости"); addQuick(quick, "Таймер", "таймер");
        content.addView(quick, lp(-1, 40, 0, 0, 0, 0));

        scroll.addView(root, new ScrollView.LayoutParams(-1, -1));
        frame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        setContentView(frame);
    }

    private void addQuick(LinearLayout row, String title, String command) {
        TextView b = text(title, 11); b.setTextColor(WHITE); b.setGravity(Gravity.CENTER);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setBackground(shape(0xFF173852, 0xFF071624, 22));
        b.setOnClickListener(v -> command(command));
        row.addView(b, lp(0, 40, 3, 0, 3, 0));
        ((LinearLayout.LayoutParams) b.getLayoutParams()).weight = 1;
    }

    private void updateNetworkState() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Network n = cm == null ? null : cm.getActiveNetwork();
            boolean connected = n != null && cm.getNetworkCapabilities(n) != null;
            online.setText(connected ? "●  В СЕТИ" : "●  НЕТ СЕТИ");
            online.setTextColor(connected ? GREEN : RED);
        } catch (Throwable ignored) { }
    }

    private void initTts() {
        tts = new TextToSpeech(this, r -> {
            if (r != TextToSpeech.SUCCESS) return;
            try {
                Locale ru = new Locale("ru", "RU");
                tts.setLanguage(ru);
                Voice best = null;
                Set<Voice> voices = tts.getVoices();
                if (voices != null) {
                    for (Voice v : voices) {
                        if (!ru.equals(v.getLocale()) && !v.getLocale().toLanguageTag().startsWith("ru")) continue;
                        if (v.isNetworkConnectionRequired()) continue;
                        if (best == null || v.getQuality() > best.getQuality()) best = v;
                    }
                }
                if (best != null) tts.setVoice(best);
                // A calmer rate/pitch sounds less synthetic on common Android TTS engines.
                tts.setSpeechRate(.91f);
                tts.setPitch(.90f);
            } catch (Throwable ignored) { }
        });
    }

    private void initSpeech() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return;
        try {
            if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this))
                recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
            else recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        } catch (Throwable t) { recognizer = null; }
        if (recognizer == null) return;
        recognizer.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(Bundle b) { setState("СЛУШАЮ"); }
            public void onBeginningOfSpeech() { setState("СЛУШАЮ"); }
            public void onRmsChanged(float v) { }
            public void onBufferReceived(byte[] b) { }
            public void onEndOfSpeech() { setState("ОБРАБОТКА"); }
            public void onError(int e) { setState("ГОТОВ"); }
            public void onResults(Bundle b) {
                ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (r != null && !r.isEmpty()) command(r.get(0)); else setState("ГОТОВ");
            }
            public void onPartialResults(Bundle b) { }
            public void onEvent(int a, Bundle b) { }
        });
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
    }

    private void listen() {
        if (recognizer == null) { setState("РЕЧЬ НЕДОСТУПНА"); return; }
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC); return;
        }
        try { if (speaking && tts != null) tts.stop(); recognizer.startListening(recognizerIntent); setState("СЛУШАЮ"); }
        catch (Throwable t) { setState("ГОТОВ"); }
    }

    private void stopAll() {
        if (recognizer != null) { try { recognizer.stopListening(); } catch (Throwable ignored) {} try { recognizer.cancel(); } catch (Throwable ignored) {} }
        if (tts != null) try { tts.stop(); } catch (Throwable ignored) {}
        speaking = false; setState("ГОТОВ");
    }

    private void sendText() {
        String s = input.getText().toString().trim(); if (s.isEmpty()) return;
        input.setText("");
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        command(s);
    }

    private void command(String s) {
        if (s == null || s.trim().isEmpty()) return;
        userLine.setText("ВЫ  •  " + s);
        jarvisLine.setText("JARVIS  •  анализирую запрос…");
        setState("ОБРАБОТКА");
        engine.handle(s);
    }

    private void speak(String s) {
        runOnUiThread(() -> {
            jarvisLine.setText("JARVIS  •  " + s);
            setState("ОТВЕЧАЮ");
        });
        if (tts != null) {
            speaking = true;
            tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply");
        }
    }

    private void setState(String s) {
        runOnUiThread(() -> {
            if (status != null) status.setText(s);
            if (core != null) core.setState(s);
            if (micLabel != null) {
                if (s.contains("СЛУША")) micLabel.setText("СЛУШАЮ ВАС…");
                else if (s.contains("ИЩУ")) micLabel.setText("ИЩУ АКТУАЛЬНУЮ ИНФОРМАЦИЮ В СЕТИ…");
                else if (s.contains("ОБРАБОТ")) micLabel.setText("АНАЛИЗИРУЮ ЗАПРОС…");
                else if (s.contains("ОТВЕЧ")) micLabel.setText("JARVIS ОТВЕЧАЕТ");
                else micLabel.setText("НАЖМИТЕ НА ЯДРО ИЛИ СКАЖИТЕ «ДЖАРВИС»");
            }
        });
    }

    @Override public void onRequestPermissionsResult(int r, String[] p, int[] g) {
        super.onRequestPermissionsResult(r, p, g);
        if (r == REQ_MIC && g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED) listen();
    }

    @Override protected void onDestroy() {
        if (recognizer != null) try { recognizer.destroy(); } catch (Throwable ignored) {}
        if (tts != null) { try { tts.stop(); } catch (Throwable ignored) {} try { tts.shutdown(); } catch (Throwable ignored) {} }
        if (engine != null) engine.shutdown();
        super.onDestroy();
    }
}
