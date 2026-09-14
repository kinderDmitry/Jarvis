package com.jarvis.homemultitool;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.*;
import android.provider.Settings;
import android.speech.*;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import java.util.ArrayList;

/** JARVIS 5.23 — premium assistant UI. Station-inspired interaction, no quick-action clutter. */
public class MainActivity extends Activity {
    private static final int BG=Color.rgb(3,7,13), PANEL=Color.rgb(7,14,23), PANEL2=Color.rgb(10,20,31);
    private static final int CYAN=Color.rgb(105,225,255), BORDER=Color.rgb(28,91,132), WHITE=Color.rgb(244,248,252), MUTED=Color.rgb(132,153,173), GREEN=Color.rgb(75,232,178), RED=Color.rgb(255,99,120);
    private static final int REQ_MIC=42, REQ_NOTIFY=43;
    private final Handler main=new Handler(Looper.getMainLooper());
    private TextView status,network,replyTitle,replyBody,voiceHint;
    private EditText input; private SpeechRecognizer recognizer; private Intent recognizerIntent;
    private JarvisVoiceManager voice; private JarvisCoreView core; private JarvisEngine engine;

    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(String s,float sp){TextView v=new TextView(this);v.setText(s);v.setTextColor(WHITE);v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,sp);v.setIncludeFontPadding(false);v.setGravity(Gravity.CENTER_VERTICAL);v.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);return v;}
    private GradientDrawable bg(int stroke,int fill,float radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));if(stroke>0)g.setStroke(dp(1),stroke);return g;}
    private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    private TextView pill(String label,View.OnClickListener click){TextView v=text(label,11);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setSingleLine(true);v.setPadding(dp(14),0,dp(14),0);v.setBackground(bg(BORDER,0xDD091722,30));v.setOnClickListener(click);return v;}
    private TextView icon(String glyph,View.OnClickListener click){TextView v=pill(glyph,click);v.setTextColor(CYAN);v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,21);v.setPadding(0,0,0,0);return v;}

    @Override protected void onCreate(Bundle state){
        super.onCreate(state); applyImmersive(); getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        buildUi();
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIFY);
        engine=new JarvisEngine(this,new JarvisEngine.Callback(){public void reply(String s){speak(s);}public void state(String s){setState(s);}});
        voice=new JarvisVoiceManager(this);voice.init(null);initSpeech();handleIntent(getIntent());
    }
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handleIntent(i);}
    private void handleIntent(Intent i){if(i==null)return;String q=i.getStringExtra("WAKE_QUERY");String pkg=i.getStringExtra("LAUNCH_PACKAGE");if(q!=null&&!q.trim().isEmpty())main.postDelayed(()->command(q),120);else if(pkg!=null&&!pkg.trim().isEmpty())main.postDelayed(()->launchPackage(pkg),100);else if(i.getBooleanExtra("LOCKSCREEN_ASSIST",false))main.postDelayed(this::listen,300);}
    private void applyImmersive(){try{if(Build.VERSION.SDK_INT>=30){getWindow().setDecorFitsSystemWindows(false);WindowInsetsController c=getWindow().getInsetsController();if(c!=null){c.hide(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}}else getWindow().getDecorView().setSystemUiVisibility(5894|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}catch(Throwable ignored){}}

    private void buildUi(){
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(BG);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(18),dp(14),dp(18),dp(150));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setBackgroundColor(BG);scroll.addView(page,new ScrollView.LayoutParams(-1,-1));root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(icon("☰",v->showTools()),new LinearLayout.LayoutParams(dp(52),dp(52)));
        LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.VERTICAL);brand.setGravity(Gravity.CENTER);
        TextView title=text("J A R V I S",23);title.setTextColor(CYAN);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER);title.setLetterSpacing(.20f);brand.addView(title,new LinearLayout.LayoutParams(-1,dp(29)));
        TextView sub=text("PERSONAL ASSISTANT",8);sub.setTextColor(MUTED);sub.setGravity(Gravity.CENTER);sub.setLetterSpacing(.16f);brand.addView(sub,new LinearLayout.LayoutParams(-1,dp(15)));
        header.addView(brand,new LinearLayout.LayoutParams(0,dp(52),1));header.addView(icon("⚙",v->openSettings()),new LinearLayout.LayoutParams(dp(52),dp(52)));page.addView(header,new LinearLayout.LayoutParams(-1,dp(52)));

        LinearLayout statusRow=new LinearLayout(this);statusRow.setGravity(Gravity.CENTER_VERTICAL);status=text("ГОТОВ",10);status.setTextColor(CYAN);status.setTypeface(Typeface.DEFAULT,Typeface.BOLD);status.setLetterSpacing(.08f);statusRow.addView(status,new LinearLayout.LayoutParams(0,dp(25),1));network=text("●  В СЕТИ",10);network.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);network.setTypeface(Typeface.DEFAULT,Typeface.BOLD);statusRow.addView(network,new LinearLayout.LayoutParams(0,dp(25),1));page.addView(statusRow,lp(-1,25,2,10,2,0));updateNetwork();

        core=new JarvisCoreView(this);core.setState("ГОТОВ");core.startPulse();core.setOnClickListener(v->listen());page.addView(core,lp(-1,dp(330),0,2,0,0));
        voiceHint=text("НАЖМИТЕ НА ЯДРО ИЛИ СКАЖИТЕ «ПРИВЕТ, ДЖАРВИС»",9);voiceHint.setTextColor(MUTED);voiceHint.setTypeface(Typeface.DEFAULT,Typeface.BOLD);voiceHint.setGravity(Gravity.CENTER);voiceHint.setLetterSpacing(.045f);page.addView(voiceHint,new LinearLayout.LayoutParams(-1,dp(28)));

        LinearLayout reply=conversationCard();replyTitle=text("JARVIS",9);replyTitle.setTextColor(CYAN);replyTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);replyTitle.setLetterSpacing(.12f);reply.addView(replyTitle,new LinearLayout.LayoutParams(-1,dp(20)));replyBody=text("Готов к разговору.",18);replyBody.setTypeface(Typeface.DEFAULT,Typeface.NORMAL);replyBody.setLineSpacing(0,1.08f);reply.addView(replyBody,lp(-1,-2,0,7,0,2));page.addView(reply,lp(-1,-2,0,8,0,8));

        LinearLayout statusActions=new LinearLayout(this);statusActions.setGravity(Gravity.CENTER);TextView speak=pill("●  ГОВОРИТЬ",v->listen());statusActions.addView(speak,new LinearLayout.LayoutParams(0,48*getResources().getDisplayMetrics().density>0?dp(48):dp(48),1));TextView stop=pill("■  СТОП",v->stopAll());statusActions.addView(stop,lp(dp(88),dp(48),8,0,0,0));page.addView(statusActions,lp(-1,dp(48),0,2,0,0));

        setContentView(root);buildComposer(root);
    }
    private LinearLayout conversationCard(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(15),dp(18),dp(14));c.setBackground(bg(BORDER,0xF006111C,22));return c;}
    private void buildComposer(FrameLayout root){
        LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.HORIZONTAL);wrap.setGravity(Gravity.CENTER_VERTICAL);wrap.setPadding(dp(8),dp(8),dp(8),dp(8));wrap.setBackground(bg(BORDER,0xF20A1723,25));
        input=new EditText(this);input.setSingleLine(true);input.setTextColor(WHITE);input.setHintTextColor(MUTED);input.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,17);input.setHint("Спросить JARVIS…");input.setGravity(Gravity.CENTER_VERTICAL);input.setPadding(dp(16),0,dp(10),0);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);input.setImeOptions(EditorInfo.IME_ACTION_SEND);input.setBackground(bg(BORDER,0xEE08141F,20));input.setOnEditorActionListener((v,id,e)->{if(id==EditorInfo.IME_ACTION_SEND){sendText();return true;}return false;});wrap.addView(input,new LinearLayout.LayoutParams(0,dp(64),1));
        TextView send=icon("➤",v->sendText());wrap.addView(send,lp(dp(64),dp(64),8,0,0,0));FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,dp(80),Gravity.BOTTOM);cp.setMargins(dp(12),0,dp(12),dp(82));root.addView(wrap,cp);
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(8),dp(5),dp(8),dp(5));nav.setBackground(bg(BORDER,0xF20A1723,23));addNav(nav,"◉","ДЖАРВИС",true,v->listen());addNav(nav,"⚙","НАСТРОЙКИ",false,v->openSettings());FrameLayout.LayoutParams np=new FrameLayout.LayoutParams(-1,dp(70),Gravity.BOTTOM);np.setMargins(dp(12),0,dp(12),dp(8));root.addView(nav,np);
    }
    private void addNav(LinearLayout bar,String glyph,String label,boolean active,View.OnClickListener click){LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setGravity(Gravity.CENTER);item.setOnClickListener(click);TextView i=text(glyph,18);i.setGravity(Gravity.CENTER);i.setTextColor(active?CYAN:MUTED);item.addView(i,new LinearLayout.LayoutParams(-1,dp(29)));TextView t=text(label,9);t.setGravity(Gravity.CENTER);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setTextColor(active?WHITE:MUTED);item.addView(t,new LinearLayout.LayoutParams(-1,dp(17)));bar.addView(item,new LinearLayout.LayoutParams(0,dp(52),1));}

    private void showTools(){
        final Dialog d=new Dialog(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),dp(18),dp(20),dp(20));box.setBackground(bg(BORDER,0xFF07131E,28));
        TextView h=text("JARVIS",20);h.setTextColor(CYAN);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.setGravity(Gravity.CENTER);h.setLetterSpacing(.18f);box.addView(h,new LinearLayout.LayoutParams(-1,dp(38)));TextView s=text("ИНСТРУМЕНТЫ",9);s.setTextColor(MUTED);s.setGravity(Gravity.CENTER);s.setLetterSpacing(.12f);box.addView(s,new LinearLayout.LayoutParams(-1,dp(20)));
        String[] labels={"ТАЙМЕР","ВРЕМЯ","ПОГОДА","НОВОСТИ","КАЛЬКУЛЯТОР","ФОНАРИК"};String[] cmds={"таймер на 5 минут","который час","какая сейчас погода в Москве","новости сейчас","посчитай 125 плюс 8","включи фонарик"};for(int i=0;i<labels.length;i++){final int k=i;TextView b=pill(labels[i],v->{d.dismiss();command(cmds[k]);});box.addView(b,lp(-1,dp(50),0,7,0,0));}
        TextView close=pill("ЗАКРЫТЬ",v->d.dismiss());box.addView(close,lp(-1,dp(50),0,12,0,0));d.setContentView(box);d.setOnShowListener(x->{Window w=d.getWindow();if(w!=null){WindowManager.LayoutParams a=w.getAttributes();a.width=(int)(getResources().getDisplayMetrics().widthPixels*.88f);w.setAttributes(a);w.setGravity(Gravity.CENTER);}});d.show();
    }
    private void openSettings(){try{startActivity(new Intent(this,SettingsActivity.class));}catch(Throwable e){safeSystem();}}
    private void safeSystem(){try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Throwable ignored){}}
    private void updateNetwork(){try{ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);Network n=cm==null?null:cm.getActiveNetwork();NetworkCapabilities c=cm==null||n==null?null:cm.getNetworkCapabilities(n);boolean ok=c!=null&&(c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)||c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));if(network!=null){network.setText(ok?"●  В СЕТИ":"●  НЕТ СЕТИ");network.setTextColor(ok?GREEN:RED);}}catch(Throwable ignored){}}
    private void initSpeech(){if(!SpeechRecognizer.isRecognitionAvailable(this)){setState("РЕЧЬ НЕДОСТУПНА");return;}recognizerIntent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ru-RU");recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"ru-RU");recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,false);recreateRecognizer();}
    private void recreateRecognizer(){try{if(recognizer!=null)recognizer.destroy();recognizer=Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this)?SpeechRecognizer.createOnDeviceSpeechRecognizer(this):SpeechRecognizer.createSpeechRecognizer(this);}catch(Throwable e){try{recognizer=SpeechRecognizer.createSpeechRecognizer(this);}catch(Throwable ignored){recognizer=null;}}if(recognizer==null)return;recognizer.setRecognitionListener(new RecognitionListener(){public void onReadyForSpeech(Bundle b){setState("СЛУШАЮ");voiceHint.setText("ГОВОРИТЕ  •  JARVIS СЛУШАЕТ");}public void onBeginningOfSpeech(){setState("СЛУШАЮ");}public void onRmsChanged(float v){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){setState("ОБРАБОТКА");}public void onError(int e){setState("ГОТОВ");voiceHint.setText("НАЖМИТЕ НА ЯДРО ИЛИ СКАЖИТЕ «ПРИВЕТ, ДЖАРВИС»");}public void onResults(Bundle b){ArrayList<String> r=b==null?null:b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty())command(r.get(0));}public void onPartialResults(Bundle b){}public void onEvent(int a,Bundle b){}});}
    private void listen(){if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_MIC);return;}if(recognizer==null){recreateRecognizer();if(recognizer==null)return;}try{recognizer.cancel();recognizer.startListening(recognizerIntent);}catch(Throwable e){recreateRecognizer();}}
    private void command(String q){if(q==null||q.trim().isEmpty())return;String clean=q.trim();if(input!=null)input.setText("");replyTitle.setText("ЗАПРОС");replyTitle.setTextColor(MUTED);replyBody.setText(clean);setState("ОБРАБОТКА");if(engine!=null)engine.handle(clean);}
    private void sendText(){String q=input==null?"":input.getText().toString();if(q.trim().isEmpty()){listen();return;}command(q);}
    private void speak(String s){if(s==null||s.trim().isEmpty())return;replyTitle.setText("JARVIS");replyTitle.setTextColor(CYAN);replyBody.setText(s);if(core!=null)core.setState("ОТВЕЧАЮ");if(voice!=null&&voice.isReady())voice.speak(s,null);}
    private void stopAll(){try{if(recognizer!=null)recognizer.cancel();}catch(Throwable ignored){}if(voice!=null)voice.stop();setState("ГОТОВ");voiceHint.setText("НАЖМИТЕ НА ЯДРО ИЛИ СКАЖИТЕ «ПРИВЕТ, ДЖАРВИС»");}
    private void setState(String s){main.post(()->{if(status!=null)status.setText(s==null?"ГОТОВ":s);if(core!=null)core.setState(s);});}
    private void launchPackage(String pkg){try{Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}}catch(Throwable ignored){}}
    @Override protected void onResume(){super.onResume();applyImmersive();updateNetwork();}
    @Override protected void onDestroy(){try{if(recognizer!=null)recognizer.destroy();}catch(Throwable ignored){}if(engine!=null)engine.shutdown();if(voice!=null)voice.shutdown();super.onDestroy();}
}
