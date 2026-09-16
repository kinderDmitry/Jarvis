package com.jarvis.homemultitool;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.speech.*;
import android.view.*;
import android.widget.*;
import java.util.ArrayList;

/** Compact assistant surface used by Android's VoiceInteractionService, including keyguard. */
public class JarvisVoiceSession extends android.service.voice.VoiceInteractionSession {
    private SpeechRecognizer recognizer; private JarvisEngine engine; private JarvisVoiceManager voice;
    private TextView status,transcript,answer; private boolean active,listening;
    private final Handler main=new Handler(Looper.getMainLooper());
    public JarvisVoiceSession(Context context){super(context);try{setTheme(android.R.style.Theme_DeviceDefault_NoActionBar);}catch(Throwable ignored){}}

    private GradientDrawable panel(){GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(5,14,22));g.setCornerRadius(42);g.setStroke(2,Color.rgb(35,94,137));return g;}
    private TextView tv(String s,float size,int color){TextView v=new TextView(getContext());v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.CENTER);v.setIncludeFontPadding(false);return v;}

    @Override public View onCreateContentView(){
        FrameLayout root=new FrameLayout(getContext());root.setBackgroundColor(Color.TRANSPARENT);root.setPadding(24,24,24,28);
        LinearLayout card=new LinearLayout(getContext());card.setOrientation(LinearLayout.VERTICAL);card.setGravity(Gravity.CENTER);card.setPadding(28,22,28,22);card.setBackground(panel());
        TextView title=tv("J A R V I S",22,Color.rgb(82,214,255));title.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);card.addView(title,new LinearLayout.LayoutParams(-1,48));
        status=tv("ГОТОВ",12,Color.WHITE);card.addView(status,new LinearLayout.LayoutParams(-1,34));
        transcript=tv("",15,Color.LTGRAY);card.addView(transcript,new LinearLayout.LayoutParams(-1,58));
        answer=tv("",16,Color.WHITE);answer.setMaxLines(4);card.addView(answer,new LinearLayout.LayoutParams(-1,88));
        TextView hint=tv("Голосовой режим • приложение не открывается",10,Color.rgb(110,140,160));card.addView(hint,new LinearLayout.LayoutParams(-1,32));
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,300,Gravity.BOTTOM);root.addView(card,cp);
        try{android.app.Dialog d=getWindow();Window w=d==null?null:d.getWindow();if(w!=null){w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));WindowManager.LayoutParams a=w.getAttributes();a.dimAmount=0f;w.setAttributes(a);w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);}}catch(Throwable ignored){}
        return root;
    }

    @Override public void onShow(Bundle args,int flags){
        super.onShow(args,flags);active=true;
        engine=new JarvisEngine(getContext(),new JarvisEngine.Callback(){public void reply(String s){main.post(()->{if(answer!=null)answer.setText(s);});speak(s);}public void state(String s){main.post(()->{if(status!=null)status.setText(s==null?"ГОТОВ":s);});}});
        voice=new JarvisVoiceManager(getContext());voice.init(null);main.postDelayed(this::startRecognition,150);
    }
    private void speak(String s){if(!active||s==null||s.trim().isEmpty())return;main.post(()->{if(voice!=null&&voice.isReady())voice.speak(s,new android.speech.tts.UtteranceProgressListener(){public void onStart(String id){}public void onDone(String id){}public void onError(String id){}});});}
    private void startRecognition(){
        if(!active)return;
        if(Build.VERSION.SDK_INT>=23&&getContext().checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){status.setText("НУЖЕН МИКРОФОН");return;}
        try{if(recognizer!=null)recognizer.destroy();recognizer=Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(getContext())?SpeechRecognizer.createOnDeviceSpeechRecognizer(getContext()):SpeechRecognizer.createSpeechRecognizer(getContext());recognizer.setRecognitionListener(listener);Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ru-RU").putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5).putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true).putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,false);recognizer.startListening(i);}catch(Throwable e){status.setText("РЕЧЬ НЕДОСТУПНА");}}
    private final RecognitionListener listener=new RecognitionListener(){
        public void onReadyForSpeech(Bundle b){listening=true;main.post(()->status.setText("СЛУШАЮ"));}
        public void onBeginningOfSpeech(){main.post(()->status.setText("СЛУШАЮ"));}
        public void onRmsChanged(float v){}public void onBufferReceived(byte[] b){}
        public void onEndOfSpeech(){listening=false;main.post(()->status.setText("ОБРАБОТКА"));}
        public void onError(int e){listening=false;if(active)main.postDelayed(()->{if(active)startRecognition();},500);}
        public void onResults(Bundle b){ArrayList<String> r=b==null?null:b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty()){String q=r.get(0);main.post(()->transcript.setText(q));if(engine!=null)engine.handle(q);}listening=false;}
        public void onPartialResults(Bundle b){}
        public void onEvent(int a,Bundle b){}
    };
    @Override public void onHide(){active=false;try{if(recognizer!=null)recognizer.cancel();}catch(Throwable ignored){}try{if(recognizer!=null)recognizer.destroy();}catch(Throwable ignored){}recognizer=null;if(engine!=null)engine.shutdown();if(voice!=null)voice.shutdown();super.onHide();}
}
