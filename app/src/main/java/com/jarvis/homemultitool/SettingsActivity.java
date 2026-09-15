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

/** JARVIS 5.31 settings: defensive, real Android state, no fake toggles. */
public class SettingsActivity extends Activity {
    private static final int BG=Color.rgb(2,5,9), PANEL=Color.rgb(7,16,26), PANEL2=Color.rgb(9,20,31);
    private static final int CYAN=Color.rgb(82,214,255), BLUE=Color.rgb(35,94,137), WHITE=Color.rgb(242,247,255), MUTED=Color.rgb(128,151,173), GREEN=Color.rgb(67,225,173), RED=Color.rgb(255,103,119);
    private static final int REQ_MIC=531;
    private TextView roleState,wakeState,micState,voiceState,mediaState,learnedState;
    private JarvisVoiceManager voice;
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private TextView label(String s,float sp){TextView v=new TextView(this);v.setText(s);v.setTextColor(WHITE);v.setTextSize(TypedValue.COMPLEX_UNIT_SP,sp);v.setIncludeFontPadding(false);v.setGravity(Gravity.CENTER_VERTICAL);v.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);return v;}
    private GradientDrawable bg(int stroke,int fill,float radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));if(stroke>0)g.setStroke(dp(1),stroke);return g;}
    private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    private TextView button(String s,View.OnClickListener click){TextView v=label(s,12);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setSingleLine(true);v.setEllipsize(TextUtils.TruncateAt.END);v.setPadding(dp(12),0,dp(12),0);v.setBackground(bg(BLUE,0xF0091723,16));v.setOnClickListener(click);return v;}
    private TextView icon(String s,View.OnClickListener click){TextView v=label(s,22);v.setGravity(Gravity.CENTER);v.setTextColor(CYAN);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setBackground(bg(BLUE,0xF0091723,17));v.setOnClickListener(click);v.setContentDescription(s);return v;}

    @Override protected void onCreate(Bundle state){
        super.onCreate(state); getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        if(Build.VERSION.SDK_INT>=29){getWindow().setStatusBarContrastEnforced(false);getWindow().setNavigationBarContrastEnforced(false);}
        try{voice=new JarvisVoiceManager(this);voice.init(this::refresh);}catch(Throwable ignored){}
        try{build();}catch(Throwable fatal){ showBuildError(fatal); }
    }

    private void build(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);scroll.setClipToPadding(false);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(14),dp(18),dp(34));scroll.addView(root,new ScrollView.LayoutParams(-1,-1));

        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(icon("‹",v->finish()),new LinearLayout.LayoutParams(dp(50),dp(50)));
        LinearLayout titleBox=new LinearLayout(this);titleBox.setOrientation(LinearLayout.VERTICAL);titleBox.setGravity(Gravity.CENTER);
        TextView title=label("J A R V I S",23);title.setTextColor(CYAN);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER);title.setLetterSpacing(.18f);titleBox.addView(title,new LinearLayout.LayoutParams(-1,dp(29)));
        TextView sub=label("CONTROL CENTER",8);sub.setTextColor(MUTED);sub.setGravity(Gravity.CENTER);sub.setLetterSpacing(.20f);titleBox.addView(sub,new LinearLayout.LayoutParams(-1,dp(15)));
        header.addView(titleBox,new LinearLayout.LayoutParams(0,dp(50),1));header.addView(icon("⚙",v->safeOpen(new Intent(Settings.ACTION_SETTINGS),new Intent(Settings.ACTION_SETTINGS))),new LinearLayout.LayoutParams(dp(50),dp(50)));root.addView(header);

        TextView intro=label("СИСТЕМА  •  ГОЛОС  •  ВЫЗОВ  •  МЕДИА  •  ОБУЧЕНИЕ",9);intro.setTextColor(MUTED);intro.setTypeface(Typeface.DEFAULT,Typeface.BOLD);intro.setLetterSpacing(.05f);root.addView(intro,lp(-1,dp(22),2,10,0,8));

        roleState=state("НЕ ПРОВЕРЕНО");
        wakeState=state("ВЫКЛЮЧЕН");
        micState=state("ПРОВЕРКА");
        mediaState=state("ПРОВЕРКА");
        section(root,"СИСТЕМНЫЙ АССИСТЕНТ");
        addStatusCard(root,"JARVIS как помощник Android","Это главный путь системного вызова: Android может держать VoiceInteractionService живым и запускать JARVIS с жеста/кнопки помощника и с экрана блокировки.",roleState,"ВЫБРАТЬ JARVIS",v->requestRole());

        section(root,"АКТИВАЦИЯ «ДЖАРВИС»");
        addStatusCard(root,"«Джарвис» / «Привет, Джарвис»","Локальный wake-listener распознаёт обе фразы и затем передаёт следующую часть команды в движок. При наличии системного on-device распознавания оно используется в первую очередь.",wakeState,"ВКЛЮЧИТЬ / ВЫКЛЮЧИТЬ",v->toggleWake());
        addAction(root,"НАСТРОИТЬ БАТАРЕЮ ДЛЯ ФОНОВОГО ВЫЗОВА",v->openBattery());

        section(root,"МИКРОФОН И ГОЛОС");
        addStatusCard(root,"Микрофон","Без RECORD_AUDIO фоновый вызов и голосовые команды невозможны.",micState,"ОТКРЫТЬ РАЗРЕШЕНИЕ",v->requestMic());
        addVoiceCard(root);

        section(root,"МЕДИА-КОНТРОЛЬ");
        addStatusCard(root,"Управление активным плеером","JARVIS использует Android MediaSession/MediaController, поэтому пауза, продолжение, следующий и предыдущий трек работают с поддерживаемыми медиаплеерами.",mediaState,"ОТКРЫТЬ ДОСТУП",v->safeOpen(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),new Intent(Settings.ACTION_SETTINGS)));

        section(root,"АДАПТИВНАЯ ПАМЯТЬ");
        LinearLayout learn=card();
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);TextView lt=label("Локальное обучение",16);lt.setTypeface(Typeface.DEFAULT,Typeface.BOLD);top.addView(lt,new LinearLayout.LayoutParams(0,dp(30),1));learnedState=state("0 КОМАНД");top.addView(learnedState,new LinearLayout.LayoutParams(dp(105),dp(30)));learn.addView(top);
        TextView ld=label("JARVIS запоминает ваши предпочтения, псевдонимы команд, выбранное музыкальное приложение и контекст последних диалогов. Это персональная адаптация, а не обещание самостоятельного переобучения большой языковой модели.",11);ld.setTextColor(MUTED);ld.setLineSpacing(0,1.15f);learn.addView(ld,lp(-1,-2,0,8,0,0));
        learn.addView(button("ПОКАЗАТЬ ПАМЯТЬ",v->showMemory()),lp(-1,dp(48),0,12,0,0));
        learn.addView(button("ОЧИСТИТЬ АДАПТИВНУЮ ПАМЯТЬ",v->{new JarvisMemory(this).clear();refresh();Toast.makeText(this,"Локальная адаптивная память очищена.",Toast.LENGTH_SHORT).show();}),lp(-1,dp(48),0,8,0,0));
        root.addView(learn,lp(-1,-2,0,0,0,10));

        section(root,"СИСТЕМНЫЕ РАЗДЕЛЫ");
        addAction(root,"ГОЛОСОВОЙ ВВОД",v->safeOpen(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),new Intent(Settings.ACTION_SETTINGS)));
        addAction(root,"УВЕДОМЛЕНИЯ JARVIS",v->safeOpen(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()),new Intent(Settings.ACTION_SETTINGS)));
        addAction(root,"РАЗРЕШЕНИЯ JARVIS",v->safeOpen(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())),new Intent(Settings.ACTION_APPLICATION_SETTINGS)));
        if(Build.VERSION.SDK_INT>=31)addAction(root,"ТОЧНЫЕ БУДИЛЬНИКИ",v->safeOpen(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())),new Intent(Settings.ACTION_SETTINGS)));
        addAction(root,"ЭКРАН",v->safeOpen(new Intent(Settings.ACTION_DISPLAY_SETTINGS),new Intent(Settings.ACTION_SETTINGS)));
        addAction(root,"WI-FI",v->safeOpen(new Intent(Settings.ACTION_WIFI_SETTINGS),new Intent(Settings.ACTION_SETTINGS)));
        addAction(root,"BLUETOOTH",v->safeOpen(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS),new Intent(Settings.ACTION_SETTINGS)));

        TextView foot=label("Все статусы выше читаются из Android. JARVIS не показывает фиктивное состояние «включено», если ОС фактически не дала доступ.",10);foot.setTextColor(MUTED);foot.setLineSpacing(0,1.15f);root.addView(foot,lp(-1,-2,2,12,0,0));
        setContentView(scroll);refresh();
    }

    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(15),dp(16),dp(15));c.setBackground(bg(BLUE,0xF007141F,22));return c;}
    private TextView state(String s){TextView v=label(s,9);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setSingleLine(true);v.setTextColor(MUTED);return v;}
    private void section(LinearLayout root,String s){TextView v=label(s,9);v.setTextColor(CYAN);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setLetterSpacing(.08f);root.addView(v,lp(-1,dp(20),2,6,0,5));}
    private void addAction(LinearLayout root,String s,View.OnClickListener c){root.addView(button(s,c),lp(-1,dp(50),0,0,0,8));}
    private void addStatusCard(LinearLayout root,String title,String desc,TextView state,String action,View.OnClickListener click){LinearLayout c=card();LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);TextView t=label(title,16);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);top.addView(t,new LinearLayout.LayoutParams(0,dp(30),1));top.addView(state,new LinearLayout.LayoutParams(dp(105),dp(30)));c.addView(top);TextView d=label(desc,11);d.setTextColor(MUTED);d.setLineSpacing(0,1.12f);c.addView(d,lp(-1,-2,0,7,0,0));c.addView(button(action,click),lp(-1,dp(48),0,13,0,0));root.addView(c,lp(-1,-2,0,0,0,10));}
    private void addVoiceCard(LinearLayout root){LinearLayout c=card();LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);TextView t=label("Профиль TTS",16);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);top.addView(t,new LinearLayout.LayoutParams(0,dp(30),1));voiceState=state("—");top.addView(voiceState,new LinearLayout.LayoutParams(dp(105),dp(30)));c.addView(top);TextView d=label("Используется реальный голосовой движок Android. Выбор пола влияет на доступный голос, скорость и высоту речи, если установленный TTS предоставляет такие голоса.",11);d.setTextColor(MUTED);d.setLineSpacing(0,1.12f);c.addView(d,lp(-1,-2,0,7,0,0));LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView male=button("МУЖСКОЙ",v->{if(voice!=null)voice.setGender(JarvisVoiceManager.MALE);refresh();});TextView female=button("ЖЕНСКИЙ",v->{if(voice!=null)voice.setGender(JarvisVoiceManager.FEMALE);refresh();});row.addView(male,new LinearLayout.LayoutParams(0,dp(48),1));row.addView(female,lp(0,dp(48),8,0,0,0));c.addView(row,lp(-1,dp(48),0,13,0,0));c.addView(button("АВТОМАТИЧЕСКИЙ ПРОФИЛЬ",v->{if(voice!=null)voice.setGender(JarvisVoiceManager.AUTO);refresh();}),lp(-1,dp(48),0,8,0,0));c.addView(button("НАСТРОЙКИ СИНТЕЗА РЕЧИ",v->safeOpen(new Intent("android.settings.TTS_SETTINGS"),new Intent(Settings.ACTION_SETTINGS))),lp(-1,dp(48),0,8,0,0));root.addView(c,lp(-1,-2,0,0,0,10));}

    private void requestRole(){
        if(Build.VERSION.SDK_INT>=29){try{RoleManager rm=getSystemService(RoleManager.class);if(rm!=null&&rm.isRoleAvailable(RoleManager.ROLE_ASSISTANT)){if(!rm.isRoleHeld(RoleManager.ROLE_ASSISTANT))startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT),41);else refresh();return;}}catch(Throwable ignored){}}
        safeOpen(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),new Intent(Settings.ACTION_SETTINGS));
    }
    private void requestMic(){if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_MIC);else safeOpen(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())),new Intent(Settings.ACTION_APPLICATION_SETTINGS));}
    private void toggleWake(){
        if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){getSharedPreferences("jarvis",0).edit().putBoolean("wake_pending",true).apply();requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_MIC);return;}
        boolean on=getSharedPreferences("jarvis",0).getBoolean("wake_enabled",false);boolean next=!on;getSharedPreferences("jarvis",0).edit().putBoolean("wake_enabled",next).apply();
        try{Intent i=new Intent(this,JarvisWakeWordService.class);if(next){if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}else stopService(i);}catch(Throwable e){getSharedPreferences("jarvis",0).edit().putBoolean("wake_enabled",false).apply();Toast.makeText(this,"Android не разрешил запуск фонового микрофона. Назначьте JARVIS помощником и проверьте разрешение микрофона.",Toast.LENGTH_LONG).show();}
        refresh();
    }
    private void openBattery(){safeOpen(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:"+getPackageName())),new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));}
    private void safeOpen(Intent primary,Intent fallback){try{if(primary!=null&&primary.resolveActivity(getPackageManager())!=null){startActivity(primary);return;}}catch(Throwable ignored){}try{if(fallback!=null&&fallback.resolveActivity(getPackageManager())!=null){startActivity(fallback);return;}startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Throwable ignored){}}
    private void refresh(){
        try{
            boolean role=JarvisVoiceInteractionService.isActive(this); if(roleState!=null){roleState.setText(role?"АКТИВЕН":"НЕ ВЫБРАН");roleState.setTextColor(role?GREEN:MUTED);}
            boolean wake=getSharedPreferences("jarvis",0).getBoolean("wake_enabled",false);if(wakeState!=null){wakeState.setText(wake?"ВКЛЮЧЕН":"ВЫКЛЮЧЕН");wakeState.setTextColor(wake?GREEN:MUTED);}
            boolean mic=Build.VERSION.SDK_INT<23||checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;if(micState!=null){micState.setText(mic?"РАЗРЕШЁН":"НЕТ");micState.setTextColor(mic?GREEN:RED);}
            if(voiceState!=null&&voice!=null){String g=voice.getGender();voiceState.setText(JarvisVoiceManager.MALE.equals(g)?"МУЖСКОЙ":JarvisVoiceManager.FEMALE.equals(g)?"ЖЕНСКИЙ":"АВТО");voiceState.setTextColor(CYAN);}
            if(mediaState!=null){boolean m=JarvisMediaSessionService.isConnected();mediaState.setText(m?"ГОТОВ":"НЕТ ДОСТУПА");mediaState.setTextColor(m?GREEN:MUTED);}
            if(learnedState!=null){int n=new JarvisMemory(this).learnedCount();learnedState.setText(n+" КОМАНД");learnedState.setTextColor(CYAN);}
        }catch(Throwable ignored){}
    }
    private void showMemory(){JarvisMemory m=new JarvisMemory(this);String body="Изученных команд: "+m.learnedCount()+"\n\n"+m.recentContext();if(body.endsWith("\n\n"))body+="Контекст пока пуст.";new AlertDialog.Builder(this).setTitle("ПАМЯТЬ JARVIS").setMessage(body).setPositiveButton("ПОНЯТНО",null).show();}
    private void showBuildError(Throwable fatal){
        new Handler(Looper.getMainLooper()).post(()->{
            try{
                String msg=fatal==null?"Неизвестная ошибка":String.valueOf(fatal.getMessage());
                new AlertDialog.Builder(this).setTitle("НАСТРОЙКИ JARVIS")
                        .setMessage("Не удалось построить внутреннюю панель настроек. Попробуйте ещё раз.\n\n"+msg)
                        .setPositiveButton("ПОВТОРИТЬ",(d,w)->{try{build();}catch(Throwable t){showBuildError(t);}})
                        .setNegativeButton("ANDROID",(d,w)->safeOpen(new Intent(Settings.ACTION_SETTINGS),new Intent(Settings.ACTION_SETTINGS)))
                        .show();
            }catch(Throwable ignored){safeOpen(new Intent(Settings.ACTION_SETTINGS),new Intent(Settings.ACTION_SETTINGS));}
        });
    }

    private void showSafeSettings(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(20),dp(20),dp(20));root.setBackgroundColor(BG);TextView h=label("J A R V I S",24);h.setTextColor(CYAN);h.setGravity(Gravity.CENTER);root.addView(h,new LinearLayout.LayoutParams(-1,dp(50)));TextView t=label("Экран настроек не удалось построить. Системные параметры остаются доступны ниже.",15);t.setGravity(Gravity.CENTER);t.setTextColor(WHITE);root.addView(t,lp(-1,dp(100),0,20,0,20));root.addView(button("ОТКРЫТЬ НАСТРОЙКИ ANDROID",v->safeOpen(new Intent(Settings.ACTION_SETTINGS),new Intent(Settings.ACTION_SETTINGS))),new LinearLayout.LayoutParams(-1,dp(52)));setContentView(root);}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_MIC&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED&&getSharedPreferences("jarvis",0).getBoolean("wake_pending",false)){getSharedPreferences("jarvis",0).edit().putBoolean("wake_pending",false).putBoolean("wake_enabled",true).apply();try{Intent i=new Intent(this,JarvisWakeWordService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}catch(Throwable ignored){}}refresh();}
    @Override protected void onResume(){super.onResume();refresh();}
    @Override protected void onDestroy(){if(voice!=null)voice.shutdown();super.onDestroy();}
}
