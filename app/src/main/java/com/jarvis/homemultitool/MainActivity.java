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
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;

/** Premium JARVIS HUD. The visual hierarchy is intentionally identical across sections. */
public class MainActivity extends Activity {
    private static final int BG=Color.rgb(2,6,10), SURFACE=Color.rgb(4,14,24), SURFACE2=Color.rgb(5,19,32);
    private static final int CYAN=Color.rgb(91,224,255), BLUE=Color.rgb(22,105,168), WHITE=Color.rgb(242,247,255);
    private static final int MUTED=Color.rgb(126,157,184), GREEN=Color.rgb(71,232,177), RED=Color.rgb(255,102,116);
    private static final int REQ_MIC=42;
    private TextView status,replyLabel,replyText,networkLabel,voiceHint; private EditText input;
    private SpeechRecognizer recognizer; private Intent recognizerIntent; private TextToSpeech tts; private JarvisCoreView core; private JarvisEngine engine; private boolean speaking;

    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);boolean lock=getIntent().getBooleanExtra("LOCKSCREEN_ASSIST",false)||getIntent().getBooleanExtra("WAKE_WORD",false);if(lock)getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);buildUi();engine=new JarvisEngine(this,new JarvisEngine.Callback(){public void reply(String s){speak(s);}public void state(String s){setState(s);}});initTts();initSpeech();String q=getIntent().getStringExtra("WAKE_QUERY");if(q!=null&&!q.trim().isEmpty())new Handler(Looper.getMainLooper()).postDelayed(()->command(q),350);else if(lock)new Handler(Looper.getMainLooper()).postDelayed(this::listen,650);}

    private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}    
    private TextView tv(String s,float z){TextView v=new TextView(this);v.setText(s);v.setTextColor(WHITE);v.setTextSize(z);v.setFontFeatureSettings("kern");return v;}
    private GradientDrawable box(int stroke,int fill,float radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));if(stroke!=0)g.setStroke(dp(1),stroke);return g;}
    private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    private TextView icon(String s,View.OnClickListener c){TextView v=tv(s,25);v.setGravity(Gravity.CENTER);v.setTextColor(CYAN);v.setBackground(box(0xFF123D5C,0xB5081421,28));v.setOnClickListener(c);return v;}
    private TextView action(String s,View.OnClickListener c){TextView v=tv(s,14);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setTextColor(WHITE);v.setBackground(box(0xFF15547C,0xE6081826,26));v.setOnClickListener(c);return v;}

    private void buildUi(){
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(BG);root.setPadding(dp(16),dp(10),dp(16),0);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(-1,-1);pp.bottomMargin=dp(94);root.addView(page,pp);

        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(icon("≡",v->showTools()),new LinearLayout.LayoutParams(dp(50),dp(50)));
        TextView brand=tv("J A R V I S",25);brand.setTextColor(CYAN);brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);brand.setGravity(Gravity.CENTER);brand.setLetterSpacing(.16f);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(50),1);bp.setMargins(dp(10),0,dp(10),0);header.addView(brand,bp);
        header.addView(icon("⚙",v->startActivity(new Intent(this,SettingsActivity.class))),new LinearLayout.LayoutParams(dp(50),dp(50)));page.addView(header);

        LinearLayout rail=new LinearLayout(this);rail.setGravity(Gravity.CENTER_VERTICAL);status=tv("ГОТОВ",11);status.setTextColor(CYAN);status.setTypeface(Typeface.DEFAULT,Typeface.BOLD);status.setLetterSpacing(.1f);rail.addView(status,new LinearLayout.LayoutParams(0,dp(30),1));networkLabel=tv("●  В СЕТИ",11);networkLabel.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);networkLabel.setTextColor(GREEN);networkLabel.setTypeface(Typeface.DEFAULT,Typeface.BOLD);rail.addView(networkLabel,new LinearLayout.LayoutParams(0,dp(30),1));page.addView(rail,lp(-1,30,0,8,0,0));updateNetworkState();

        core=new JarvisCoreView(this);core.setState("ГОТОВ");core.startPulse();core.setOnClickListener(v->listen());page.addView(core,lp(-1,dp(270),0,6,0,0));
        TextView coreTitle=tv("●  JARVIS ONLINE",14);coreTitle.setGravity(Gravity.CENTER);coreTitle.setTextColor(CYAN);coreTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);page.addView(coreTitle,lp(-1,28,0,0,0,2));

        LinearLayout response=new LinearLayout(this);response.setOrientation(LinearLayout.VERTICAL);response.setPadding(dp(18),dp(14),dp(18),dp(14));response.setBackground(box(0xFF155078,0xEE06131F,24));replyLabel=tv("ГОТОВ К ЗАПРОСУ",11);replyLabel.setTextColor(MUTED);replyLabel.setTypeface(Typeface.DEFAULT,Typeface.BOLD);replyLabel.setLetterSpacing(.1f);response.addView(replyLabel);replyText=tv("На связи, сэр. Нажмите ядро и говорите.",15);replyText.setTypeface(Typeface.DEFAULT,Typeface.BOLD);replyText.setMaxLines(3);response.addView(replyText,lp(-1,-2,0,5,0,0));page.addView(response,lp(-1,dp(90),0,10,0,0));

        LinearLayout command=new LinearLayout(this);command.setGravity(Gravity.CENTER_VERTICAL);input=new EditText(this);input.setSingleLine(true);input.setTextColor(WHITE);input.setHintTextColor(MUTED);input.setTextSize(15);input.setHint("Спросить JARVIS…");input.setPadding(dp(16),0,dp(12),0);input.setBackground(box(0xFF155078,0xE6071624,26));command.addView(input,new LinearLayout.LayoutParams(0,dp(50),1));TextView send=action("➤",v->sendText());send.setTextSize(21);command.addView(send,lp(54,50,8,0,0,0));page.addView(command,lp(-1,50,0,10,0,0));

        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);TextView speak=action("◉  ГОВОРИТЬ",v->listen());TextView stop=action("■  СТОП",v->stopAll());controls.addView(speak,new LinearLayout.LayoutParams(0,52,1));controls.addView(stop,lp(0,52,8,0,0,0));((LinearLayout.LayoutParams)stop.getLayoutParams()).weight=1;page.addView(controls,lp(-1,52,0,8,0,0));
        voiceHint=tv("Нажмите на ядро или «Джарвис»",11);voiceHint.setGravity(Gravity.CENTER);voiceHint.setTextColor(MUTED);voiceHint.setTypeface(Typeface.DEFAULT,Typeface.BOLD);page.addView(voiceHint,lp(-1,25,0,4,0,0));

        LinearLayout quick=new LinearLayout(this);quick.setGravity(Gravity.CENTER);addQuick(quick,"ВРЕМЯ","сколько времени");addQuick(quick,"ПОГОДА","погода сейчас");addQuick(quick,"НОВОСТИ","новости сейчас");addQuick(quick,"ТАЙМЕР","таймер на 5 минут");page.addView(quick,lp(-1,42,0,8,0,0));
        setContentView(root);

        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(8),dp(6),dp(8),dp(5));nav.setBackground(box(0xFF123B5A,0xF2071019,28));addNav(nav,"◉","JARVIS",true,v->{});addNav(nav,"◌","Память",false,v->command("что ты помнишь"));addNav(nav,"⌁","Инструменты",false,v->showTools());addNav(nav,"⚙","Настройки",false,v->startActivity(new Intent(this,SettingsActivity.class)));FrameLayout.LayoutParams np=new FrameLayout.LayoutParams(-1,dp(78),Gravity.BOTTOM);np.bottomMargin=dp(8);root.addView(nav,np);
    }
    private void addQuick(LinearLayout r,String title,String cmd){TextView b=action(title,v->command(cmd));r.addView(b,new LinearLayout.LayoutParams(0,42,1));LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)b.getLayoutParams();p.setMargins(dp(3),0,dp(3),0);}
    private void addNav(LinearLayout n,String i,String t,boolean a,View.OnClickListener c){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setGravity(Gravity.CENTER);x.setOnClickListener(c);TextView iv=tv(i,23);iv.setGravity(Gravity.CENTER);iv.setTextColor(a?CYAN:MUTED);x.addView(iv,new LinearLayout.LayoutParams(-1,32));TextView tv=tv(t,11);tv.setGravity(Gravity.CENTER);tv.setTypeface(Typeface.DEFAULT,Typeface.BOLD);tv.setTextColor(a?WHITE:MUTED);x.addView(tv,new LinearLayout.LayoutParams(-1,22));n.addView(x,new LinearLayout.LayoutParams(0,66,1));}
    private void showTools(){new AlertDialogBuilder(this).show();}

    private final class AlertDialogBuilder{final Activity a;AlertDialogBuilder(Activity x){a=x;}void show(){final android.app.Dialog d=new android.app.Dialog(a);LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(22),dp(20),dp(22),dp(18));l.setBackground(box(0xFF155078,0xFF06131F,28));TextView h=tv("JARVIS • ИНСТРУМЕНТЫ",20);h.setTextColor(CYAN);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);l.addView(h);String[] cmds={"⏱  Таймер","◷  Время","⌁  Погода","✦  Новости","▣  Калькулятор","☼  Фонарик","⚙  Настройки"};String[] c={"таймер на 5 минут","сколько времени","погода сейчас","новости сейчас","посчитай 125 плюс 8","включи фонарик","открой настройки"};for(int k=0;k<cmds.length;k++){TextView b=action(cmds[k],v->{int ix=(int)v.getTag();if(ix==6){d.dismiss();startActivity(new Intent(MainActivity.this,SettingsActivity.class));}else{d.dismiss();command(c[ix]);}});b.setTag(k);l.addView(b,lp(-1,48,0,8,0,0));}TextView close=action("ЗАКРЫТЬ",v->d.dismiss());l.addView(close,lp(-1,48,0,14,0,0));d.setContentView(l);WindowManager.LayoutParams w=new WindowManager.LayoutParams();w.copyFrom(d.getWindow().getAttributes());w.width=(int)(getResources().getDisplayMetrics().widthPixels*.88);w.height=WindowManager.LayoutParams.WRAP_CONTENT;d.getWindow().setAttributes(w);d.show();}}

    private void updateNetworkState(){try{ConnectivityManager cm=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);Network n=cm==null?null:cm.getActiveNetwork();boolean ok=n!=null&&cm.getNetworkCapabilities(n)!=null;networkLabel.setText(ok?"●  В СЕТИ":"●  НЕТ СЕТИ");networkLabel.setTextColor(ok?GREEN:RED);}catch(Throwable ignored){}}
    private void initTts(){tts=new TextToSpeech(this,r->{if(r!=TextToSpeech.SUCCESS)return;try{Locale ru=new Locale("ru","RU");tts.setLanguage(ru);Voice best=null;for(Voice v:tts.getVoices()){if(!"ru".equalsIgnoreCase(v.getLocale().getLanguage()))continue;if(best==null||v.getQuality()>best.getQuality()||(v.getQuality()==best.getQuality()&&v.isNetworkConnectionRequired()&&!best.isNetworkConnectionRequired()))best=v;}if(best!=null)tts.setVoice(best);tts.setSpeechRate(.84f);tts.setPitch(.78f);}catch(Throwable ignored){}});}
    private void initSpeech(){if(!SpeechRecognizer.isRecognitionAvailable(this))return;try{recognizer=(Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this))?SpeechRecognizer.createOnDeviceSpeechRecognizer(this):SpeechRecognizer.createSpeechRecognizer(this);}catch(Throwable e){recognizer=null;}if(recognizer==null)return;recognizer.setRecognitionListener(new RecognitionListener(){public void onReadyForSpeech(Bundle b){setState("СЛУШАЮ");}public void onBeginningOfSpeech(){setState("СЛУШАЮ");}public void onRmsChanged(float v){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){setState("ОБРАБОТКА");}public void onError(int e){setState("ГОТОВ");}public void onResults(Bundle b){ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty())command(r.get(0));else setState("ГОТОВ");}public void onPartialResults(Bundle b){}public void onEvent(int a,Bundle b){}});recognizerIntent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ru-RU");recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false);}
    private void listen(){if(recognizer==null){setState("РЕЧЬ НЕДОСТУПНА");return;}if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_MIC);return;}try{if(tts!=null)tts.stop();recognizer.cancel();recognizer.startListening(recognizerIntent);setState("СЛУШАЮ");}catch(Throwable t){setState("РЕЧЬ НЕДОСТУПНА");}}
    private void stopAll(){if(recognizer!=null){try{recognizer.stopListening();}catch(Throwable ignored){}try{recognizer.cancel();}catch(Throwable ignored){}}if(tts!=null)try{tts.stop();}catch(Throwable ignored){}speaking=false;setState("ГОТОВ");}
    private void sendText(){String s=input.getText().toString().trim();if(s.isEmpty())return;input.setText("");InputMethodManager im=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(im!=null)im.hideSoftInputFromWindow(input.getWindowToken(),0);command(s);}
    private void command(String s){if(s==null||s.trim().isEmpty())return;replyLabel.setText("ВЫ  •  "+s);replyText.setText("JARVIS  •  обрабатываю…");setState("ОБРАБОТКА");engine.handle(s);}
    private void speak(String s){runOnUiThread(()->{replyText.setText("JARVIS  •  "+s);setState("ОТВЕЧАЮ");});if(tts!=null){speaking=true;tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"jarvis_reply");}}
    private void setState(String s){runOnUiThread(()->{if(status!=null)status.setText(s);if(core!=null)core.setState(s);if(voiceHint!=null){if(s.contains("СЛУША"))voiceHint.setText("СЛУШАЮ ВАС…");else if(s.contains("ИЩУ"))voiceHint.setText("ИЩУ АКТУАЛЬНУЮ ИНФОРМАЦИЮ…");else if(s.contains("ОБРАБОТ"))voiceHint.setText("АНАЛИЗИРУЮ ЗАПРОС…");else if(s.contains("ОТВЕЧ"))voiceHint.setText("JARVIS ОТВЕЧАЕТ");else voiceHint.setText("Нажмите на ядро или «Джарвис»");}});}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_MIC&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED){getSharedPreferences("jarvis",0).edit().putBoolean("wake_enabled",true).apply();listen();}}
    private void startWakeService(){try{Intent i=new Intent(this,JarvisWakeWordService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}catch(Throwable ignored){}}
    private void stopWakeService(){try{stopService(new Intent(this,JarvisWakeWordService.class));}catch(Throwable ignored){}}
    @Override protected void onResume(){super.onResume();stopWakeService();}
    @Override protected void onPause(){super.onPause();if(getSharedPreferences("jarvis",0).getBoolean("wake_enabled",true)&&Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)startWakeService();}
    @Override protected void onDestroy(){if(recognizer!=null)try{recognizer.destroy();}catch(Throwable ignored){}if(tts!=null){try{tts.stop();}catch(Throwable ignored){}try{tts.shutdown();}catch(Throwable ignored){}}if(engine!=null)engine.shutdown();super.onDestroy();}
}
