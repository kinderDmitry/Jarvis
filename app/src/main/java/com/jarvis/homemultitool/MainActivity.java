package com.jarvis.homemultitool;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.content.Context;
import android.widget.*;
import java.util.ArrayList;
import java.util.Locale;

/** JARVIS: voice-first assistant surface. Designed for phones, including lock-screen invocation. */
public class MainActivity extends Activity {
    private static final int BG=Color.rgb(2,7,13), PANEL=Color.rgb(5,15,26), PANEL2=Color.rgb(7,21,35);
    private static final int BLUE=Color.rgb(31,163,255), CYAN=Color.rgb(102,230,255), TEXT=Color.WHITE;
    private static final int MUTED=Color.rgb(126,161,190), GREEN=Color.rgb(64,238,178);
    private static final int REQ_MIC=42;
    private TextView status, userLine, jarvisLine, online, coreHint;
    private EditText input;
    private SpeechRecognizer recognizer;
    private Intent recognizerIntent;
    private TextToSpeech tts;
    private JarvisCoreView core;
    private JarvisEngine engine;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        boolean lock=getIntent().getBooleanExtra("LOCKSCREEN_ASSIST",false)||getIntent().getBooleanExtra("WAKE_WORD",false);
        if(lock) getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        buildUi();
        engine=new JarvisEngine(this,new JarvisEngine.Callback(){
            public void reply(String s){speak(s);} public void state(String s){setState(s);}
        });
        initTts(); initSpeech();
        String wakeQuery=getIntent().getStringExtra("WAKE_QUERY");
        if(wakeQuery!=null&&!wakeQuery.trim().isEmpty()) new Handler(Looper.getMainLooper()).postDelayed(()->command(wakeQuery),180);
        else if(lock) new Handler(Looper.getMainLooper()).postDelayed(this::listen,300);
    }

    private TextView tv(String s,float z){TextView v=new TextView(this);v.setText(s);v.setTextColor(TEXT);v.setTextSize(z);return v;}
    private GradientDrawable shape(int stroke,int fill,float radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(radius);if(stroke!=0)g.setStroke(1,stroke);return g;}
    private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(l,t,r,b);return p;}
    private TextView label(String s){TextView v=tv(s,10);v.setTextColor(MUTED);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setLetterSpacing(.13f);return v;}

    private void buildUi(){
        final LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG); root.setPadding(18,8,18,12);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){WindowInsets i=insets;android.graphics.Insets x=i.getInsets(WindowInsets.Type.systemBars());v.setPadding(18,Math.max(8,x.top+4),18,Math.max(12,x.bottom+4));}
            return insets;
        });
        if(Build.VERSION.SDK_INT>=30) root.requestApplyInsets();

        LinearLayout head=new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.VERTICAL);brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo=tv("JARVIS",24);logo.setTextColor(CYAN);logo.setTypeface(Typeface.DEFAULT,Typeface.BOLD);brand.addView(logo,lp(-1,29,0,0,0,0));
        TextView subtitle=label("PERSONAL AI ASSISTANT");brand.addView(subtitle,lp(-1,18,0,0,0,0));
        head.addView(brand,lp(0,52,0,0,0,0));((LinearLayout.LayoutParams)brand.getLayoutParams()).weight=1;
        online=tv("●  ПРОВЕРКА",10);online.setGravity(Gravity.CENTER);online.setTextColor(MUTED);online.setBackground(shape(0x2240EEB2,0x11050F1A,18));head.addView(online,lp(84,34,0,0,7,0));
        TextView settings=tv("⚙",23);settings.setTextColor(TEXT);settings.setGravity(Gravity.CENTER);settings.setBackground(shape(0xFF173650,0x00000000,18));settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));head.addView(settings,lp(42,42,0,0,0,0));
        root.addView(head,lp(-1,54,0,0,0,4));
        updateNetworkState();

        LinearLayout stateRow=new LinearLayout(this);stateRow.setGravity(Gravity.CENTER_VERTICAL);
        status=tv("ГОТОВ",11);status.setTextColor(CYAN);status.setTypeface(Typeface.DEFAULT,Typeface.BOLD);stateRow.addView(status,lp(0,28,0,0,0,0));((LinearLayout.LayoutParams)status.getLayoutParams()).weight=1;
        TextView live=label("ГОЛОС  •  ИНТЕРНЕТ  •  КОМАНДЫ");live.setGravity(Gravity.RIGHT);stateRow.addView(live,lp(0,28,0,0,0,0));((LinearLayout.LayoutParams)live.getLayoutParams()).weight=1;
        root.addView(stateRow,lp(-1,28,0,0,0,0));

        FrameLayout coreFrame=new FrameLayout(this);core=new JarvisCoreView(this);core.setOnClickListener(v->listen());core.startPulse();coreFrame.addView(core,new FrameLayout.LayoutParams(-1,-1));
        coreHint=tv("НАЖМИТЕ, ЧТОБЫ ГОВОРИТЬ",10);coreHint.setTextColor(MUTED);coreHint.setGravity(Gravity.CENTER);coreHint.setTypeface(Typeface.DEFAULT,Typeface.BOLD);coreHint.setLetterSpacing(.12f);
        FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-1,30,Gravity.BOTTOM);hp.bottomMargin=3;coreFrame.addView(coreHint,hp);root.addView(coreFrame,lp(-1,282,0,0,0,0));

        LinearLayout conversation=new LinearLayout(this);conversation.setOrientation(LinearLayout.VERTICAL);conversation.setPadding(15,12,15,12);conversation.setBackground(shape(0xFF173E5C,PANEL,20));
        userLine=tv("",11);userLine.setTextColor(MUTED);conversation.addView(userLine,lp(-1,-2,0,0,0,2));
        jarvisLine=tv("Готов к работе. Скажите, что нужно сделать.",14);jarvisLine.setTypeface(Typeface.DEFAULT,Typeface.BOLD);jarvisLine.setMaxLines(3);conversation.addView(jarvisLine,lp(-1,-2,0,2,0,0));
        root.addView(conversation,lp(-1,-2,0,5,0,8));

        LinearLayout inputRow=new LinearLayout(this);inputRow.setGravity(Gravity.CENTER_VERTICAL);
        input=new EditText(this);input.setSingleLine(true);input.setTextColor(TEXT);input.setHintTextColor(MUTED);input.setTextSize(14);input.setHint("Спросить JARVIS…");input.setPadding(16,0,12,0);input.setBackground(shape(0xFF1A537A,PANEL2,25));
        input.setImeOptions(6);inputRow.addView(input,lp(0,52,0,0,8,0));((LinearLayout.LayoutParams)input.getLayoutParams()).weight=1;
        Button send=button("➤",CYAN);send.setOnClickListener(v->sendText());inputRow.addView(send,lp(52,52,0,0,0,0));root.addView(inputRow,lp(-1,52,0,0,0,7));

        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER_VERTICAL);
        Button mic=button("◉  ГОВОРИТЬ",CYAN);mic.setOnClickListener(v->listen());controls.addView(mic,lp(0,48,0,0,7,0));((LinearLayout.LayoutParams)mic.getLayoutParams()).weight=1;
        Button stop=button("■",TEXT);stop.setOnClickListener(v->stopAll());controls.addView(stop,lp(48,48,0,0,0,0));root.addView(controls,lp(-1,48,0,0,0,7));

        LinearLayout quick=new LinearLayout(this);quick.setGravity(Gravity.CENTER);String[] q={"Время","Погода","Новости","Таймер"};for(String x:q){Button b=button(x,TEXT);b.setTextSize(11);b.setOnClickListener(v->command(x.equals("Время")?"который час":x.equals("Таймер")?"таймер":x));quick.addView(b,lp(0,38,3,0,3,0));((LinearLayout.LayoutParams)b.getLayoutParams()).weight=1;}root.addView(quick,lp(-1,38,0,0,0,0));

        setContentView(root);
    }

    private Button button(String s,int color){Button b=new Button(this);b.setText(s);b.setTextColor(color);b.setTextSize(12);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setPadding(4,0,4,0);b.setBackground(shape(0xFF173E5C,0xFF071624,22));return b;}

    private void updateNetworkState(){
        try {
            ConnectivityManager cm=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            Network n=cm==null?null:cm.getActiveNetwork();
            boolean connected=n!=null && cm.getNetworkCapabilities(n)!=null;
            online.setText(connected?"●  В СЕТИ":"●  НЕТ СЕТИ");
            online.setTextColor(connected?GREEN:Color.rgb(255,110,110));
        } catch(Throwable ignored) {}
    }

    private void initTts(){tts=new TextToSpeech(this,r->{if(r==TextToSpeech.SUCCESS){tts.setLanguage(new Locale("ru","RU"));tts.setSpeechRate(.94f);tts.setPitch(.82f);}});}
    private void initSpeech(){
        if(!SpeechRecognizer.isRecognitionAvailable(this))return;
        try{if(Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this))recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(this);else recognizer=SpeechRecognizer.createSpeechRecognizer(this);}catch(Throwable t){recognizer=null;}
        if(recognizer==null)return;
        recognizer.setRecognitionListener(new RecognitionListener(){
            public void onReadyForSpeech(Bundle b){setState("СЛУШАЮ");}
            public void onBeginningOfSpeech(){}
            public void onRmsChanged(float v){}
            public void onBufferReceived(byte[] b){}
            public void onEndOfSpeech(){setState("ОБРАБОТКА");}
            public void onError(int e){setState("ГОТОВ");}
            public void onResults(Bundle b){ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty())command(r.get(0));else setState("ГОТОВ");}
            public void onPartialResults(Bundle b){}
            public void onEvent(int a,Bundle b){}
        });
        recognizerIntent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ru-RU");recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);
    }
    private void listen(){if(recognizer==null){setState("РЕЧЬ НЕДОСТУПНА");return;}if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_MIC);return;}try{recognizer.startListening(recognizerIntent);setState("СЛУШАЮ");}catch(Throwable t){setState("ГОТОВ");}}
    private void stopAll(){if(recognizer!=null){try{recognizer.stopListening();}catch(Throwable ignored){}try{recognizer.cancel();}catch(Throwable ignored){}}if(tts!=null)try{tts.stop();}catch(Throwable ignored){}setState("ГОТОВ");}
    private void sendText(){String s=input.getText().toString().trim();if(s.isEmpty())return;input.setText("");((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(),0);command(s);}
    private void command(String s){if(s==null||s.trim().isEmpty())return;userLine.setText("ВЫ  •  "+s);jarvisLine.setText("JARVIS  •  обрабатываю запрос…");engine.handle(s);}
    private void speak(String s){runOnUiThread(()->{jarvisLine.setText("JARVIS  •  "+s);setState("ОТВЕЧАЮ");});if(tts!=null)tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"jarvis_reply");}
    private void setState(String s){runOnUiThread(()->{if(status!=null)status.setText(s);if(core!=null)core.setState(s);if(coreHint!=null)coreHint.setText(s.contains("СЛУША")?"ГОВОРИТЕ…":"НАЖМИТЕ, ЧТОБЫ ГОВОРИТЬ");});}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_MIC&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)listen();}
    @Override protected void onDestroy(){if(recognizer!=null)try{recognizer.destroy();}catch(Throwable ignored){}if(tts!=null){try{tts.stop();}catch(Throwable ignored){}try{tts.shutdown();}catch(Throwable ignored){}}if(engine!=null)engine.shutdown();super.onDestroy();}
}
