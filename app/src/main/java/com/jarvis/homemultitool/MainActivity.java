package com.jarvis.homemultitool;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.*;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;

/** JARVIS 5.14: responsive HUD, unified controls and reliable voice entry points. */
public class MainActivity extends Activity {
    private static final int BG=Color.rgb(2,6,10), SUR=Color.rgb(5,15,25), SUR2=Color.rgb(4,12,21);
    private static final int CYAN=Color.rgb(83,220,255), BORDER=Color.rgb(24,91,139), WHITE=Color.rgb(242,247,255);
    private static final int MUTED=Color.rgb(121,153,180), GREEN=Color.rgb(71,232,177), RED=Color.rgb(255,102,116);
    private static final int REQ_MIC=42, REQ_NOTIFY=43;
    private TextView status,replyLabel,replyText,networkLabel,voiceHint;
    private EditText input;
    private SpeechRecognizer recognizer; private Intent recognizerIntent; private TextToSpeech tts;
    private JarvisCoreView core; private JarvisEngine engine;
    private boolean speaking;
    private final Handler main=new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle b){
        super.onCreate(b); applyImmersive(); getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        boolean lock=getIntent().getBooleanExtra("LOCKSCREEN_ASSIST",false)||getIntent().getBooleanExtra("WAKE_WORD",false);
        if(lock)getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        buildUi();
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIFY);
        engine=new JarvisEngine(this,new JarvisEngine.Callback(){public void reply(String s){speak(s);}public void state(String s){setState(s);}});
        initTts(); initSpeech();
        String q=getIntent().getStringExtra("WAKE_QUERY");
        if(q!=null&&!q.trim().isEmpty())main.postDelayed(()->command(q),350);
        else if(lock)main.postDelayed(this::listen,650);
    }

    private void applyImmersive(){
        try{
            if(Build.VERSION.SDK_INT>=30){
                getWindow().setDecorFitsSystemWindows(false);
                WindowInsetsController c=getWindow().getInsetsController();
                if(c!=null){c.hide(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}
            } else getWindow().getDecorView().setSystemUiVisibility(5894|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }catch(Throwable ignored){}
    }
    private int dp(float x){return(int)(x*getResources().getDisplayMetrics().density+.5f);}
    private TextView tv(String s,float size){TextView v=new TextView(this);v.setText(s);v.setTextColor(WHITE);v.setTextSize(size);v.setGravity(Gravity.CENTER_VERTICAL);v.setIncludeFontPadding(true);return v;}
    private GradientDrawable box(int stroke,int fill,float radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));if(stroke!=0)g.setStroke(dp(1),stroke);return g;}
    private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}

    /** One visual language for every interactive control in every screen. */
    private TextView hudButton(String text,View.OnClickListener click){
        TextView v=tv(text,13);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setTextColor(WHITE);
        v.setBackground(box(BORDER,0xE6071724,18));v.setPadding(dp(7),0,dp(7),0);v.setOnClickListener(click);
        if(Build.VERSION.SDK_INT>=26)v.setAutoSizeTextTypeUniformWithConfiguration(dp(9),dp(13),1,1);
        return v;
    }
    private TextView topButton(String glyph,View.OnClickListener click){
        TextView v=tv(glyph,24);v.setGravity(Gravity.CENTER);v.setTextColor(CYAN);v.setBackground(box(BORDER,0xCC07131E,18));v.setOnClickListener(click);return v;
    }

    private void buildUi(){
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(BG);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(18),dp(8),dp(18),dp(88));
        root.addView(page,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(topButton("☰",v->showTools()),new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout center=new LinearLayout(this);center.setOrientation(LinearLayout.VERTICAL);center.setGravity(Gravity.CENTER);
        TextView brand=tv("J A R V I S",24);brand.setGravity(Gravity.CENTER);brand.setTextColor(CYAN);brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);brand.setLetterSpacing(.18f);
        center.addView(brand,new LinearLayout.LayoutParams(-1,dp(28)));
        TextView sub=tv("PERSONAL AI SYSTEM",9);sub.setGravity(Gravity.CENTER);sub.setTextColor(MUTED);sub.setLetterSpacing(.11f);center.addView(sub,new LinearLayout.LayoutParams(-1,dp(15)));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(48),1);cp.setMargins(dp(8),0,dp(8),0);header.addView(center,cp);
        header.addView(topButton("⚙",v->startActivity(new Intent(this,SettingsActivity.class))),new LinearLayout.LayoutParams(dp(48),dp(48)));
        page.addView(header,new LinearLayout.LayoutParams(-1,dp(50)));

        LinearLayout rail=new LinearLayout(this);rail.setGravity(Gravity.CENTER_VERTICAL);
        status=tv("ГОТОВ",11);status.setTextColor(CYAN);status.setTypeface(Typeface.DEFAULT,Typeface.BOLD);status.setLetterSpacing(.1f);rail.addView(status,new LinearLayout.LayoutParams(0,dp(26),1));
        networkLabel=tv("●  В СЕТИ",11);networkLabel.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);networkLabel.setTypeface(Typeface.DEFAULT,Typeface.BOLD);rail.addView(networkLabel,new LinearLayout.LayoutParams(0,dp(26),1));
        page.addView(rail,lp(-1,28,0,4,0,0));updateNetworkState();

        core=new JarvisCoreView(this);core.setState("ГОТОВ");core.startPulse();core.setClickable(true);core.setOnClickListener(v->listen());
        page.addView(core,new LinearLayout.LayoutParams(-1,0,1));
        TextView online=tv("●  JARVIS ONLINE",12);online.setGravity(Gravity.CENTER);online.setTextColor(CYAN);online.setTypeface(Typeface.DEFAULT,Typeface.BOLD);online.setLetterSpacing(.08f);
        page.addView(online,new LinearLayout.LayoutParams(-1,dp(24)));

        LinearLayout response=new LinearLayout(this);response.setOrientation(LinearLayout.VERTICAL);response.setPadding(dp(15),dp(8),dp(15),dp(8));response.setBackground(box(BORDER,0xEF06131F,21));
        replyLabel=tv("ГОТОВ К ЗАПРОСУ",10);replyLabel.setTextColor(MUTED);replyLabel.setTypeface(Typeface.DEFAULT,Typeface.BOLD);replyLabel.setLetterSpacing(.08f);response.addView(replyLabel,new LinearLayout.LayoutParams(-1,dp(17)));
        replyText=tv("Нажмите ядро или ГОВОРИТЬ",14);replyText.setTypeface(Typeface.DEFAULT,Typeface.BOLD);replyText.setMaxLines(2);replyText.setEllipsize(android.text.TextUtils.TruncateAt.END);response.addView(replyText,new LinearLayout.LayoutParams(-1,0,1));
        page.addView(response,new LinearLayout.LayoutParams(-1,dp(68)));

        LinearLayout voiceRow=new LinearLayout(this);voiceRow.setGravity(Gravity.CENTER);voiceRow.setPadding(0,dp(5),0,0);
        TextView speak=hudButton("◉  ГОВОРИТЬ",v->listen());voiceRow.addView(speak,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView stop=hudButton("■  СТОП",v->stopAll());LinearLayout.LayoutParams st=new LinearLayout.LayoutParams(0,dp(48),1);st.setMargins(dp(8),0,0,0);voiceRow.addView(stop,st);
        page.addView(voiceRow,new LinearLayout.LayoutParams(-1,dp(54)));
        voiceHint=tv("МИКРОФОН ГОТОВ  •  НАЖМИТЕ ГОВОРИТЬ",9);voiceHint.setGravity(Gravity.CENTER);voiceHint.setTextColor(MUTED);voiceHint.setTypeface(Typeface.DEFAULT,Typeface.BOLD);voiceHint.setLetterSpacing(.04f);page.addView(voiceHint,new LinearLayout.LayoutParams(-1,dp(19)));

        LinearLayout command=new LinearLayout(this);command.setGravity(Gravity.CENTER_VERTICAL);
        input=new EditText(this);input.setSingleLine(true);input.setTextColor(WHITE);input.setHintTextColor(MUTED);input.setTextSize(14);input.setHint("Спросить JARVIS…");input.setPadding(dp(15),0,dp(8),0);input.setBackground(box(BORDER,0xE6071624,18));
        command.addView(input,new LinearLayout.LayoutParams(0,dp(46),1));TextView send=hudButton("➤",v->sendText());send.setTextSize(19);command.addView(send,lp(dp(50),dp(46),8,0,0,0));page.addView(command,new LinearLayout.LayoutParams(-1,dp(48)));

        LinearLayout quick=new LinearLayout(this);quick.setGravity(Gravity.CENTER_VERTICAL);addQuick(quick,"ВРЕМЯ","сколько времени");addQuick(quick,"ПОГОДА","какая сейчас погода в Москве");addQuick(quick,"ТАЙМЕР","таймер на 5 минут");page.addView(quick,new LinearLayout.LayoutParams(-1,dp(40)));
        setContentView(root);

        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(7),dp(3),dp(7),dp(3));nav.setBackground(box(BORDER,0xF2071019,22));
        addNav(nav,"◉","JARVIS",true,v->{});addNav(nav,"◌","Память",false,v->command("что ты помнишь"));addNav(nav,"⌁","Инструменты",false,v->showTools());addNav(nav,"⚙","Настройки",false,v->startActivity(new Intent(this,SettingsActivity.class)));
        FrameLayout.LayoutParams np=new FrameLayout.LayoutParams(-1,dp(68),Gravity.BOTTOM);np.setMargins(dp(10),0,dp(10),dp(7));root.addView(nav,np);
    }
    private void addQuick(LinearLayout r,String title,String cmd){TextView b=hudButton(title,v->command(cmd));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(36),1);p.setMargins(dp(3),0,dp(3),0);r.addView(b,p);}
    private void addNav(LinearLayout n,String i,String t,boolean active,View.OnClickListener c){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setGravity(Gravity.CENTER);x.setOnClickListener(c);TextView iv=tv(i,20);iv.setGravity(Gravity.CENTER);iv.setTextColor(active?CYAN:MUTED);x.addView(iv,new LinearLayout.LayoutParams(-1,dp(26)));TextView label=tv(t,9);label.setGravity(Gravity.CENTER);label.setTypeface(Typeface.DEFAULT,Typeface.BOLD);label.setTextColor(active?WHITE:MUTED);label.setSingleLine(true);x.addView(label,new LinearLayout.LayoutParams(-1,dp(17)));n.addView(x,new LinearLayout.LayoutParams(0,dp(52),1));}

    private void showTools(){final Dialog d=new Dialog(this);LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(18),dp(16),dp(18),dp(16));l.setBackground(box(BORDER,0xFF06131F,24));TextView h=tv("JARVIS  •  ИНСТРУМЕНТЫ",18);h.setTextColor(CYAN);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);l.addView(h,new LinearLayout.LayoutParams(-1,dp(30)));String[] names={"⏱  Таймер","◷  Время","⌁  Погода","✦  Новости","▣  Калькулятор","☼  Фонарик","⚙  Настройки"};String[] cmds={"таймер на 5 минут","сколько времени","какая сейчас погода в Москве","новости сейчас","посчитай 125 плюс 8","включи фонарик",""};for(int k=0;k<names.length;k++){final int ix=k;TextView b=hudButton(names[k],v->{d.dismiss();if(ix==6)startActivity(new Intent(MainActivity.this,SettingsActivity.class));else command(cmds[ix]);});l.addView(b,lp(-1,46,0,6,0,0));}TextView close=hudButton("ЗАКРЫТЬ",v->d.dismiss());l.addView(close,lp(-1,46,0,12,0,0));d.setContentView(l);d.setOnShowListener(x->{Window w=d.getWindow();if(w!=null){WindowManager.LayoutParams a=w.getAttributes();a.width=(int)(getResources().getDisplayMetrics().widthPixels*.88);a.gravity=Gravity.CENTER;w.setAttributes(a);}});d.show();}
    private void updateNetworkState(){try{ConnectivityManager cm=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);Network n=cm==null?null:cm.getActiveNetwork();boolean ok=n!=null&&cm.getNetworkCapabilities(n)!=null;networkLabel.setText(ok?"●  В СЕТИ":"●  НЕТ СЕТИ");networkLabel.setTextColor(ok?GREEN:RED);}catch(Throwable ignored){}}

    private void initTts(){tts=new TextToSpeech(this,r->{if(r!=TextToSpeech.SUCCESS)return;try{Locale ru=new Locale("ru","RU");tts.setLanguage(ru);Voice best=null;for(Voice v:tts.getVoices()){if(!"ru".equalsIgnoreCase(v.getLocale().getLanguage()))continue;int q=v.getQuality();if(q>=Voice.QUALITY_NORMAL&&(best==null||q>best.getQuality()||(q==best.getQuality()&&v.isNetworkConnectionRequired()&&!best.isNetworkConnectionRequired())))best=v;}if(best!=null)tts.setVoice(best);tts.setSpeechRate(.90f);tts.setPitch(.92f);tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){public void onStart(String id){speaking=true;}public void onDone(String id){speaking=false;setState("ГОТОВ");}public void onError(String id){speaking=false;setState("ГОТОВ");}});}catch(Throwable ignored){}});}
    private void initSpeech(){
        if(!SpeechRecognizer.isRecognitionAvailable(this))return; recreateRecognizer();
        recognizerIntent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ru-RU");recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false);recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,false);
    }
    private void recreateRecognizer(){try{if(recognizer!=null)recognizer.destroy();recognizer=(Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this))?SpeechRecognizer.createOnDeviceSpeechRecognizer(this):SpeechRecognizer.createSpeechRecognizer(this);}catch(Throwable e){recognizer=null;}if(recognizer==null)return;recognizer.setRecognitionListener(new RecognitionListener(){public void onReadyForSpeech(Bundle b){setState("СЛУШАЮ");}public void onBeginningOfSpeech(){setState("СЛУШАЮ");}public void onRmsChanged(float v){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){setState("ОБРАБОТКА");}public void onError(int e){setState("ГОТОВ");}public void onResults(Bundle b){ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty())command(r.get(0));else setState("ГОТОВ");}public void onPartialResults(Bundle b){}public void onEvent(int a,Bundle b){}});}
    private void listen(){
        if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_MIC);return;}
        if(recognizer==null){recreateRecognizer();if(recognizer==null){setState("РЕЧЬ НЕДОСТУПНА");return;}}
        try{if(tts!=null)tts.stop();recognizer.cancel();recognizer.startListening(recognizerIntent);setState("СЛУШАЮ");}catch(Throwable t){main.postDelayed(()->{recreateRecognizer();try{if(recognizer!=null)recognizer.startListening(recognizerIntent);}catch(Throwable ignored){}},250);}
    }
    private void stopAll(){if(recognizer!=null){try{recognizer.stopListening();}catch(Throwable ignored){}try{recognizer.cancel();}catch(Throwable ignored){}}if(tts!=null)try{tts.stop();}catch(Throwable ignored){}speaking=false;setState("ГОТОВ");}
    private void sendText(){String s=input.getText().toString().trim();if(s.isEmpty())return;input.setText("");InputMethodManager im=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(im!=null)im.hideSoftInputFromWindow(input.getWindowToken(),0);command(s);}
    private void command(String s){if(s==null||s.trim().isEmpty())return;replyLabel.setText("ВЫ  •  "+s);replyText.setText("JARVIS  •  обрабатываю…");setState("ОБРАБОТКА");engine.handle(s);}
    private void speak(String s){runOnUiThread(()->{replyText.setText("JARVIS  •  "+s);setState("ОТВЕЧАЮ");if(tts!=null){speaking=true;tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"jarvis_reply");}});}
    private void setState(String s){runOnUiThread(()->{if(status!=null)status.setText(s);if(core!=null)core.setState(s);if(voiceHint!=null){if(s.contains("СЛУША"))voiceHint.setText("СЛУШАЮ ВАС…");else if(s.contains("ОБРАБОТ"))voiceHint.setText("АНАЛИЗИРУЮ ЗАПРОС…");else if(s.contains("ОТВЕЧ"))voiceHint.setText("JARVIS ОТВЕЧАЕТ…");else voiceHint.setText("МИКРОФОН ГОТОВ  •  НАЖМИТЕ ГОВОРИТЬ");}});}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_MIC&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)main.postDelayed(this::listen,200);}
    @Override protected void onResume(){super.onResume();applyImmersive();updateNetworkState();}
    private void restartWakeIfEnabled(){
        if(!getSharedPreferences("jarvis",0).getBoolean("wake_enabled",false))return;
        try{Intent i=new Intent(this,JarvisWakeWordService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}catch(Throwable ignored){}
    }
    @Override protected void onDestroy(){if(recognizer!=null)try{recognizer.destroy();}catch(Throwable ignored){}if(tts!=null){try{tts.stop();}catch(Throwable ignored){}try{tts.shutdown();}catch(Throwable ignored){}}if(engine!=null)engine.shutdown();restartWakeIfEnabled();super.onDestroy();}
}
