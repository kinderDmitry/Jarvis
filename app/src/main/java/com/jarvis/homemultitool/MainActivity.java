package com.jarvis.homemultitool;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.speech.*;
import android.speech.tts.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int BG=Color.rgb(2,5,10), PANEL=Color.rgb(7,13,22), BLUE=Color.rgb(74,159,255), TEXT=Color.WHITE, MUTED=Color.rgb(135,155,180);
    private TextView status, transcript, response; private EditText input; private SpeechRecognizer sr; private Intent speechIntent; private TextToSpeech tts; private JarvisCoreView core;
    private JarvisEngine engine;
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);boolean lock=getIntent().getBooleanExtra("LOCKSCREEN_ASSIST",false);if(lock)getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);buildUi();engine=new JarvisEngine(this,new JarvisEngine.Callback(){public void reply(String s){speak(s);}public void state(String s){status.setText(s);}});initTts();initSpeech();if(lock)new Handler(Looper.getMainLooper()).postDelayed(this::listen,450);}
    private TextView tv(String s,float size){TextView v=new TextView(this);v.setText(s);v.setTextColor(TEXT);v.setTextSize(size);return v;}
    private GradientDrawable box(int stroke){GradientDrawable g=new GradientDrawable();g.setColor(PANEL);g.setCornerRadius(30);g.setStroke(1,stroke);return g;}
    private void buildUi(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,14,20,12);root.setBackgroundColor(BG);
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);TextView title=tv("J A R V I S",26);title.setTypeface(null,1);title.setTextColor(BLUE);head.addView(title,new LinearLayout.LayoutParams(0,54,1));TextView gear=tv("⚙",22);gear.setGravity(Gravity.CENTER);gear.setOnClickListener(v->openAssistantSettings());head.addView(gear,new LinearLayout.LayoutParams(52,54));root.addView(head);
        TextView sub=tv("LOCAL AI CORE  •  NO CLOUD  •  NO API",10);sub.setTextColor(MUTED);root.addView(sub);
        core=new JarvisCoreView(this);core.startPulse();root.addView(core,new LinearLayout.LayoutParams(-1,245));
        status=tv("ГОТОВ",12);status.setGravity(Gravity.CENTER);status.setTextColor(MUTED);root.addView(status,new LinearLayout.LayoutParams(-1,34));
        transcript=tv("Вы: —",14);transcript.setTextColor(MUTED);transcript.setPadding(16,12,16,12);transcript.setBackground(box(Color.rgb(24,55,90)));root.addView(transcript);
        response=tv("Я на связи. Говорите.",17);response.setPadding(18,14,18,14);response.setBackground(box(Color.rgb(28,75,122)));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,92);rp.topMargin=8;root.addView(response,rp);
        LinearLayout row=new LinearLayout(this);row.setPadding(0,9,0,0);input=new EditText(this);input.setSingleLine(true);input.setHint("Напишите JARVIS…");input.setHintTextColor(MUTED);input.setTextColor(TEXT);input.setTextSize(15);input.setPadding(18,0,10,0);input.setBackground(box(Color.rgb(24,55,90)));row.addView(input,new LinearLayout.LayoutParams(0,58,1));Button send=button("➤");send.setOnClickListener(v->{String s=input.getText().toString().trim();if(!s.isEmpty()){input.setText("");command(s);}});row.addView(send,new LinearLayout.LayoutParams(62,58));root.addView(row);
        Button mic=button("●  ГОВОРИТЬ");mic.setTextSize(14);mic.setOnClickListener(v->listen());LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,60);mp.topMargin=9;root.addView(mic,mp);
        TextView foot=tv("Все основные функции выполняются локально. Тяжёлые модули не загружаются без необходимости.",10);foot.setTextColor(MUTED);foot.setPadding(4,10,4,0);root.addView(foot);setContentView(root);}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setAllCaps(false);b.setBackground(box(Color.rgb(32,82,135)));return b;}
    private void initTts(){tts=new TextToSpeech(this,r->{if(r==TextToSpeech.SUCCESS){tts.setLanguage(new Locale("ru","RU"));tts.setSpeechRate(.86f);tts.setPitch(.72f);tts.setAudioAttributes(new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ASSISTANT).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build());}});}
    private void initSpeech(){if(!SpeechRecognizer.isRecognitionAvailable(this))return;sr=SpeechRecognizer.createSpeechRecognizer(this);sr.setRecognitionListener(new RecognitionListener(){public void onReadyForSpeech(Bundle b){status.setText("СЛУШАЮ");}public void onBeginningOfSpeech(){}public void onRmsChanged(float v){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){status.setText("ОБРАБОТКА");}public void onError(int e){status.setText("ГОТОВ");}public void onResults(Bundle b){ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty())command(r.get(0));}public void onPartialResults(Bundle b){}public void onEvent(int a,Bundle b){}});speechIntent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ru-RU");speechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);}
    private void listen(){if(sr==null){speak("Распознавание речи недоступно на этом устройстве.");return;}if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},10);return;}try{sr.startListening(speechIntent);}catch(Exception e){status.setText("ГОТОВ");}}
    private void command(String s){transcript.setText("Вы: "+s);engine.handle(s);}
    private void speak(String s){response.setText(s);status.setText("JARVIS • ГОТОВ");if(tts!=null)tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"jarvis");}
    private void openAssistantSettings(){try{startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}
    @Override protected void onDestroy(){if(sr!=null)sr.destroy();if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
}
