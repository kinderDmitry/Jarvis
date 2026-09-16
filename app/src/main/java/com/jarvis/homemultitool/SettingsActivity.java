package com.jarvis.homemultitool;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

/** Real, self-contained JARVIS control center. */
public class SettingsActivity extends Activity {
    private static final int BG=Color.rgb(2,5,9), PANEL=Color.rgb(7,16,26), BORDER=Color.rgb(35,94,137);
    private static final int CYAN=Color.rgb(82,214,255), WHITE=Color.rgb(242,247,255), MUTED=Color.rgb(128,151,173), GREEN=Color.rgb(67,225,173), RED=Color.rgb(255,103,119);
    private static final int REQ_MIC=531;
    private TextView roleState,wakeState,micState,voiceState,mediaState,learnedState,batteryState,knowledgeState;
    private JarvisVoiceManager voice;

    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(String s,float sp){TextView v=new TextView(this);v.setText(s);v.setTextColor(WHITE);v.setTextSize(TypedValue.COMPLEX_UNIT_SP,sp);v.setGravity(Gravity.CENTER_VERTICAL);v.setIncludeFontPadding(false);return v;}
    private GradientDrawable bg(int stroke,int fill,float radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));if(stroke>0)g.setStroke(dp(1),stroke);return g;}
    private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    private TextView button(String s,View.OnClickListener c){TextView v=text(s,12);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setSingleLine(true);v.setEllipsize(TextUtils.TruncateAt.END);v.setPadding(dp(10),0,dp(10),0);v.setBackground(bg(BORDER,0xF0091723,16));v.setOnClickListener(c);return v;}
    private TextView state(String s){TextView v=text(s,9);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setSingleLine(true);v.setTextColor(MUTED);return v;}

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        if(Build.VERSION.SDK_INT>=29){getWindow().setStatusBarContrastEnforced(false);getWindow().setNavigationBarContrastEnforced(false);}
        voice=new JarvisVoiceManager(this);voice.init(this::refresh);build();refresh();
    }

    private void build(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(14),dp(18),dp(30));scroll.addView(root,new ScrollView.LayoutParams(-1,-1));

        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(button("‹",v->finish()),new LinearLayout.LayoutParams(dp(50),dp(50)));
        LinearLayout titleBox=new LinearLayout(this);titleBox.setOrientation(LinearLayout.VERTICAL);titleBox.setGravity(Gravity.CENTER);
        TextView title=text("J A R V I S",23);title.setTextColor(CYAN);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER);titleBox.addView(title,new LinearLayout.LayoutParams(-1,dp(30)));
        TextView sub=text("CONTROL CENTER",8);sub.setTextColor(MUTED);sub.setGravity(Gravity.CENTER);titleBox.addView(sub,new LinearLayout.LayoutParams(-1,dp(16)));
        header.addView(titleBox,new LinearLayout.LayoutParams(0,dp(50),1));header.addView(button("⚙",v->open(Settings.ACTION_SETTINGS)),new LinearLayout.LayoutParams(dp(50),dp(50)));root.addView(header);

        TextView intro=text("СИСТЕМА  •  ГОЛОС  •  ВЫЗОВ  •  МЕДИА  •  ОБУЧЕНИЕ",9);intro.setTextColor(MUTED);intro.setTypeface(Typeface.DEFAULT,Typeface.BOLD);root.addView(intro,lp(-1,dp(22),2,10,0,8));

        roleState=state("ПРОВЕРКА");section(root,"СИСТЕМНЫЙ АССИСТЕНТ");
        statusCard(root,"JARVIS как помощник Android","Назначьте JARVIS системным помощником. После этого Android сможет вызывать его жестом/кнопкой помощника и через VoiceInteractionService, в том числе с экрана блокировки.",roleState,"ВЫБРАТЬ JARVIS",v->requestRole());

        wakeState=state("ВЫКЛЮЧЕН");section(root,"АКТИВАЦИЯ «ДЖАРВИС»");
        statusCard(root,"Фоновое ожидание «Джарвис»","Отдельный foreground-сервис слушает только для обнаружения имени помощника, затем открывает окно команды. Android сам ограничивает микрофон в фоне — JARVIS не скрывает эти ограничения.",wakeState,"ВКЛЮЧИТЬ / ВЫКЛЮЧИТЬ",v->toggleWake());
        addAction(root,"ОПТИМИЗАЦИЯ БАТАРЕИ",v->openBattery());batteryState=state("—");root.addView(batteryState,lp(-1,dp(24),0,0,0,8));

        micState=state("ПРОВЕРКА");section(root,"МИКРОФОН И РЕЧЬ");
        statusCard(root,"Микрофон", "Без разрешения RECORD_AUDIO голосовой вызов невозможен.",micState,"РАЗРЕШЕНИЕ МИКРОФОНА",v->requestMic());
        addVoice(root);

        mediaState=state("ПРОВЕРКА");section(root,"МЕДИА");
        statusCard(root,"Управление активным плеером","JARVIS использует Android MediaSession/MediaController: пауза, продолжение, следующий/предыдущий трек и лайк — когда конкретный плеер предоставляет эти возможности.",mediaState,"ОТКРЫТЬ ДОСТУП",v->open(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));

        section(root,"АДАПТИВНЫЙ ИНТЕЛЛЕКТ");
        LinearLayout learn=card();LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);TextView lt=text("Локальное обучение",16);lt.setTypeface(Typeface.DEFAULT,Typeface.BOLD);top.addView(lt,new LinearLayout.LayoutParams(0,dp(30),1));learnedState=state("0");top.addView(learnedState,new LinearLayout.LayoutParams(dp(100),dp(30)));learn.addView(top);
        TextView d=text("JARVIS автоматически запоминает успешные формулировки, контекст и предпочтения. Адаптивное ядро усиливает удачные трактовки, ослабляет ошибочные и кэширует полученные из интернета знания, чтобы повторные вопросы понимались быстрее.",11);d.setTextColor(MUTED);d.setLineSpacing(0,1.15f);learn.addView(d,lp(-1,-2,0,8,0,0));
        learn.addView(button("ПОКАЗАТЬ ПАМЯТЬ",v->showMemory()),lp(-1,dp(48),0,12,0,0));
        knowledgeState=state("0"); LinearLayout knowledgeRow=new LinearLayout(this); knowledgeRow.setGravity(Gravity.CENTER_VERTICAL); TextView kt=text("Интернет-знания в памяти",13); knowledgeRow.addView(kt,new LinearLayout.LayoutParams(0,dp(30),1)); knowledgeRow.addView(knowledgeState,new LinearLayout.LayoutParams(dp(105),dp(30))); learn.addView(knowledgeRow,lp(-1,dp(30),0,8,0,0));
        learn.addView(button("ОЧИСТИТЬ ПАМЯТЬ",v->{new JarvisMemory(this).clear();new JarvisAdaptiveBrain(this).clear();refresh();Toast.makeText(this,"Локальная память и адаптивные знания очищены.",Toast.LENGTH_SHORT).show();}),lp(-1,dp(48),0,8,0,0));
        root.addView(learn,lp(-1,-2,0,0,0,10));

        section(root,"СИСТЕМНЫЕ РАЗДЕЛЫ");
        addAction(root,"РОЛЬ ГОЛОСОВОГО ПОМОЩНИКА",v->open(Settings.ACTION_VOICE_INPUT_SETTINGS));
        addAction(root,"ГОЛОСОВОЙ ВВОД",v->open(Settings.ACTION_VOICE_INPUT_SETTINGS));
        addAction(root,"НАСТРОЙКИ СИНТЕЗА РЕЧИ",v->open("android.settings.TTS_SETTINGS"));
        addAction(root,"УВЕДОМЛЕНИЯ JARVIS",v->open(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()),new Intent(Settings.ACTION_SETTINGS)));
        addAction(root,"РАЗРЕШЕНИЯ JARVIS",v->open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())),new Intent(Settings.ACTION_SETTINGS)));
        if(Build.VERSION.SDK_INT>=31)addAction(root,"ТОЧНЫЕ БУДИЛЬНИКИ",v->open(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())),new Intent(Settings.ACTION_SETTINGS)));
        addAction(root,"WI-FI",v->open(Settings.ACTION_WIFI_SETTINGS));
        addAction(root,"BLUETOOTH",v->open(Settings.ACTION_BLUETOOTH_SETTINGS));
        addAction(root,"ЭКРАН",v->open(Settings.ACTION_DISPLAY_SETTINGS));
        TextView foot=text("Здесь нет фиктивных переключателей: статусы читаются непосредственно из Android.",10);foot.setTextColor(MUTED);root.addView(foot,lp(-1,-2,2,12,0,0));
        setContentView(scroll);
    }

    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(15),dp(16),dp(15));c.setBackground(bg(BORDER,0xF007141F,22));return c;}
    private void section(LinearLayout root,String s){TextView v=text(s,9);v.setTextColor(CYAN);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);root.addView(v,lp(-1,dp(20),2,6,0,5));}
    private void addAction(LinearLayout root,String s,View.OnClickListener c){root.addView(button(s,c),lp(-1,dp(50),0,0,0,8));}
    private void statusCard(LinearLayout root,String title,String desc,TextView status,String action,View.OnClickListener click){LinearLayout c=card();LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);TextView t=text(title,16);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);top.addView(t,new LinearLayout.LayoutParams(0,dp(30),1));top.addView(status,new LinearLayout.LayoutParams(dp(105),dp(30)));c.addView(top);TextView d=text(desc,11);d.setTextColor(MUTED);d.setLineSpacing(0,1.12f);c.addView(d,lp(-1,-2,0,7,0,0));c.addView(button(action,click),lp(-1,dp(48),0,13,0,0));root.addView(c,lp(-1,-2,0,0,0,10));}
    private void addVoice(LinearLayout root){LinearLayout c=card();LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);TextView t=text("Профиль голоса JARVIS",16);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);top.addView(t,new LinearLayout.LayoutParams(0,dp(30),1));voiceState=state("—");top.addView(voiceState,new LinearLayout.LayoutParams(dp(105),dp(30)));c.addView(top);TextView d=text("Используется реальный Android TTS. Можно выбрать доступный мужской профиль; точная копия голоса из фильма невозможна без отдельной лицензированной модели.",11);d.setTextColor(MUTED);d.setLineSpacing(0,1.12f);c.addView(d,lp(-1,-2,0,7,0,0));LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.addView(button("МУЖСКОЙ",v->{voice.setGender(JarvisVoiceManager.MALE);refresh();}),new LinearLayout.LayoutParams(0,dp(48),1));row.addView(button("ЖЕНСКИЙ",v->{voice.setGender(JarvisVoiceManager.FEMALE);refresh();}),lp(0,dp(48),8,0,0,0));c.addView(row,lp(-1,dp(48),0,13,0,0));c.addView(button("АВТО",v->{voice.setGender(JarvisVoiceManager.AUTO);refresh();}),lp(-1,dp(48),0,8,0,0));root.addView(c,lp(-1,-2,0,0,0,10));}

    private void requestRole(){
        if(Build.VERSION.SDK_INT>=29){try{RoleManager rm=getSystemService(RoleManager.class);if(rm!=null&&rm.isRoleAvailable(RoleManager.ROLE_ASSISTANT)){if(!rm.isRoleHeld(RoleManager.ROLE_ASSISTANT))startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT),41);else refresh();return;}}catch(Throwable ignored){}}
        open(Settings.ACTION_VOICE_INPUT_SETTINGS);
    }
    private void requestMic(){if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_MIC);else open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())),new Intent(Settings.ACTION_SETTINGS));}
    private void toggleWake(){
        if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){getSharedPreferences("jarvis",0).edit().putBoolean("wake_pending",true).apply();requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_MIC);return;}
        boolean next=!getSharedPreferences("jarvis",0).getBoolean("wake_enabled",false);getSharedPreferences("jarvis",0).edit().putBoolean("wake_enabled",next).apply();
        try{Intent i=new Intent(this,JarvisWakeWordService.class);if(next){if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}else stopService(i);}catch(Throwable e){getSharedPreferences("jarvis",0).edit().putBoolean("wake_enabled",false).apply();Toast.makeText(this,"Android не разрешил фоновый микрофон. Назначьте JARVIS помощником.",Toast.LENGTH_LONG).show();}refresh();
    }
    private void openBattery(){open(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:"+getPackageName())),new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));}
    private void open(String action){open(new Intent(action),new Intent(Settings.ACTION_SETTINGS));}
    private void open(String action,Intent fallback){open(new Intent(action),fallback);}
    private void open(Intent primary,Intent fallback){try{if(primary!=null&&primary.resolveActivity(getPackageManager())!=null){startActivity(primary);return;}}catch(Throwable ignored){}try{if(fallback!=null&&fallback.resolveActivity(getPackageManager())!=null){startActivity(fallback);return;}startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Throwable ignored){}}

    private void refresh(){
        try{
            boolean role=JarvisVoiceInteractionService.isActive(this);set(roleState,role?"АКТИВЕН":"НЕ ВЫБРАН",role?GREEN:MUTED);
            boolean wake=getSharedPreferences("jarvis",0).getBoolean("wake_enabled",false);set(wakeState,wake?"ВКЛЮЧЕН":"ВЫКЛЮЧЕН",wake?GREEN:MUTED);
            boolean mic=Build.VERSION.SDK_INT<23||checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;set(micState,mic?"РАЗРЕШЁН":"НЕТ",mic?GREEN:RED);
            if(voiceState!=null&&voice!=null){String g=voice.getGender();set(voiceState,JarvisVoiceManager.MALE.equals(g)?"МУЖСКОЙ":JarvisVoiceManager.FEMALE.equals(g)?"ЖЕНСКИЙ":"АВТО",CYAN);}
            boolean media=JarvisMediaSessionService.isConnected();set(mediaState,media?"ГОТОВ":"НЕТ ДОСТУПА",media?GREEN:MUTED);
            if(learnedState!=null){learnedState.setText(new JarvisMemory(this).learnedCount()+" ФРАЗ");learnedState.setTextColor(CYAN);}
            if(knowledgeState!=null){knowledgeState.setText(new JarvisAdaptiveBrain(this).knowledgeCount()+" ЗАПИСЕЙ");knowledgeState.setTextColor(CYAN);}
            if(batteryState!=null&&Build.VERSION.SDK_INT>=23){android.os.PowerManager pm=(android.os.PowerManager)getSystemService(POWER_SERVICE);boolean ok=pm!=null&&pm.isIgnoringBatteryOptimizations(getPackageName());set(batteryState,ok?"БАТАРЕЯ: ИГНОРИРУЕТСЯ":"БАТАРЕЯ: ОГРАНИЧЕНА",ok?GREEN:MUTED);}
        }catch(Throwable ignored){}
    }
    private void set(TextView v,String s,int color){if(v!=null){v.setText(s);v.setTextColor(color);}}
    private void showMemory(){JarvisMemory m=new JarvisMemory(this);JarvisAdaptiveBrain a=new JarvisAdaptiveBrain(this);String body="Изученных фраз: "+m.learnedCount()+"\nИнтернет-знаний: "+a.knowledgeCount()+"\n\n"+m.recentContext();if(body.endsWith("\n\n"))body+="Контекст пока пуст.";new AlertDialog.Builder(this).setTitle("ПАМЯТЬ JARVIS").setMessage(body).setPositiveButton("ПОНЯТНО",null).show();}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_MIC&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED&&getSharedPreferences("jarvis",0).getBoolean("wake_pending",false)){getSharedPreferences("jarvis",0).edit().putBoolean("wake_pending",false).putBoolean("wake_enabled",true).apply();try{Intent i=new Intent(this,JarvisWakeWordService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}catch(Throwable ignored){}}refresh();}
    @Override protected void onResume(){super.onResume();refresh();}
    @Override protected void onDestroy(){if(voice!=null)voice.shutdown();super.onDestroy();}
}
