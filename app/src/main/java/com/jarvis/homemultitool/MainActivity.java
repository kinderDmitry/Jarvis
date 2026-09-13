package com.jarvis.homemultitool;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import java.util.ArrayList;
import java.util.Locale;

/** Premium, voice-first JARVIS surface. Settings stay off the main screen. */
public class MainActivity extends Activity {
    private static final int BG=Color.rgb(2,7,13), PANEL=Color.rgb(5,15,26), BLUE=Color.rgb(31,163,255), CYAN=Color.rgb(102,230,255), TEXT=Color.WHITE, MUTED=Color.rgb(126,161,190), GREEN=Color.rgb(64,238,178);
    private static final int REQ_MIC=42;
    private TextView status,userLine,jarvisLine,online;
    private EditText input;
    private SpeechRecognizer recognizer;
    private Intent recognizerIntent;
    private TextToSpeech tts;
    private JarvisCoreView core;
    private JarvisEngine engine;

    @Override protected void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        boolean lock=getIntent().getBooleanExtra("LOCKSCREEN_ASSIST",false)||getIntent().getBooleanExtra("WAKE_WORD",false);
        if(lock)getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        buildUi();
        engine=new JarvisEngine(this,new JarvisEngine.Callback(){public void reply(String s){speak(s);} public void state(String s){setState(s);}});
        initTts(); initSpeech(); if(lock)new Handler(Looper.getMainLooper()).postDelayed(this::listen,250);
    }
    private TextView tv(String s,float z){TextView v=new TextView(this);v.setText(s);v.setTextColor(TEXT);v.setTextSize(z);return v;}
    private GradientDrawable shape(int stroke,int fill,float radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(radius);g.setStroke(1,stroke);return g;}
    private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(l,t,r,b);return p;}
    private TextView label(String s){TextView v=tv(s,10);v.setTextColor(MUTED);v.setLetterSpacing(.14f);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}

    private void buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(18,12,18,18);
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo=tv("JARVIS",24);logo.setTextColor(CYAN);logo.setTypeface(Typeface.DEFAULT,Typeface.BOLD);head.addView(logo,lp(0,50,0,0,0,0));((LinearLayout.LayoutParams)logo.getLayoutParams()).weight=1;
        status=tv("ГОТОВ",10);status.setTextColor(MUTED);status.setGravity(Gravity.CENTER);head.addView(status,lp(70,38,0,0,4,0));
        online=tv("● ONLINE",10);online.setTextColor(GREEN);online.setGravity(Gravity.CENTER);head.addView(online,lp(82,38,0,0,4,0));
        TextView settings=tv("⚙",24);settings.setTextColor(CYAN);settings.setGravity(Gravity.CENTER);settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));head.addView(settings,lp(48,48,0,0,0,0));
        root.addView(head,lp(-1,52,0,0,0,0));
        LinearLayout sub=new LinearLayout(this);sub.setGravity(Gravity.CENTER_VERTICAL);TextView s=label("PERSONAL AI ASSISTANT");sub.addView(s);root.addView(sub,lp(-1,25,2,0,0,2));

        FrameLayout coreFrame=new FrameLayout(this);core=new JarvisCoreView(this);core.setOnClickListener(v->listen());coreFrame.addView(core,new FrameLayout.LayoutParams(-1,350));
        TextView center=tv("TAP TO SPEAK",11);center.setTextColor(CYAN);center.setGravity(Gravity.CENTER);center.setTypeface(Typeface.DEFAULT,Typeface.BOLD);center.setLetterSpacing(.18f);FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,40,Gravity.BOTTOM);cp.bottomMargin=8;coreFrame.addView(center,cp);
        root.addView(coreFrame,lp(-1,350,0,2,0,0));

        LinearLayout chat=new LinearLayout(this);chat.setOrientation(LinearLayout.VERTICAL);chat.setPadding(16,14,16,14);chat.setBackground(shape(0xFF124A76,PANEL,26));
        userLine=tv("",12);userLine.setTextColor(MUTED);jarvisLine=tv("JARVIS готов.",15);jarvisLine.setTypeface(Typeface.DEFAULT,Typeface.BOLD);chat.addView(userLine);chat.addView(jarvisLine,lp(-1,-2,0,7,0,0));root.addView(chat,lp(-1,-2,0,4,0,10));

        LinearLayout inputRow=new LinearLayout(this);inputRow.setGravity(Gravity.CENTER_VERTICAL);input=new EditText(this);input.setSingleLine(true);input.setTextColor(TEXT);input.setHintTextColor(MUTED);input.setTextSize(14);input.setHint("Спросите что-нибудь…");input.setPadding(16,0,12,0);input.setBackground(shape(0xFF15577E,0xFF071725,28));inputRow.addView(input,lp(0,56,0,0,8,0));
        Button send=smallButton("➤");send.setTextSize(18);send.setOnClickListener(v->sendText());inputRow.addView(send,lp(58,56,0,0,0,0));root.addView(inputRow,lp(-1,56,0,0,0,10));

        LinearLayout micRow=new LinearLayout(this);micRow.setGravity(Gravity.CENTER);Button mic=primary("◉  ГОВОРИТЬ");mic.setOnClickListener(v->listen());micRow.addView(mic,lp(0,52,0,0,0,0));((LinearLayout.LayoutParams)mic.getLayoutParams()).weight=1;Button stop=smallButton("■");stop.setOnClickListener(v->stopAll());micRow.addView(stop,lp(58,52,8,0,0,0));root.addView(micRow,lp(-1,52,0,0,0,10));

        LinearLayout chips=new LinearLayout(this);chips.setGravity(Gravity.CENTER);String[] q={"Время","Погода","Новости","Таймер"};for(String x:q){Button b=smallButton(x);b.setOnClickListener(v->command(x.equals("Время")?"который час":x.equals("Таймер")?"таймер":x));chips.addView(b,lp(0,42,3,0,3,0));((LinearLayout.LayoutParams)b.getLayoutParams()).weight=1;}root.addView(chips,lp(-1,42,0,0,0,8));
        TextView hint=tv("JARVIS умеет искать актуальную информацию в интернете",10);hint.setTextColor(MUTED);hint.setGravity(Gravity.CENTER);root.addView(hint,lp(-1,28,0,0,0,0));
        scroll.addView(root);setContentView(scroll);
    }
    private Button smallButton(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(11);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(shape(0xFF124A76,0xFF061522,22));return b;}
    private Button primary(String s){Button b=smallButton(s);b.setTextSize(13);b.setTextColor(CYAN);b.setBackground(shape(0xFF1D9DFF,0xFF071A2A,28));return b;}
    private void initTts(){tts=new TextToSpeech(this,r->{if(r==TextToSpeech.SUCCESS){tts.setLanguage(new Locale("ru","RU"));tts.setSpeechRate(.94f);tts.setPitch(.82f);}});}
    private void initSpeech(){if(!SpeechRecognizer.isRecognitionAvailable(this))return;try{if(Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this))recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(this);else recognizer=SpeechRecognizer.createSpeechRecognizer(this);}catch(Throwable t){recognizer=null;}if(recognizer==null)return;
        recognizer.setRecognitionListener(new RecognitionListener(){public void onReadyForSpeech(Bundle b){setState("СЛУШАЮ");}public void onBeginningOfSpeech(){}public void onRmsChanged(float v){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){setState("ОБРАБОТКА");}public void onError(int e){setState("ГОТОВ");}public void onResults(Bundle b){ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty())command(r.get(0));else setState("ГОТОВ");}public void onPartialResults(Bundle b){}public void onEvent(int a,Bundle b){}});
        recognizerIntent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ru-RU");recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);
    }
    private void listen(){if(recognizer==null){setState("НЕТ РЕЧИ");return;}if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_MIC);return;}try{recognizer.startListening(recognizerIntent);setState("СЛУШАЮ");}catch(Throwable t){setState("ГОТОВ");}}
    private void stopAll(){if(recognizer!=null){try{recognizer.stopListening();}catch(Throwable ignored){}try{recognizer.cancel();}catch(Throwable ignored){}}if(tts!=null)try{tts.stop();}catch(Throwable ignored){}setState("ГОТОВ");}
    private void sendText(){String s=input.getText().toString().trim();if(s.isEmpty())return;input.setText("");((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(),0);command(s);}
    private void command(String s){userLine.setText("ВЫ  ·  "+s);jarvisLine.setText("JARVIS  ·  анализирую…");engine.handle(s);}
    private void speak(String s){runOnUiThread(()->{jarvisLine.setText("JARVIS  ·  "+s);setState("ГОВОРЮ");});if(tts!=null)tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"jarvis_reply");}
    private void setState(String s){runOnUiThread(()->{statusText(s);if(core!=null)core.setState(s);});}
    private void statusText(String s){if(status!=null)status.setText(s);}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_MIC&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)listen();}
    @Override protected void onDestroy(){if(recognizer!=null)try{recognizer.destroy();}catch(Throwable ignored){}if(tts!=null){try{tts.stop();}catch(Throwable ignored){}try{tts.shutdown();}catch(Throwable ignored){}}if(engine!=null)engine.shutdown();super.onDestroy();}
}
