package com.jarvis.homemultitool;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;

/**
 * JARVIS single-screen voice assistant UI. The screen intentionally contains very little text:
 * the assistant is the interface, not a collection of feature cards.
 */
public class MainActivity extends Activity {
    private static final int BG = Color.rgb(1, 5, 11);
    private static final int BLUE = Color.rgb(18, 151, 255);
    private static final int CYAN = Color.rgb(92, 224, 255);
    private static final int TEXT = Color.WHITE;
    private static final int MUTED = Color.rgb(116, 151, 183);
    private static final int GREEN = Color.rgb(65, 240, 178);
    private static final int REQ_ROLE = 41;
    private static final int REQ_MIC = 42;

    private TextView status;
    private TextView userLine;
    private TextView jarvisLine;
    private TextView roleState;
    private EditText input;
    private SpeechRecognizer recognizer;
    private Intent recognizerIntent;
    private TextToSpeech tts;
    private JarvisCoreView core;
    private JarvisEngine engine;
    private boolean speaking;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        boolean lock = getIntent().getBooleanExtra("LOCKSCREEN_ASSIST", false)
                || getIntent().getBooleanExtra("WAKE_WORD", false);
        if (lock) {
            w.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        buildUi();
        engine = new JarvisEngine(this, new JarvisEngine.Callback() {
            @Override public void reply(String text) { speak(text); }
            @Override public void state(String value) { MainActivity.this.setState(value); }
        });
        initTts();
        initSpeech();
        if (lock) new Handler(Looper.getMainLooper()).postDelayed(this::listen, 300);
        refreshAssistantState();
    }

