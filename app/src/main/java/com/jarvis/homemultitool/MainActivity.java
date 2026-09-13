package com.jarvis.homemultitool;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.speech.*;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import java.util.ArrayList;

/** JARVIS premium home screen. Geometry is fixed and text never controls button height. */
public class MainActivity extends Activity {
    private static final int BG=Color.rgb(2,6,11), PANEL=Color.rgb(6,16,27), PANEL_2=Color.rgb(7,20,32), CYAN=Color.rgb(83,220,255), BORDER=Color.rgb(21,91,137), WHITE=Color.rgb(242,247,255), MUTED=Color.rgb(122,153,181), GREEN=Color.rgb(71,232,177), RED=Color.rgb(255,103,119);
    private static final int REQ_MIC=42, REQ_NOTIFY=43;
    private final Handler main=new Handler(Looper.getMainLooper());
    private TextView status,network,replyTitle,replyBody,voiceHint;
    private EditText input;
    private SpeechRecognizer recognizer;
    private Intent recognizerIntent;
    private JarvisVoiceManager voice;
    private JarvisCoreView core;
    private JarvisEngine engine;

    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(String s,float sp){
        TextView v=new TextView(this); v.setText(s); v.setTextColor(WHITE); v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,sp);
        v.setIncludeFontPadding(false); v.setGravity(Gravity.CENTER_VERTICAL); v.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE); return v;
    }
    private GradientDrawable surface(int stroke,int fill,float radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));if(stroke>0)g.setStroke(dp(1),stroke);return g;}
    private LinearLayout.LayoutParams margin(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    private TextView action(String label,View.OnClickListener listener){
        TextView v=text(label,13);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setSingleLine(true);v.setEllipsize(android.text.TextUtils.TruncateAt.END);
        v.setPadding(dp(12),0,dp(12),0);v.setMinHeight(dp(50));v.setBackground(surface(BORDER,0xF0071622,17));v.setOnClickListener(listener);
        if(Build.VERSION.SDK_INT>=26)v.setAutoSizeTextTypeUniformWithConfiguration(dp(11),dp(14),dp(1),android.util.TypedValue.COMPLEX_UNIT_PX);
        return v;
    }
    private TextView icon(String glyph,View.OnClickListener listener){TextView v=action(glyph,listener);v.setTextColor(CYAN);v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,24);v.setPadding(0,0,0,0);return v;}

    @Override protected void onCreate(Bundle state){
        super.onCreate(state); applyImmersive(); getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE); buildUi();
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIFY);
        engine=new JarvisEngine(this,new JarvisEngine.Callback(){public void reply(String s){speak(s);}public void state(String s){setState(s);}});
        voice=new JarvisVoiceManager(this); voice.init(null); initSpeech(); handleIntent(getIntent());
    }
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handleIntent(i);}
    private void handleIntent(Intent i){
        if(i==null)return; String q=i.getStringExtra("WAKE_QUERY"); String pkg=i.getStringExtra("LAUNCH_PACKAGE");
        if(q!=null&&!q.trim().isEmpty())main.postDelayed(()->command(q),150);
        else if(pkg!=null&&!pkg.trim().isEmpty())main.postDelayed(()->launchPackage(pkg),100);
        else if(i.getBooleanExtra("LOCKSCREEN_ASSIST",false))main.postDelayed(this::listen,300);
    }
    private void applyImmersive(){
        try{if(Build.VERSION.SDK_INT>=30){getWindow().setDecorFitsSystemWindows(false);WindowInsetsController c=getWindow().getInsetsController();if(c!=null){c.hide(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}}
        else getWindow().getDecorView().setSystemUiVisibility(5894|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}catch(Throwable ignored){}
    }
    private void buildUi(){
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(BG);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setBackgroundColor(BG);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(20),dp(16),dp(20),dp(180));scroll.addView(page,new ScrollView.LayoutParams(-1,-1));root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(icon("☰",v->showTools()),new LinearLayout.LayoutParams(dp(54),dp(54)));
        LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.VERTICAL);brand.setGravity(Gravity.CENTER);
        TextView b=text("J A R V I S",25);b.setTextColor(CYAN);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setGravity(Gravity.CENTER);b.setLetterSpacing(.18f);brand.addView(b,new LinearLayout.LayoutParams(-1,dp(31)));
        TextView sub=text("PERSONAL AI SYSTEM",9);sub.setTextColor(MUTED);sub.setGravity(Gravity.CENTER);sub.setLetterSpacing(.11f);brand.addView(sub,new LinearLayout.LayoutParams(-1,dp(17)));
        header.addView(brand,new LinearLayout.LayoutParams(0,dp(54),1));header.addView(icon("⚙",v->openSettings()),new LinearLayout.LayoutParams(dp(54),dp(54)));page.addView(header,new LinearLayout.LayoutParams(-1,dp(54)));

        LinearLayout statusRow=new LinearLayout(this);statusRow.setGravity(Gravity.CENTER_VERTICAL);
        status=text("ГОТОВ",11);status.setTextColor(CYAN);status.setTypeface(Typeface.DEFAULT,Typeface.BOLD);status.setLetterSpacing(.09f);statusRow.addView(status,new LinearLayout.LayoutParams(0,dp(26),1));
        network=text("●  В СЕТИ",11);network.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);network.setTypeface(Typeface.DEFAULT,Typeface.BOLD);statusRow.addView(network,new LinearLayout.LayoutParams(0,dp(26),1));page.addView(statusRow,margin(-1,26,0,7,0,0));updateNetwork();

        core=new JarvisCoreView(this);core.setState("ГОТОВ");core.startPulse();core.setOnClickListener(v->listen());page.addView(core,margin(-1,272,0,4,0,0));
        TextView online=text("●  JARVIS ONLINE",12);online.setTextColor(CYAN);online.setTypeface(Typeface.DEFAULT,Typeface.BOLD);online.setGravity(Gravity.CENTER);online.setLetterSpacing(.08f);page.addView(online,new LinearLayout.LayoutParams(-1,dp(30)));

        LinearLayout response=panel();replyTitle=text("ГОТОВ К ЗАПРОСУ",10);replyTitle.setTextColor(MUTED);replyTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);replyTitle.setLetterSpacing(.08f);response.addView(replyTitle,new LinearLayout.LayoutParams(-1,dp(20)));
        replyBody=text("Нажмите на ядро или скажите «Привет, Джарвис»",16);replyBody.setTypeface(Typeface.DEFAULT,Typeface.BOLD);replyBody.setMaxLines(2);replyBody.setEllipsize(android.text.TextUtils.TruncateAt.END);response.addView(replyBody,margin(-1,50,0,5,0,0));page.addView(response,margin(-1,86,0,8,0,0));

        LinearLayout voiceRow=new LinearLayout(this);voiceRow.setGravity(Gravity.CENTER);TextView speak=action("◉  ГОВОРИТЬ",v->listen());voiceRow.addView(speak,new LinearLayout.LayoutParams(0,dp(54),1));
        TextView stop=action("■  СТОП",v->stopAll());voiceRow.addView(stop,margin(0,54,8,0,0,0));page.addView(voiceRow,margin(-1,54,0,7,0,0));
        voiceHint=text("МИКРОФОН ГОТОВ  •  НАЖМИТЕ ГОВОРИТЬ",9);voiceHint.setTextColor(MUTED);voiceHint.setTypeface(Typeface.DEFAULT,Typeface.BOLD);voiceHint.setGravity(Gravity.CENTER);page.addView(voiceHint,new LinearLayout.LayoutParams(-1,dp(22)));

        setContentView(root);
        buildBottom(root);
    }
    private LinearLayout panel(){LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(18),dp(13),dp(18),dp(10));p.setBackground(surface(BORDER,0xED06131F,24));return p;}
    private void buildBottom(FrameLayout root){
        LinearLayout composer=new LinearLayout(this);composer.setOrientation(LinearLayout.HORIZONTAL);composer.setGravity(Gravity.CENTER_VERTICAL);composer.setPadding(dp(10),dp(10),dp(10),dp(10));composer.setBackground(surface(BORDER,0xF2071420,24));
        input=new EditText(this);input.setSingleLine(true);input.setTextColor(WHITE);input.setHintTextColor(MUTED);input.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,16);input.setHint("Спросить JARVIS…");input.setGravity(Gravity.CENTER_VERTICAL);input.setPadding(dp(16),0,dp(12),0);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);input.setImeOptions(EditorInfo.IME_ACTION_SEND);input.setBackground(surface(BORDER,0xEE071724,20));input.setOnEditorActionListener((v,id,e)->{if(id==EditorInfo.IME_ACTION_SEND){sendText();return true;}return false;});composer.addView(input,new LinearLayout.LayoutParams(0,dp(66),1));
        TextView send=icon("➤",v->sendText());composer.addView(send,margin(66,66,8,0,0,0));FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,dp(86),Gravity.BOTTOM);cp.setMargins(dp(12),0,dp(12),dp(86));root.addView(composer,cp);

        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(8),dp(6),dp(8),dp(6));nav.setBackground(surface(BORDER,0xF2071420,24));
        addNav(nav,"◉","JARVIS",true,v->{});addNav(nav,"◌","ПАМЯТЬ",false,v->command("что ты помнишь"));addNav(nav,"⌁","ИНСТРУМЕНТЫ",false,v->showTools());addNav(nav,"⚙","НАСТРОЙКИ",false,v->openSettings());
        FrameLayout.LayoutParams np=new FrameLayout.LayoutParams(-1,dp(76),Gravity.BOTTOM);np.setMargins(dp(12),0,dp(12),dp(8));root.addView(nav,np);
    }
    private void addNav(LinearLayout bar,String glyph,String label,boolean active,View.OnClickListener click){
        LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setGravity(Gravity.CENTER);item.setOnClickListener(click);
        TextView i=text(glyph,17);i.setGravity(Gravity.CENTER);i.setTextColor(active?CYAN:MUTED);item.addView(i,new LinearLayout.LayoutParams(-1,dp(30)));
        TextView t=text(label,9);t.setGravity(Gravity.CENTER);t.setSingleLine(true);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setTextColor(active?WHITE:MUTED);item.addView(t,new LinearLayout.LayoutParams(-1,dp(18)));bar.addView(item,new LinearLayout.LayoutParams(0,dp(54),1));
    }
    private void showTools(){
        final Dialog d=new Dialog(this);d.getWindow();LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(18),dp(18),dp(18));box.setBackground(surface(BORDER,0xFF06131F,26));
        TextView h=text("ИНСТРУМЕНТЫ JARVIS",18);h.setTextColor(CYAN);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.setGravity(Gravity.CENTER);box.addView(h,new LinearLayout.LayoutParams(-1,dp(42)));
        String[] labels={"ТАЙМЕР","ВРЕМЯ","ПОГОДА","НОВОСТИ","КАЛЬКУЛЯТОР","ФОНАРИК","ЗАКРЫТЬ"};String[] cmds={"таймер на 5 минут","который час","какая сейчас погода в Москве","новости сейчас","посчитай 125 плюс 8","включи фонарик",""};
        for(int i=0;i<labels.length;i++){final int k=i;box.addView(action(labels[i],v->{d.dismiss();if(k<6)command(cmds[k]);}),margin(-1,52,0,7,0,0));}
        d.setContentView(box);d.setOnShowListener(x->{Window w=d.getWindow();if(w!=null){WindowManager.LayoutParams a=w.getAttributes();a.width=(int)(getResources().getDisplayMetrics().widthPixels*.88);a.height=WindowManager.LayoutParams.WRAP_CONTENT;w.setAttributes(a);w.setGravity(Gravity.CENTER);}});d.show();
    }
    private void openSettings(){try{startActivity(new Intent(this,SettingsActivity.class));}catch(Throwable e){safeSystem();}}
    private void safeSystem(){try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Throwable ignored){}}
    private void updateNetwork(){try{android.net.ConnectivityManager cm=(android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);android.net.Network n=cm==null?null:cm.getActiveNetwork();boolean ok=n!=null&&cm.getNetworkCapabilities(n)!=null;if(network!=null){network.setText(ok?"●  В СЕТИ":"●  НЕТ СЕТИ");network.setTextColor(ok?GREEN:RED);}}catch(Throwable ignored){}}

    private void initSpeech(){
        if(!SpeechRecognizer.isRecognitionAvailable(this)){setState("РЕЧЬ НЕДОСТУПНА");return;}
        recognizerIntent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ru-RU");recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"ru-RU");recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,false);recreateRecognizer();
    }
    private void recreateRecognizer(){
        try{if(recognizer!=null)recognizer.destroy();recognizer=Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this)?SpeechRecognizer.createOnDeviceSpeechRecognizer(this):SpeechRecognizer.createSpeechRecognizer(this);}catch(Throwable e){try{recognizer=SpeechRecognizer.createSpeechRecognizer(this);}catch(Throwable ignored){recognizer=null;}}
        if(recognizer==null)return;recognizer.setRecognitionListener(new RecognitionListener(){
            public void onReadyForSpeech(Bundle b){setState("СЛУШАЮ");voiceHint.setText("ГОВОРИТЕ  •  JARVIS СЛУШАЕТ");}
            public void onBeginningOfSpeech(){setState("СЛУШАЮ");} public void onRmsChanged(float v){} public void onBufferReceived(byte[] b){} public void onEndOfSpeech(){setState("ОБРАБОТКА");}
            public void onError(int e){setState("ГОТОВ");voiceHint.setText("МИКРОФОН ГОТОВ  •  НАЖМИТЕ ГОВОРИТЬ");}
            public void onResults(Bundle b){ArrayList<String> r=b==null?null:b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty())command(r.get(0));}
            public void onPartialResults(Bundle b){} public void onEvent(int a,Bundle b){}
        });
    }
    private void listen(){
        if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_MIC);return;}
        if(recognizer==null){recreateRecognizer();if(recognizer==null)return;}try{recognizer.cancel();recognizer.startListening(recognizerIntent);}catch(Throwable e){recreateRecognizer();}
    }
    private void command(String q){if(q==null||q.trim().isEmpty())return;String clean=q.trim();if(input!=null)input.setText("");replyTitle.setText("КОМАНДА");replyBody.setText(clean);setState("ОБРАБОТКА");if(engine!=null)engine.handle(clean);}
    private void sendText(){String q=input==null?"":input.getText().toString();if(q.trim().isEmpty()){listen();return;}command(q);}
    private void speak(String s){if(s==null||s.trim().isEmpty())return;replyTitle.setText("JARVIS");replyBody.setText(s);if(core!=null)core.setState("ОТВЕЧАЮ");if(voice!=null&&voice.isReady())voice.speak(s,null);}
    private void stopAll(){try{if(recognizer!=null)recognizer.cancel();}catch(Throwable ignored){}if(voice!=null)voice.stop();setState("ГОТОВ");voiceHint.setText("МИКРОФОН ГОТОВ  •  НАЖМИТЕ ГОВОРИТЬ");}
    private void setState(String s){main.post(()->{if(status!=null)status.setText(s==null?"ГОТОВ":s);if(core!=null)core.setState(s);});}
    private void launchPackage(String pkg){try{Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}}catch(Throwable ignored){}}
    @Override protected void onResume(){super.onResume();applyImmersive();updateNetwork();}
    @Override protected void onDestroy(){try{if(recognizer!=null)recognizer.destroy();}catch(Throwable ignored){}if(engine!=null)engine.shutdown();if(voice!=null)voice.shutdown();super.onDestroy();}
}
