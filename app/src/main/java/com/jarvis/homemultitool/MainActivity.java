package com.jarvis.homemultitool;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.*;
import android.os.*;
import android.provider.Settings;
import android.speech.*;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import java.util.ArrayList;

/** JARVIS 5.30 — assistant-first UI. No quick-command clutter; composer follows the IME. */
public class MainActivity extends Activity {
    private static final int BG=Color.rgb(2,5,9), PANEL=Color.rgb(7,14,22), PANEL2=Color.rgb(9,19,29);
    private static final int CYAN=Color.rgb(91,220,255), BORDER=Color.rgb(27,83,119), WHITE=Color.rgb(244,248,252), MUTED=Color.rgb(126,151,174), GREEN=Color.rgb(70,226,174), RED=Color.rgb(255,100,120);
    private static final int REQ_MIC=42, REQ_NOTIFY=43;
    private final Handler main=new Handler(Looper.getMainLooper());
    private TextView status,network,replyTitle,replyBody,voiceHint;
    private EditText input; private SpeechRecognizer recognizer; private Intent recognizerIntent;
    private JarvisVoiceManager voice; private JarvisCoreView core; private JarvisEngine engine;
    private FrameLayout root; private View composer,nav;

    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(String s,float sp){TextView v=new TextView(this);v.setText(s);v.setTextColor(WHITE);v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,sp);v.setIncludeFontPadding(false);v.setGravity(Gravity.CENTER_VERTICAL);v.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);return v;}
    private GradientDrawable bg(int stroke,int fill,float radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));if(stroke>0)g.setStroke(dp(1),stroke);return g;}
    private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    private TextView button(String label,View.OnClickListener click){TextView v=text(label,13);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setSingleLine(true);v.setEllipsize(android.text.TextUtils.TruncateAt.END);v.setPadding(dp(10),0,dp(10),0);v.setBackground(bg(BORDER,0xF0091722,18));v.setOnClickListener(click);return v;}
    private TextView icon(String glyph,View.OnClickListener click){TextView v=button(glyph,click);v.setTextColor(CYAN);v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,21);return v;}

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);configureWindow();buildUi();
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIFY);
        engine=new JarvisEngine(this,new JarvisEngine.Callback(){public void reply(String s){speak(s);}public void state(String s){setState(s);}});
        voice=new JarvisVoiceManager(this);voice.init(null);initSpeech();handleIntent(getIntent());
    }
    private void configureWindow(){getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);if(Build.VERSION.SDK_INT>=29){getWindow().setStatusBarContrastEnforced(false);getWindow().setNavigationBarContrastEnforced(false);}getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handleIntent(i);}
    private void handleIntent(Intent i){if(i==null)return;String q=i.getStringExtra("WAKE_QUERY");String pkg=i.getStringExtra("LAUNCH_PACKAGE");if(q!=null&&!q.trim().isEmpty())main.postDelayed(()->command(q),120);else if(pkg!=null&&!pkg.trim().isEmpty())main.postDelayed(()->launchPackage(pkg),100);else if(i.getBooleanExtra("LOCKSCREEN_ASSIST",false))main.postDelayed(this::listen,300);}

    private void buildUi(){
        root=new FrameLayout(this);root.setBackgroundColor(BG);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(18),dp(12),dp(18),dp(176));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setBackgroundColor(BG);scroll.addView(page,new ScrollView.LayoutParams(-1,-1));root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(icon("☰",v->showTools()),new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.VERTICAL);brand.setGravity(Gravity.CENTER);
        TextView title=text("J A R V I S",23);title.setTextColor(CYAN);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER);title.setLetterSpacing(.20f);brand.addView(title,new LinearLayout.LayoutParams(-1,dp(29)));
        TextView sub=text("PERSONAL AI ASSISTANT",8);sub.setTextColor(MUTED);sub.setGravity(Gravity.CENTER);sub.setLetterSpacing(.14f);brand.addView(sub,new LinearLayout.LayoutParams(-1,dp(14)));
        header.addView(brand,new LinearLayout.LayoutParams(0,dp(48),1));header.addView(icon("⚙",v->openSettings()),new LinearLayout.LayoutParams(dp(48),dp(48)));page.addView(header,new LinearLayout.LayoutParams(-1,dp(48)));

        LinearLayout statusRow=new LinearLayout(this);statusRow.setGravity(Gravity.CENTER_VERTICAL);status=text("ГОТОВ",10);status.setTextColor(CYAN);status.setTypeface(Typeface.DEFAULT,Typeface.BOLD);statusRow.addView(status,new LinearLayout.LayoutParams(0,dp(24),1));network=text("●  В СЕТИ",10);network.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);network.setTypeface(Typeface.DEFAULT,Typeface.BOLD);statusRow.addView(network,new LinearLayout.LayoutParams(0,dp(24),1));page.addView(statusRow,lp(-1,24,2,7,2,0));updateNetwork();

        core=new JarvisCoreView(this);core.setState("ГОТОВ");core.startPulse();core.setOnClickListener(v->listen());page.addView(core,lp(-1,dp(270),0,4,0,0));
        voiceHint=text("Скажите «Привет, Джарвис» или нажмите на ядро",11);voiceHint.setTextColor(MUTED);voiceHint.setGravity(Gravity.CENTER);page.addView(voiceHint,new LinearLayout.LayoutParams(-1,dp(30)));

        LinearLayout reply=conversationCard();replyTitle=text("JARVIS",10);replyTitle.setTextColor(CYAN);replyTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);replyTitle.setLetterSpacing(.12f);reply.addView(replyTitle,new LinearLayout.LayoutParams(-1,dp(18)));replyBody=text("Готов к разговору.",17);replyBody.setLineSpacing(0,1.08f);reply.addView(replyBody,lp(-1,-2,0,6,0,1));page.addView(reply,lp(-1,-2,0,10,0,0));
        setContentView(root);buildBottom();
    }
    private LinearLayout conversationCard(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(14),dp(18),dp(14));c.setBackground(bg(BORDER,0xF006111A,22));return c;}

    private void buildBottom(){
        LinearLayout bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.HORIZONTAL);bottom.setGravity(Gravity.CENTER_VERTICAL);bottom.setPadding(dp(8),dp(8),dp(8),dp(8));bottom.setBackground(bg(BORDER,0xF3081520,24));
        input=new EditText(this);input.setSingleLine(true);input.setTextColor(WHITE);input.setHintTextColor(MUTED);input.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,17);input.setHint("Спросить JARVIS…");input.setGravity(Gravity.CENTER_VERTICAL);input.setPadding(dp(17),0,dp(10),0);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);input.setImeOptions(EditorInfo.IME_ACTION_SEND);input.setBackground(bg(0,0xEE0A1825,19));input.setOnEditorActionListener((v,id,e)->{if(id==EditorInfo.IME_ACTION_SEND){sendText();return true;}return false;});bottom.addView(input,new LinearLayout.LayoutParams(0,dp(70),1));
        TextView send=icon("➤",v->sendText());send.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,22);send.setBackground(bg(BORDER,0xFF0C2434,20));bottom.addView(send,lp(dp(70),dp(70),8,0,0,0));
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,dp(86),Gravity.BOTTOM);cp.setMargins(dp(12),0,dp(12),dp(78));root.addView(bottom,cp);composer=bottom;
        nav=new LinearLayout(this);((LinearLayout)nav).setGravity(Gravity.CENTER);nav.setPadding(dp(6),dp(5),dp(6),dp(5));nav.setBackground(bg(BORDER,0xF3081520,24));addNav((LinearLayout)nav,"◉","JARVIS",true,v->listen());addNav((LinearLayout)nav,"◌","ПАМЯТЬ",false,v->showMemory());addNav((LinearLayout)nav,"⌁","ИНСТРУМЕНТЫ",false,v->showTools());addNav((LinearLayout)nav,"⚙","НАСТРОЙКИ",false,v->openSettings());FrameLayout.LayoutParams np=new FrameLayout.LayoutParams(-1,dp(66),Gravity.BOTTOM);np.setMargins(dp(12),0,dp(12),dp(8));root.addView(nav,np);
        root.setOnApplyWindowInsetsListener((v,insets)->{int ime=0,bars=0;if(Build.VERSION.SDK_INT>=30){ime=insets.getInsets(WindowInsets.Type.ime()).bottom;bars=insets.getInsets(WindowInsets.Type.systemBars()).bottom;}boolean keyboard=ime>bars+dp(8);FrameLayout.LayoutParams cp2=(FrameLayout.LayoutParams)composer.getLayoutParams();cp2.bottomMargin=(keyboard?ime+dp(8):dp(78));composer.setLayoutParams(cp2);nav.setVisibility(keyboard?View.GONE:View.VISIBLE);return insets;});
        root.requestApplyInsets();
    }
    private void addNav(LinearLayout bar,String glyph,String label,boolean active,View.OnClickListener click){LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setGravity(Gravity.CENTER);item.setOnClickListener(click);TextView i=text(glyph,18);i.setGravity(Gravity.CENTER);i.setTextColor(active?CYAN:MUTED);item.addView(i,new LinearLayout.LayoutParams(-1,dp(29)));TextView t=text(label,9);t.setGravity(Gravity.CENTER);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setTextColor(active?WHITE:MUTED);item.addView(t,new LinearLayout.LayoutParams(-1,dp(15)));bar.addView(item,new LinearLayout.LayoutParams(0,dp(50),1));}

    private void showMemory(){String n=getSharedPreferences("jarvis_local",0).getString("notes","");String ctx=new JarvisMemory(this).recentContext();String body=(n.isEmpty()?"Локальные заметки пока пусты.":n)+(ctx.isEmpty()?"":"\n\nПоследний контекст:\n"+ctx);showInfoDialog("ПАМЯТЬ JARVIS",body);}
    private void showInfoDialog(String title,String body){Dialog d=new Dialog(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),dp(18),dp(20),dp(18));box.setBackground(bg(BORDER,0xFF07131E,26));TextView h=text(title,18);h.setTextColor(CYAN);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.setGravity(Gravity.CENTER);box.addView(h,new LinearLayout.LayoutParams(-1,dp(34)));TextView b=text(body,14);b.setGravity(Gravity.TOP);b.setLineSpacing(0,1.12f);ScrollView sc=new ScrollView(this);sc.addView(b);box.addView(sc,lp(-1,dp(260),0,10,0,10));box.addView(button("ЗАКРЫТЬ",v->d.dismiss()),new LinearLayout.LayoutParams(-1,dp(50)));d.setContentView(box);d.setOnShowListener(x->{Window w=d.getWindow();if(w!=null){WindowManager.LayoutParams a=w.getAttributes();a.width=(int)(getResources().getDisplayMetrics().widthPixels*.90f);a.height=WindowManager.LayoutParams.WRAP_CONTENT;w.setAttributes(a);w.setGravity(Gravity.CENTER);}});d.show();}
    private void showTools(){
        String tools="JARVIS работает командами естественной речью.\n\n"+
                "Примеры:\n«Поставь таймер на 5 минут»\n«Какая погода завтра в Берлине?»\n«Открой Яндекс Музыку и включи Miyagi»\n«Включи плейлист, который мне нравится»\n«Поставь музыку на паузу»\n«Найди сериал … где посмотреть»\n«Открой камеру»\n«Сделай громче»\n«Запомни, что …»\n«Научи: когда я говорю X, выполняй Y»\n\n"+
                "AI ACTION CENTER\n"+new JarvisActionCenter(this).formatted()+"\n\nДля полноценного управления активным плеером можно один раз дать JARVIS доступ к уведомлениям в настройках.";
        showInfoDialog("ИНСТРУМЕНТЫ JARVIS",tools);
    }

    private void openSettings(){try{startActivity(new Intent(this,SettingsActivity.class));}catch(Throwable e){safeSystem();}}
    private void safeSystem(){try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Throwable ignored){}}
    private void updateNetwork(){try{ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);Network n=cm==null?null:cm.getActiveNetwork();NetworkCapabilities c=cm==null||n==null?null:cm.getNetworkCapabilities(n);boolean ok=c!=null&&(c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)||c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));if(network!=null){network.setText(ok?"●  В СЕТИ":"●  НЕТ СЕТИ");network.setTextColor(ok?GREEN:RED);}}catch(Throwable ignored){}}

    private void initSpeech(){if(!SpeechRecognizer.isRecognitionAvailable(this)){setState("РЕЧЬ НЕДОСТУПНА");return;}recognizerIntent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ru-RU");recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"ru-RU");recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,false);recreateRecognizer();}
    private void recreateRecognizer(){try{if(recognizer!=null)recognizer.destroy();recognizer=Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this)?SpeechRecognizer.createOnDeviceSpeechRecognizer(this):SpeechRecognizer.createSpeechRecognizer(this);}catch(Throwable e){try{recognizer=SpeechRecognizer.createSpeechRecognizer(this);}catch(Throwable ignored){recognizer=null;}}if(recognizer==null)return;recognizer.setRecognitionListener(new RecognitionListener(){public void onReadyForSpeech(Bundle b){setState("СЛУШАЮ");main.post(()->{if(voiceHint!=null)voiceHint.setText("СЛУШАЮ • ГОВОРИТЕ");});}public void onBeginningOfSpeech(){setState("СЛУШАЮ");}public void onRmsChanged(float v){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){setState("ОБРАБОТКА");}public void onError(int e){setState("ГОТОВ");main.post(()->{if(voiceHint!=null)voiceHint.setText("Скажите «Привет, Джарвис» или нажмите на ядро");});}public void onResults(Bundle b){ArrayList<String> r=b==null?null:b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty())command(r.get(0));}public void onPartialResults(Bundle b){}public void onEvent(int a,Bundle b){}});}
    private void listen(){if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_MIC);return;}if(recognizer==null){recreateRecognizer();if(recognizer==null)return;}try{recognizer.cancel();recognizer.startListening(recognizerIntent);}catch(Throwable e){recreateRecognizer();}}
    private void command(String q){if(q==null||q.trim().isEmpty())return;String clean=q.trim();main.post(()->{if(input!=null)input.setText("");if(replyTitle!=null){replyTitle.setText("ЗАПРОС");replyTitle.setTextColor(MUTED);}if(replyBody!=null)replyBody.setText(clean);setState("ОБРАБОТКА");});if(engine!=null)engine.handle(clean);}
    private void sendText(){String q=input==null?"":input.getText().toString();if(q.trim().isEmpty()){listen();return;}command(q);}
    private void speak(String s){if(s==null||s.trim().isEmpty())return;main.post(()->{if(replyTitle!=null){replyTitle.setText("JARVIS");replyTitle.setTextColor(CYAN);}if(replyBody!=null)replyBody.setText(s);if(core!=null)core.setState("ОТВЕЧАЮ");if(voice!=null&&voice.isReady())voice.speak(s,null);});}
    private void setState(String s){main.post(()->{if(status!=null)status.setText(s==null?"ГОТОВ":s);if(core!=null)core.setState(s);});}
    private void launchPackage(String pkg){try{Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}}catch(Throwable ignored){}}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_MIC&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)main.postDelayed(this::listen,250);}
    @Override protected void onResume(){super.onResume();updateNetwork();}
    @Override protected void onDestroy(){try{if(recognizer!=null)recognizer.destroy();}catch(Throwable ignored){}if(engine!=null)engine.shutdown();if(voice!=null)voice.shutdown();super.onDestroy();}
}