    private TextView text(String value, float size) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(TEXT);
        v.setTextSize(size);
        return v;
    }

    private GradientDrawable bg(int stroke, int fill) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{fill, 0xFF020A14});
        g.setCornerRadius(30);
        g.setStroke(1, stroke);
        return g;
    }

    private Button actionButton(String title) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextColor(TEXT);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(bg(0xFF124A76, 0xFF061525));
        return b;
    }

    private LinearLayout.LayoutParams lp(int width, int height, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(width, height);
        p.setMargins(l, t, r, b);
        return p;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(16, 8, 16, 18);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = text("J A R V I S", 24);
        logo.setTextColor(CYAN);
        logo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView settings = text("⚙", 24);
        settings.setTextColor(CYAN);
        settings.setGravity(Gravity.CENTER);
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        header.addView(logo, lp(0, 54, 0, 0, 0, 0));
        ((LinearLayout.LayoutParams) logo.getLayoutParams()).weight = 1f;
        header.addView(settings, lp(48, 54, 8, 0, 0, 0));
        root.addView(header, lp(-1, 54, 0, 0, 0, 0));

        LinearLayout stateRow = new LinearLayout(this);
        stateRow.setGravity(Gravity.CENTER);
        TextView dot = text("●", 10);
        dot.setTextColor(GREEN);
        status = text("  ГОТОВ  •  ЛОКАЛЬНО", 11);
        status.setTextColor(MUTED);
        stateRow.addView(dot);
        stateRow.addView(status);
        root.addView(stateRow, lp(-1, 28, 0, 0, 0, 2));

        core = new JarvisCoreView(this);
        core.setOnClickListener(v -> listen());
        root.addView(core, lp(-1, 330, 0, 2, 0, 0));

        TextView coreHint = text("НАЖМИТЕ И ГОВОРИТЕ", 11);
        coreHint.setTextColor(CYAN);
        coreHint.setGravity(Gravity.CENTER);
        coreHint.setLetterSpacing(.12f);
        root.addView(coreHint, lp(-1, 32, 0, -2, 0, 4));

        LinearLayout dialogue = new LinearLayout(this);
        dialogue.setOrientation(LinearLayout.VERTICAL);
        dialogue.setPadding(16, 13, 16, 13);
        dialogue.setBackground(bg(0xFF0B5689, 0xFF061322));
        userLine = text("", 12);
        userLine.setTextColor(MUTED);
        jarvisLine = text("", 15);
        jarvisLine.setTextColor(TEXT);
        jarvisLine.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        dialogue.addView(userLine);
        dialogue.addView(jarvisLine, lp(-1, -2, 0, 5, 0, 0));
        root.addView(dialogue, lp(-1, -2, 0, 5, 0, 8));

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        input = new EditText(this);
        input.setSingleLine(true);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setTextSize(14);
        input.setHint("Сообщение JARVIS");
        input.setPadding(16, 0, 12, 0);
        input.setBackground(bg(0xFF0E4770, 0xFF061321));
        inputRow.addView(input, lp(0, 54, 0, 0, 7, 0));
        Button send = actionButton("➤");
        send.setTextSize(18);
        send.setOnClickListener(v -> sendText());
        inputRow.addView(send, lp(58, 54, 0, 0, 0, 0));
        root.addView(inputRow, lp(-1, 54, 0, 0, 0, 8));

        LinearLayout quick = new LinearLayout(this);
        quick.setGravity(Gravity.CENTER);
        String[] commands = {"Время", "Таймер", "Память", "Камера"};
        for (String cmd : commands) {
            Button q = actionButton(cmd);
            q.setOnClickListener(v -> command(cmd));
            quick.addView(q, lp(0, 48, 3, 0, 3, 0));
            ((LinearLayout.LayoutParams) q.getLayoutParams()).weight = 1f;
        }
        root.addView(quick, lp(-1, 48, 0, 0, 0, 8));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER);
        Button stop = actionButton("■  Остановить ответ");
        stop.setOnClickListener(v -> stopListening());
        bottom.addView(stop, lp(-1, 46, 0, 0, 0, 0));
        root.addView(bottom);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void initTts() {
        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("ru", "RU"));
                tts.setSpeechRate(.94f);
                tts.setPitch(.82f);
            }
        });
    }

    private void initSpeech() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return;
        if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
        } else {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        }
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle b) { setState("СЛУШАЮ"); }
            @Override public void onBeginningOfSpeech() { }
            @Override public void onRmsChanged(float v) { }
            @Override public void onBufferReceived(byte[] b) { }
            @Override public void onEndOfSpeech() { setState("ОБРАБОТКА"); }
            @Override public void onError(int e) { setState("ГОТОВ"); }
            @Override public void onResults(Bundle b) {
                ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (r != null && !r.isEmpty()) command(r.get(0)); else setState("ГОТОВ");
            }
            @Override public void onPartialResults(Bundle b) { }
            @Override public void onEvent(int a, Bundle b) { }
        });
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
    }

    private void listen() {
        if (recognizer == null) {
            setState("НЕТ РЕЧИ");
            return;
        }
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            return;
        }
        try {
            recognizer.startListening(recognizerIntent);
            setState("СЛУШАЮ");
        } catch (Throwable ignored) {
            setState("ГОТОВ");
        }
    }

    private void stopListening() {
        if (recognizer != null) {
            try { recognizer.stopListening(); } catch (Throwable ignored) { }
            try { recognizer.cancel(); } catch (Throwable ignored) { }
        }
        stopService(new Intent(this, JarvisWakeWordService.class));
        if (tts != null) {
            try { tts.stop(); } catch (Throwable ignored) { }
        }
        setState("ГОТОВ");
    }

    private void sendText() {
        String s = input.getText().toString().trim();
        if (s.isEmpty()) return;
        input.setText("");
        command(s);
    }

    private void command(String s) {
        if (userLine != null) userLine.setText("ВЫ  •  " + s);
        if (jarvisLine != null) jarvisLine.setText("JARVIS  •  …");
        if (engine != null) engine.handle(s);
    }

    private void speak(String s) {
        runOnUiThread(() -> {
            if (jarvisLine != null) jarvisLine.setText("JARVIS  •  " + s);
            setState("ГОВОРЮ");
        });
        if (tts != null) {
            speaking = true;
            tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply");
        }
    }

    private void setState(String s) {
        runOnUiThread(() -> {
            if (status != null) status.setText("  " + s + "  •  ЛОКАЛЬНО");
            if (core != null) core.setState(s);
        });
    }

    private void requestAssistantRole() {
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                RoleManager rm = getSystemService(RoleManager.class);
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                    if (!rm.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                        startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT), REQ_ROLE);
                    } else {
                        refreshAssistantState();
                    }
                    return;
                }
            } catch (Throwable ignored) { }
        }
        try {
            startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS));
        } catch (Throwable ignored) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void refreshAssistantState() {
        boolean active = JarvisVoiceInteractionService.isActive(this);
        if (roleState != null) {
            roleState.setText(active ? "АКТИВЕН" : "НЕ ВЫБРАН");
            roleState.setTextColor(active ? GREEN : MUTED);
        }
    }

    private void startWakeWord() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            return;
        }
        Intent i = new Intent(this, JarvisWakeWordService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
            setState("ЖДУ «ДЖАРВИС»");
        } catch (Throwable ignored) {
            setState("ГОТОВ");
        }
    }

    @Override protected void onResume() {
        super.onResume();
        refreshAssistantState();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ROLE) new Handler(Looper.getMainLooper()).postDelayed(this::refreshAssistantState, 500);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MIC && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) listen();
    }

    @Override protected void onDestroy() {
        if (recognizer != null) { try { recognizer.destroy(); } catch (Throwable ignored) { } }
        if (tts != null) { try { tts.stop(); } catch (Throwable ignored) { } try { tts.shutdown(); } catch (Throwable ignored) { } }
        super.onDestroy();
    }
}
