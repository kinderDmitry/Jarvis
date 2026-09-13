package com.jarvis.homemultitool;

import android.Manifest;
import android.app.Activity;
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

/** Premium, geometry-safe settings. Every control is a real action or a concrete Android settings page. */
public class SettingsActivity extends Activity {
    private static final int BG=Color.rgb(2,6,11), PANEL=Color.rgb(6,16,27), PANEL2=Color.rgb(7,20,32);
    private static final int CYAN=Color.rgb(83,220,255), BORDER=Color.rgb(25,104,154), WHITE=Color.rgb(242,247,255);
    private static final int MUTED=Color.rgb(122,153,181), GREEN=Color.rgb(71,232,177), RED=Color.rgb(255,103,119);
    private static final int REQ_MIC=51;
    private TextView roleState,wakeState,micState,voiceState;
    private JarvisVoiceManager voice;

    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private TextView label(String s,float sp){
        TextView v=new TextView(this); v.setText(s); v.setTextColor(WHITE);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP,sp); v.setIncludeFontPadding(false);
        v.setGravity(Gravity.CENTER_VERTICAL); v.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE); return v;
    }
    private GradientDrawable bg(int stroke,int fill,float radius){
        GradientDrawable g=new GradientDrawable(); g.setColor(fill); g.setCornerRadius(dp(radius));
        if(stroke>0)g.setStroke(dp(1),stroke); return g;
    }
    private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;
    }
    private TextView button(String text,View.OnClickListener click){
        TextView v=label(text,13);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        v.setSingleLine(true);v.setEllipsize(TextUtils.TruncateAt.END);v.setPadding(dp(12),0,dp(12),0);
        v.setMinHeight(dp(48));v.setBackground(bg(BORDER,0xF0071622,17));v.setOnClickListener(click);
        if(Build.VERSION.SDK_INT>=26)v.setAutoSizeTextTypeUniformWithConfiguration(dp(11),dp(14),dp(1),TypedValue.COMPLEX_UNIT_PX);
        return v;
    }
    private TextView icon(String glyph,View.OnClickListener click){TextView v=button(glyph,click);v.setTextColor(CYAN);v.setTextSize(TypedValue.COMPLEX_UNIT_SP,24);v.setPadding(0,0,0,0);return v;}

    private void immersive(){
        try{if(Build.VERSION.SDK_INT>=30){getWindow().setDecorFitsSystemWindows(false);WindowInsetsController c=getWindow().getInsetsController();if(c!=null){c.hide(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}}
        else getWindow().getDecorView().setSystemUiVisibility(5894|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}catch(Throwable ignored){}
    }

    @Override protected void onCreate(Bundle state){
        super.onCreate(state); immersive(); getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        voice=new JarvisVoiceManager(this);voice.init(this::refresh);build();
    }

    private void build(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setBackgroundColor(BG);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(14),dp(18),dp(30));

        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(icon("‹",v->finish()),new LinearLayout.LayoutParams(dp(54),dp(54)));
        TextView title=label("J A R V I S",24);title.setTextColor(CYAN);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER);title.setLetterSpacing(.18f);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(54),1));
        header.addView(icon("⚙",v->safeOpen(new Intent(Settings.ACTION_SETTINGS),new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:"+getPackageName())))),new LinearLayout.LayoutParams(dp(54),dp(54)));
        root.addView(header);

        TextView sub=label("СИСТЕМА  •  ГОЛОС  •  ВЫЗОВ  •  ПРИВАТНОСТЬ",10);sub.setTextColor(MUTED);sub.setTypeface(Typeface.DEFAULT,Typeface.BOLD);sub.setLetterSpacing(.055f);root.addView(sub,lp(-1,24,2,7,0,4));

        section(root,"СИСТЕМНЫЙ АССИСТЕНТ");
        LinearLayout role=card();roleState=state("—");info(role,"JARVIS как помощник Android","Назначение JARVIS системным помощником позволяет вызывать его системной кнопкой ассистента.",roleState);actionRow(role,"ОТКРЫТЬ НАСТРОЙКУ",v->requestRole());root.addView(role,lp(-1,-2,0,0,0,10));

        section(root,"ВЫЗОВ «ПРИВЕТ, ДЖАРВИС»");
        LinearLayout wake=card();wakeState=state("—");info(wake,"Фоновое ожидание фразы","JARVIS слушает короткими сессиями через foreground-сервис и ищет «Джарвис». Для работы поверх других приложений нужны микрофон и разрешение работы без жёстких ограничений батареи.",wakeState);
        actionRow(wake,"ВКЛЮЧИТЬ / ВЫКЛЮЧИТЬ",v->toggleWake());actionRow(wake,"НАСТРОИТЬ БАТАРЕЮ",v->openBattery());root.addView(wake,lp(-1,-2,0,0,0,10));

        section(root,"МИКРОФОН");
        LinearLayout mic=card();micState=state("—");info(mic,"Разрешение микрофона","Реальный микрофон Android используется для распознавания речи и wake-word режима.",micState);actionRow(mic,"ОТКРЫТЬ РАЗРЕШЕНИЯ",v->requestMic());root.addView(mic,lp(-1,-2,0,0,0,10));

        section(root,"ГОЛОС JARVIS");
        LinearLayout vc=card();voiceState=state("—");info(vc,"Профиль ответа","JARVIS использует реально установленный русский TTS-голос. Профили меняют доступный тембр и скорость; искусственных аудиофайлов нет.",voiceState);
        LinearLayout profiles=new LinearLayout(this);profiles.setOrientation(LinearLayout.HORIZONTAL);profiles.setGravity(Gravity.CENTER_VERTICAL);
        TextView male=button("МУЖСКОЙ",v->{voice.setGender(JarvisVoiceManager.MALE);voice.speak("Мужской профиль JARVIS активирован.",null);refresh();});
        TextView female=button("ЖЕНСКИЙ",v->{voice.setGender(JarvisVoiceManager.FEMALE);voice.speak("Женский профиль JARVIS активирован.",null);refresh();});
        profiles.addView(male,new LinearLayout.LayoutParams(0,dp(48),1));profiles.addView(female,lp(0,48,8,0,0,0));vc.addView(profiles,lp(-1,48,0,10,0,0));
        actionRow(vc,"АВТОМАТИЧЕСКИЙ ПРОФИЛЬ",v->{voice.setGender(JarvisVoiceManager.AUTO);refresh();});actionRow(vc,"ОТКРЫТЬ НАСТРОЙКИ TTS",v->openTtsSettings());root.addView(vc,lp(-1,-2,0,0,0,10));

        section(root,"СИСТЕМНЫЕ РАЗДЕЛЫ");
        setting(root,"ГОЛОСОВОЙ ВВОД",new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),new Intent(Settings.ACTION_SETTINGS));
        setting(root,"УВЕДОМЛЕНИЯ JARVIS",new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()).setData(Uri.parse("package:"+getPackageName())),new Intent(Settings.ACTION_SETTINGS));
        setting(root,"РАЗРЕШЕНИЯ JARVIS",new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:"+getPackageName())),new Intent(Settings.ACTION_APPLICATION_SETTINGS));
        if(Build.VERSION.SDK_INT>=31)setting(root,"ТОЧНЫЕ БУДИЛЬНИКИ",new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())),new Intent(Settings.ACTION_SETTINGS));
        setting(root,"ОПТИМИЗАЦИЯ БАТАРЕИ",new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:"+getPackageName())),new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        setting(root,"ЭКРАН",new Intent(Settings.ACTION_DISPLAY_SETTINGS),new Intent(Settings.ACTION_SETTINGS));
        setting(root,"WI-FI",new Intent(Settings.ACTION_WIFI_SETTINGS),new Intent(Settings.ACTION_SETTINGS));
        setting(root,"BLUETOOTH",new Intent(Settings.ACTION_BLUETOOTH_SETTINGS),new Intent(Settings.ACTION_SETTINGS));

        TextView foot=label("Каждый элемент выше выполняет реальное действие: настройка JARVIS, разрешение, системный раздел или безопасный Android fallback. Фиктивных переключателей нет.",11);foot.setTextColor(MUTED);foot.setLineSpacing(0,1.12f);root.addView(foot,lp(-1,-2,2,12,0,0));
        scroll.addView(root,new ScrollView.LayoutParams(-1,-1));setContentView(scroll);refresh();
    }

    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(15),dp(16),dp(14));c.setBackground(bg(BORDER,0xED06131F,24));return c;}
    private void info(LinearLayout card,String title,String desc,TextView state){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.TOP);
        LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);
        TextView t=label(title,16);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setMaxLines(2);t.setEllipsize(TextUtils.TruncateAt.END);copy.addView(t,lp(0,-2,0,0,0,4));
        TextView d=label(desc,11);d.setTextColor(MUTED);d.setMaxLines(5);d.setEllipsize(TextUtils.TruncateAt.END);copy.addView(d,new LinearLayout.LayoutParams(-1,-2));
        row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));row.addView(state,lp(84,34,10,0,0,0));card.addView(row);
    }
    private TextView state(String s){TextView v=label(s,10);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setSingleLine(true);return v;}
    private void actionRow(LinearLayout card,String text,View.OnClickListener click){card.addView(button(text,click),lp(-1,48,0,12,0,0));}
    private void section(LinearLayout root,String text){TextView v=label(text,10);v.setTextColor(CYAN);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setLetterSpacing(.09f);root.addView(v,lp(-1,20,2,8,0,2));}
    private void setting(LinearLayout root,String text,Intent primary,Intent fallback){root.addView(button(text,v->safeOpen(primary,fallback)),lp(-1,48,0,0,0,8));}

    private void requestRole(){
        if(Build.VERSION.SDK_INT>=29){try{RoleManager rm=getSystemService(RoleManager.class);if(rm!=null&&rm.isRoleAvailable(RoleManager.ROLE_ASSISTANT)){if(!rm.isRoleHeld(RoleManager.ROLE_ASSISTANT)){startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT),41);}else refresh();return;}}catch(Throwable ignored){}}
        safeOpen(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),new Intent(Settings.ACTION_SETTINGS));
    }
    private void requestMic(){
        if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_MIC);
        else safeOpen(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:"+getPackageName())),new Intent(Settings.ACTION_SETTINGS));
    }
    private void toggleWake(){
        if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){getSharedPreferences("jarvis",0).edit().putBoolean("wake_pending",true).apply();requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_MIC);return;}
        boolean on=getSharedPreferences("jarvis",0).getBoolean("wake_enabled",false);getSharedPreferences("jarvis",0).edit().putBoolean("wake_enabled",!on).apply();Intent i=new Intent(this,JarvisWakeWordService.class);
        try{if(!on){if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}else stopService(i);}catch(Throwable e){getSharedPreferences("jarvis",0).edit().putBoolean("wake_enabled",false).apply();Toast.makeText(this,"Android не разрешил запустить фоновый микрофон. Проверьте разрешения и батарею.",Toast.LENGTH_LONG).show();}refresh();
    }
    private void openBattery(){safeOpen(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:"+getPackageName())),new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));}
    private void openTtsSettings(){safeOpen(new Intent("android.settings.TTS_SETTINGS"),new Intent(Settings.ACTION_SETTINGS));}
    private void safeOpen(Intent primary,Intent fallback){try{if(primary!=null&&primary.resolveActivity(getPackageManager())!=null){startActivity(primary);return;}}catch(Throwable ignored){}try{if(fallback!=null&&fallback.resolveActivity(getPackageManager())!=null){startActivity(fallback);return;}startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Throwable ignored){}}
    private void refresh(){
        if(roleState==null)return;boolean role=JarvisVoiceInteractionService.isActive(this);roleState.setText(role?"АКТИВЕН":"НЕ ВЫБРАН");roleState.setTextColor(role?GREEN:MUTED);
        boolean wake=getSharedPreferences("jarvis",0).getBoolean("wake_enabled",false);wakeState.setText(wake?"ВКЛЮЧЕН":"ВЫКЛЮЧЕН");wakeState.setTextColor(wake?GREEN:MUTED);
        boolean mic=Build.VERSION.SDK_INT<23||checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;micState.setText(mic?"РАЗРЕШЁН":"НЕТ");micState.setTextColor(mic?GREEN:RED);
        String g=voice==null?JarvisVoiceManager.AUTO:voice.getGender();voiceState.setText(JarvisVoiceManager.MALE.equals(g)?"МУЖСКОЙ":JarvisVoiceManager.FEMALE.equals(g)?"ЖЕНСКИЙ":"АВТО");voiceState.setTextColor(CYAN);
    }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_MIC&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED&&getSharedPreferences("jarvis",0).getBoolean("wake_pending",false)){getSharedPreferences("jarvis",0).edit().putBoolean("wake_pending",false).putBoolean("wake_enabled",true).apply();try{Intent i=new Intent(this,JarvisWakeWordService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}catch(Throwable ignored){}}refresh();}
    @Override protected void onResume(){super.onResume();immersive();refresh();}
    @Override protected void onDestroy(){if(voice!=null)voice.shutdown();super.onDestroy();}
}
