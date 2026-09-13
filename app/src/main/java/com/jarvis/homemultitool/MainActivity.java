package com.jarvis.homemultitool;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.speech.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;

/** JARVIS premium HUD. All dimensions are density independent and buttons use wrap-safe text. */
public class MainActivity extends Activity {
    private static final int BG=Color.rgb(2,6,10),SUR=Color.rgb(5,15,25),CYAN=Color.rgb(83,220,255),BORDER=Color.rgb(25,102,153),WHITE=Color.rgb(242,247,255),MUTED=Color.rgb(121,153,180),GREEN=Color.rgb(71,232,177),RED=Color.rgb(255,102,116);
    private static final int REQ_MIC=42,REQ_NOTIFY=43;
    private TextView status,replyLabel,replyText,networkLabel,voiceHint;
    private EditText input;
    private SpeechRecognizer recognizer;
    private Intent recognizerIntent;
    private JarvisVoiceManager voice;
    private JarvisCoreView core;
    private JarvisEngine engine;
    private final Handler main=new Handler(Looper.getMainLooper());

    private int dp(float v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
    private TextView txt(String s,float sp){
        TextView v=new TextView(this); v.setText(s); v.setTextColor(WHITE); float scale=Math.min(getResources().getConfiguration().fontScale,1.15f); v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,sp/scale);
        v.setIncludeFontPadding(true); v.setGravity(Gravity.CENTER_VERTICAL); v.setHorizontallyScrolling(false); v.setMaxLines(Integer.MAX_VALUE); return v;
    }
    private GradientDrawable bg(int stroke,int fill,float radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));if(stroke>0)g.setStroke(dp(1),stroke);return g;}
    private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    private TextView button(String label,View.OnClickListener click){
        TextView v=txt(label,13); v.setGravity(Gravity.CENTER); v.setTypeface(Typeface.DEFAULT,Typeface.BOLD); v.setTextColor(WHITE);
        v.setPadding(dp(10),dp(4),dp(10),dp(4)); v.setBackground(bg(BORDER,0xD9071420,18)); v.setOnClickListener(click);
        boolean longText=label.length()>22; v.setSingleLine(!longText); v.setMaxLines(longText?2:1);
        v.setEllipsize(android.text.TextUtils.TruncateAt.END); v.setMinHeight(dp(longText?54:48)); v.setMaxHeight(dp(longText?58:52));
        if(Build.VERSION.SDK_INT>=26)v.setAutoSizeTextTypeUniformWithConfiguration(longText?8:9,13,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        return v;
    }
    private TextView iconButton(String icon,View.OnClickListener click){TextView v=txt(icon,24);v.setGravity(Gravity.CENTER);v.setTextColor(CYAN);v.setBackground(bg(BORDER,0xC9071420,18));v.setOnClickListener(click);return v;}

    @Override protected void onCreate(Bundle b){
        super.onCreate(b); immersive(); getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        buildUi();
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIFY);
        engine=new JarvisEngine(this,new JarvisEngine.Callback(){public void reply(String s){speak(s);}public void state(String s){setState(s);}});
        voice=new JarvisVoiceManager(this); voice.init(null); initSpeech();
        handleIncomingIntent(getIntent());
    }

    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent); setIntent(intent); handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent){
        if(intent==null)return;
        String q=intent.getStringExtra("WAKE_QUERY");
        String launchPackage=intent.getStringExtra("LAUNCH_PACKAGE");
        if(q!=null&&!q.trim().isEmpty())main.postDelayed(()->command(q),220);
        else if(launchPackage!=null&&!launchPackage.trim().isEmpty())main.postDelayed(()->launchPackage(launchPackage),120);
        else if(intent.getBooleanExtra("LOCKSCREEN_ASSIST",false))main.postDelayed(this::listen,450);
    }

    private void launchPackage(String packageName){
        try{
            Intent launch=getPackageManager().getLaunchIntentForPackage(packageName);
            if(launch==null)return;
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
        }catch(Throwable ignored){}
    }

    private void immersive(){
        try{
            if(Build.VERSION.SDK_INT>=30){
                getWindow().setDecorFitsSystemWindows(false); WindowInsetsController c=getWindow().getInsetsController();
                if(c!=null){c.hide(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}
            }else getWindow().getDecorView().setSystemUiVisibility(5894|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }catch(Throwable ignored){}
    }

    private void buildUi(){
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(BG);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setBackgroundColor(BG);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(18),dp(10),dp(18),dp(110));
        scroll.addView(page,new ScrollView.LayoutParams(-1,-1));root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(iconButton("☰",v->showTools()),new LinearLayout.LayoutParams(dp(52),dp(52)));
        LinearLayout brandBox=new LinearLayout(this);brandBox.setOrientation(LinearLayout.VERTICAL);brandBox.setGravity(Gravity.CENTER);
        TextView brand=txt("J A R V I S",24);brand.setTextColor(CYAN);brand.setGravity(Gravity.CENTER);brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);brand.setLetterSpacing(.17f);brandBox.addView(brand,new LinearLayout.LayoutParams(-1,dp(31)));
        TextView sub=txt("PERSONAL AI SYSTEM",9);sub.setTextColor(MUTED);sub.setGravity(Gravity.CENTER);sub.setLetterSpacing(.12f);brandBox.addView(sub,new LinearLayout.LayoutParams(-1,dp(17)));
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(52),1);bp.setMargins(dp(8),0,dp(8),0);header.addView(brandBox,bp);
        header.addView(iconButton("⚙",v->openSettings()),new LinearLayout.LayoutParams(dp(52),dp(52))); page.addView(header,new LinearLayout.LayoutParams(-1,dp(52)));

        LinearLayout rail=new LinearLayout(this);rail.setGravity(Gravity.CENTER_VERTICAL);
        status=txt("ГОТОВ",11);status.setTextColor(CYAN);status.setTypeface(Typeface.DEFAULT,Typeface.BOLD);status.setLetterSpacing(.09f);rail.addView(status,new LinearLayout.LayoutParams(0,dp(28),1));
        networkLabel=txt("●  В СЕТИ",11);networkLabel.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);networkLabel.setTypeface(Typeface.DEFAULT,Typeface.BOLD);rail.addView(networkLabel,new LinearLayout.LayoutParams(0,dp(28),1));
        page.addView(rail,lp(-1,28,0,5,0,0));updateNetworkState();

        core=new JarvisCoreView(this);core.setState("ГОТОВ");core.startPulse();core.setOnClickListener(v->listen());page.addView(core,lp(-1,315,0,0,0,0));
        TextView online=txt("●  JARVIS ONLINE",12);online.setTextColor(CYAN);online.setGravity(Gravity.CENTER);online.setTypeface(Typeface.DEFAULT,Typeface.BOLD);online.setLetterSpacing(.08f);page.addView(online,new LinearLayout.LayoutParams(-1,dp(28)));

        LinearLayout response=card(); replyLabel=txt("ГОТОВ К ЗАПРОСУ",10);replyLabel.setTextColor(MUTED);replyLabel.setTypeface(Typeface.DEFAULT,Typeface.BOLD);replyLabel.setLetterSpacing(.07f);response.addView(replyLabel,new LinearLayout.LayoutParams(-1,dp(22)));
        replyText=txt("Нажмите ядро или ГОВОРИТЬ",15);replyText.setTypeface(Typeface.DEFAULT,Typeface.BOLD);replyText.setMaxLines(2);replyText.setEllipsize(android.text.TextUtils.TruncateAt.END);response.addView(replyText,new LinearLayout.LayoutParams(-1,dp(48)));
        page.addView(response,lp(-1,-2,0,7,0,0));

        LinearLayout voiceRow=new LinearLayout(this);voiceRow.setGravity(Gravity.CENTER_VERTICAL);
        voiceRow.addView(button("◉  ГОВОРИТЬ",v->listen()),new LinearLayout.LayoutParams(0,dp(54),1));
        voiceRow.addView(button("■  СТОП",v->stopAll()),lp(0,54,8,0,0,0));page.addView(voiceRow,lp(-1,58,0,7,0,0));
        voiceHint=txt("МИКРОФОН ГОТОВ  •  НАЖМИТЕ ГОВОРИТЬ",9);voiceHint.setTextColor(MUTED);voiceHint.setGravity(Gravity.CENTER);voiceHint.setTypeface(Typeface.DEFAULT,Typeface.BOLD);page.addView(voiceHint,new LinearLayout.LayoutParams(-1,dp(22)));

        LinearLayout command=new LinearLayout(this);command.setGravity(Gravity.CENTER_VERTICAL);
        input=new EditText(this);input.setSingleLine(true);input.setTextColor(WHITE);input.setHintTextColor(MUTED);input.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,14);input.setHint("Спросить JARVIS…");input.setIncludeFontPadding(true);input.setGravity(Gravity.CENTER_VERTICAL);input.setPadding(dp(15),0,dp(8),0);input.setBackground(bg(BORDER,0xD9071420,18));input.setImeOptions(6);input.setOnEditorActionListener((v,a,e)->{sendText();return true;});command.addView(input,new LinearLayout.LayoutParams(0,dp(52),1));
        TextView send=button("➤",v->sendText());send.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,21);command.addView(send,lp(58,52,8,0,0,0));page.addView(command,lp(-1,54,0,7,0,0));

        LinearLayout quick=new LinearLayout(this);quick.setGravity(Gravity.CENTER_VERTICAL);addQuick(quick,"ВРЕМЯ","сколько времени");addQuick(quick,"ПОГОДА","какая сейчас погода в Москве");addQuick(quick,"ТАЙМЕР","таймер на 5 минут");page.addView(quick,lp(-1,50,0,5,0,0));
        setContentView(root);

        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(7),dp(6),dp(7),dp(6));nav.setBackground(bg(BORDER,0xF2071019,22));
        addNav(nav,"◉","JARVIS",true,v->scroll.smoothScrollTo(0,0));addNav(nav,"◌","ПАМЯТЬ",false,v->command("что ты помнишь"));addNav(nav,"⌁","ИНСТРУМЕНТЫ",false,v->showTools());addNav(nav,"⚙","НАСТРОЙКИ",false,v->openSettings());
        FrameLayout.LayoutParams np=new FrameLayout.LayoutParams(-1,dp(78),Gravity.BOTTOM);np.setMargins(dp(10),0,dp(10),dp(8));root.addView(nav,np);
    }

    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(11),dp(16),dp(11));c.setBackground(bg(BORDER,0xEF06131F,22));return c;}
    private void addQuick(LinearLayout r,String title,String cmd){TextView b=button(title,v->command(cmd));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(46),1);p.setMargins(dp(3),0,dp(3),0);r.addView(b,p);}
    private void addNav(LinearLayout n,String icon,String label,boolean active,View.OnClickListener c){LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setGravity(Gravity.CENTER);item.setOnClickListener(c);TextView i=txt(icon,18);i.setGravity(Gravity.CENTER);i.setTextColor(active?CYAN:MUTED);item.addView(i,new LinearLayout.LayoutParams(-1,dp(28)));TextView t=txt(label,9);t.setGravity(Gravity.CENTER);t.setSingleLine(true);t.setEllipsize(android.text.TextUtils.TruncateAt.END);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setTextColor(active?WHITE:MUTED);item.addView(t,new LinearLayout.LayoutParams(-1,dp(18)));n.addView(item,new LinearLayout.LayoutParams(0,dp(54),1));}

    private void showTools(){
        final Dialog d=new Dialog(this); LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(16),dp(16),dp(16));box.setBackground(bg(BORDER,0xFF06131F,24));
        TextView h=txt("JARVIS  •  ИНСТРУМЕНТЫ",18);h.setTextColor(CYAN);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);box.addView(h,new LinearLayout.LayoutParams(-1,dp(38)));
        String[] names={"⏱  ТАЙМЕР","◷  ВРЕМЯ","⌁  ПОГОДА","✦  НОВОСТИ","▣  КАЛЬКУЛЯТОР","☼  ФОНАРИК","⚙  НАСТРОЙКИ"};
        String[] cmds={"таймер на 5 минут","сколько времени","какая сейчас погода в Москве","новости сейчас","посчитай 125 плюс 8","включи фонарик",""};
        for(int k=0;k<names.length;k++){final int ix=k;box.addView(button(names[k],v->{d.dismiss();if(ix==6)openSettings();else command(cmds[ix]);}),lp(-1,-2,0,6,0,0));}
        box.addView(button("ЗАКРЫТЬ",v->d.dismiss()),lp(-1,-2,0,12,0,0));
        d.setContentView(box);d.setOnShowListener(x->{Window w=d.getWindow();if(w!=null){WindowManager.LayoutParams a=w.getAttributes();a.width=(int)(getResources().getDisplayMetrics().widthPixels*.90);a.height=WindowManager.LayoutParams.WRAP_CONTENT;w.setAttributes(a);w.setGravity(Gravity.CENTER);}});d.show();
    }
    private void openSettings(){try{startActivity(new Intent(this,SettingsActivity.class));}catch(Throwable ignored){startActivity(new Intent(Settings.ACTION_SETTINGS));}}
    private void updateNetworkState(){try{android.net.ConnectivityManager cm=(android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);android.net.Network n=cm==null?null:cm.getActiveNetwork();boolean ok=n!=null&&cm.getNetworkCapabilities(n)!=null;if(networkLabel!=null){networkLabel.setText(ok?"●  В СЕТИ":"●  НЕТ СЕТИ");networkLabel.setTextColor(ok?GREEN:RED);}}catch(Throwable ignored){}}

    private void initSpeech(){
        if(!SpeechRecognizer.isRecognitionAvailable(this)){setState("РЕЧЬ НЕДОСТУПНА");return;}
        recognizerIntent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ru-RU");recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"ru-RU");recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,false);recreateRecognizer();
    }
    private void recreateRecognizer(){
        try{if(recognizer!=null)recognizer.destroy();recognizer=SpeechRecognizer.createSpeechRecognizer(this);}catch(Throwable e){recognizer=null;}
        if(recognizer==null)return; recognizer.setRecognitionListener(new RecognitionListener(){
            public void onReadyForSpeech(Bundle b){setState("СЛУШАЮ");voiceHint.setText("ГОВОРИТЕ  •  JARVIS СЛУШАЕТ");}
            public void onBeginningOfSpeech(){setState("СЛУШАЮ");}
            public void onRmsChanged(float v){} public void onBufferReceived(byte[] b){} public void onEndOfSpeech(){setState("ОБРАБОТКА");}
            public void onError(int e){setState("ГОТОВ");voiceHint.setText("МИКРОФОН ГОТОВ  •  НАЖМИТЕ ГОВОРИТЬ");}
            public void onResults(Bundle b){ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty())command(r.get(0));else setState("ГОТОВ");}
            public void onPartialResults(Bundle b){} public void onEvent(int a,Bundle b){}
        });
    }
    private void listen(){
        if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_MIC);return;}
        if(recognizer==null){recreateRecognizer();if(recognizer==null){setState("РЕЧЬ НЕДОСТУПНА");return;}}
        if(voice!=null)voice.stop();
        try{recognizer.cancel();recognizer.startListening(recognizerIntent);setState("СЛУШАЮ");}catch(Throwable t){recreateRecognizer();main.postDelayed(()->{try{if(recognizer!=null)recognizer.startListening(recognizerIntent);}catch(Throwable ignored){}},250);}
    }
    private void stopAll(){if(recognizer!=null){try{recognizer.stopListening();}catch(Throwable ignored){}try{recognizer.cancel();}catch(Throwable ignored){}}if(voice!=null)voice.stop();setState("ГОТОВ");if(voiceHint!=null)voiceHint.setText("МИКРОФОН ГОТОВ  •  НАЖМИТЕ ГОВОРИТЬ");}
    private void sendText(){String s=input.getText().toString().trim();if(s.isEmpty())return;input.setText("");InputMethodManager im=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(im!=null)im.hideSoftInputFromWindow(input.getWindowToken(),0);command(s);}
    private void command(String s){if(s==null||s.trim().isEmpty())return;replyLabel.setText("ВЫ  •  "+s.trim());replyText.setText("JARVIS  •  анализирую запрос…");setState("ОБРАБОТКА");if(engine!=null)engine.handle(s.trim());}
    private void speak(String s){runOnUiThread(()->{replyText.setText("JARVIS  •  "+s);replyText.setContentDescription(s);setState("ОТВЕЧАЮ");if(voice!=null){if(!voice.isReady()){voice.init(null);main.postDelayed(()->speakAgain(s),250);return;}speakAgain(s);}});}
    private void speakAgain(String s){if(voice!=null)voice.speak(s,new android.speech.tts.UtteranceProgressListener(){public void onStart(String id){}public void onDone(String id){setState("ГОТОВ");}public void onError(String id){setState("ГОТОВ");}});}
    private void setState(String s){runOnUiThread(()->{if(status!=null)status.setText(s);if(core!=null)core.setState(s);});}
    @Override protected void onResume(){super.onResume();immersive();updateNetworkState();}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_MIC&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)listen();}
    @Override protected void onDestroy(){if(recognizer!=null)try{recognizer.destroy();}catch(Throwable ignored){}if(voice!=null)voice.shutdown();if(engine!=null)engine.shutdown();super.onDestroy();}
}
