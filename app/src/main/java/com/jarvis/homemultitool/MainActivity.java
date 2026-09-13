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
import android.view.View;
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

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private TextView iconButton(String glyph, View.OnClickListener listener) {
        TextView v = text(glyph, 24);
        v.setGravity(Gravity.CENTER);
        v.setTextColor(WHITE);
        v.setBackground(shape(0xFF123B5A, 0xAA071522, dp(30)));
        v.setOnClickListener(listener);
        return v;
    }

    private TextView pill(String label, int color) {
        TextView v = text(label, 11);
        v.setGravity(Gravity.CENTER);
        v.setTextColor(color);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setLetterSpacing(.08f);
        v.setBackground(shape(0xFF123D5C, 0xCC061421, dp(22)));
        return v;
    }

    private void buildUi() {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(BG);
        frame.setPadding(dp(16), 0, dp(16), 0);

        // Top command rail. It deliberately stays compact so the JARVIS core owns the screen.
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView menu = iconButton("≡", v -> Toast.makeText(this, "Меню JARVIS", Toast.LENGTH_SHORT).show());
        header.addView(menu, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView brand = text("J A R V I S", 24);
        brand.setTextColor(CYAN); brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD); brand.setGravity(Gravity.CENTER);
        brand.setLetterSpacing(.16f);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, dp(48), 1f); bp.setMargins(dp(8),0,dp(8),0);
        header.addView(brand, bp);
        TextView settings = iconButton("⌘", v -> startActivity(new Intent(this, SettingsActivity.class)));
        header.addView(settings, new LinearLayout.LayoutParams(dp(48), dp(48)));
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-1, dp(64), Gravity.TOP); hp.topMargin = dp(12);
        frame.addView(header, hp);

        // Small status rail under the header.
        LinearLayout rail = new LinearLayout(this); rail.setGravity(Gravity.CENTER_VERTICAL);
        status = micro("ГОТОВ"); status.setTextSize(11); status.setTextColor(CYAN);
        rail.addView(status, new LinearLayout.LayoutParams(0, dp(28), 1f));
        online = micro("●  ПРОВЕРКА СЕТИ"); online.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        rail.addView(online, new LinearLayout.LayoutParams(0, dp(28), 1f));
        FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(-1, dp(28), Gravity.TOP); rp.topMargin = dp(82);
        frame.addView(rail, rp); updateNetworkState();

        // Central HUD. The visual core is intentionally isolated from controls.
        FrameLayout coreHolder = new FrameLayout(this);
        core = new JarvisCoreView(this); core.setState("ГОТОВ"); core.startPulse(); core.setOnClickListener(v -> listen());
        coreHolder.addView(core, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-1, dp(270), Gravity.TOP); cp.topMargin = dp(112);
        frame.addView(coreHolder, cp);

        TextView coreStatus = text("●  СИСТЕМА ГОТОВА", 13); coreStatus.setTextColor(CYAN); coreStatus.setGravity(Gravity.CENTER); coreStatus.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        FrameLayout.LayoutParams csp = new FrameLayout.LayoutParams(-1, dp(28), Gravity.TOP); csp.topMargin = dp(362);
        frame.addView(coreStatus, csp);

        // Response surface — no permanent giant chat box.
        LinearLayout response = new LinearLayout(this); response.setOrientation(LinearLayout.VERTICAL); response.setPadding(dp(18),dp(14),dp(18),dp(14));
        response.setBackground(shape(0xFF174B70, 0xE605111D, dp(24)));
        userLine = micro("ГОТОВ К ЗАПРОСУ"); response.addView(userLine, new LinearLayout.LayoutParams(-1, dp(22)));
        jarvisLine = text("Сэр, я готов. Нажмите на ядро или скажите «Джарвис». ", 15); jarvisLine.setTypeface(Typeface.DEFAULT, Typeface.BOLD); jarvisLine.setMaxLines(3);
        response.addView(jarvisLine, new LinearLayout.LayoutParams(-1, -2));
        FrameLayout.LayoutParams resp = new FrameLayout.LayoutParams(-1, dp(88), Gravity.BOTTOM); resp.bottomMargin = dp(160);
        frame.addView(response, resp);

        // Voice command controls.
        LinearLayout commandBar = new LinearLayout(this); commandBar.setGravity(Gravity.CENTER_VERTICAL);
        input = new EditText(this); input.setSingleLine(true); input.setTextColor(WHITE); input.setHintTextColor(MUTED); input.setTextSize(15); input.setHint("Спросить JARVIS…"); input.setPadding(dp(18),0,dp(10),0); input.setBackground(shape(0xFF15527A,0xE6091927,dp(27))); input.setImeOptions(6);
        commandBar.addView(input, new LinearLayout.LayoutParams(0, dp(54), 1f));
        TextView send = iconButton("›", v -> sendText()); send.setTextSize(32); LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(54),dp(54)); sp.setMargins(dp(8),0,0,0); commandBar.addView(send,sp);
        FrameLayout.LayoutParams cmdp = new FrameLayout.LayoutParams(-1, dp(54), Gravity.BOTTOM); cmdp.bottomMargin = dp(96);
        frame.addView(commandBar,cmdp);

        LinearLayout actionRow = new LinearLayout(this); actionRow.setGravity(Gravity.CENTER); actionRow.setPadding(0,0,0,0);
        TextView mic = pill("●  ГОВОРИТЬ", CYAN); mic.setTextSize(13); mic.setOnClickListener(v -> listen());
        actionRow.addView(mic, new LinearLayout.LayoutParams(0,dp(52),1f));
        TextView stop = pill("■  СТОП", WHITE); LinearLayout.LayoutParams stp=new LinearLayout.LayoutParams(dp(92),dp(52)); stp.setMargins(dp(8),0,0,0); actionRow.addView(stop,stp); stop.setOnClickListener(v->stopAll());
        FrameLayout.LayoutParams arp = new FrameLayout.LayoutParams(-1,dp(52),Gravity.BOTTOM); arp.bottomMargin=dp(36+60); frame.addView(actionRow,arp);

        // Bottom navigation: visually integrated, not a row of random Android buttons.
        LinearLayout nav = new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setPadding(dp(8),dp(7),dp(8),dp(5)); nav.setBackground(shape(0xFF123B5A,0xF2071019,dp(28)));
        addNavItem(nav,"◉","JARVIS",true, v->{}); addNavItem(nav,"◌","Память",false,v->command("что ты помнишь")); addNavItem(nav,"⌁","Инструменты",false,v->Toast.makeText(this,"Инструменты доступны голосом",Toast.LENGTH_SHORT).show()); addNavItem(nav,"⌘","Настройки",false,v->startActivity(new Intent(this,SettingsActivity.class)));
        FrameLayout.LayoutParams np=new FrameLayout.LayoutParams(-1,dp(76),Gravity.BOTTOM); np.bottomMargin=dp(8); frame.addView(nav,np);

        micLabel = text("Нажмите на ядро или скажите «Джарвис»", 11); micLabel.setTextColor(MUTED); micLabel.setGravity(Gravity.CENTER); micLabel.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        FrameLayout.LayoutParams mlp=new FrameLayout.LayoutParams(-1,dp(24),Gravity.BOTTOM); mlp.bottomMargin=dp(168); frame.addView(micLabel,mlp);

        setContentView(frame);
    }

    private void addNavItem(LinearLayout nav,String icon,String title,boolean active,View.OnClickListener click){
        LinearLayout item=new LinearLayout(this); item.setOrientation(LinearLayout.VERTICAL); item.setGravity(Gravity.CENTER); item.setOnClickListener(click);
        TextView i=text(icon,22); i.setGravity(Gravity.CENTER); i.setTextColor(active?CYAN:MUTED); item.addView(i,new LinearLayout.LayoutParams(-1,dp(30)));
        TextView t=text(title,10); t.setGravity(Gravity.CENTER); t.setTextColor(active?WHITE:MUTED); t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); item.addView(t,new LinearLayout.LayoutParams(-1,dp(22)));
        nav.addView(item,new LinearLayout.LayoutParams(0,dp(64),1f));
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
                        if (!v.getLocale().getLanguage().equalsIgnoreCase("ru")) continue;
                        // Prefer the highest-quality Russian voice. Network voices are allowed
                        // when the installed TTS engine provides them; they are often much more natural.
                        if (best == null || v.getQuality() > best.getQuality() ||
                                (v.getQuality() == best.getQuality() && v.isNetworkConnectionRequired() && !best.isNetworkConnectionRequired())) best = v;
                    }
                }
                if (best != null) tts.setVoice(best);
                // A calmer rate/pitch sounds less synthetic on common Android TTS engines.
                tts.setSpeechRate(.88f);
                tts.setPitch(.82f);
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
