package com.jarvis.homemultitool;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

/** Stable premium settings screen. Every action has a concrete Android target and a safe fallback. */
public class SettingsActivity extends Activity {
    private static final int BG=Color.rgb(2,6,10), SUR=Color.rgb(5,15,25), CYAN=Color.rgb(83,220,255), BORDER=Color.rgb(25,102,153), WHITE=Color.rgb(242,247,255), MUTED=Color.rgb(121,153,180), GREEN=Color.rgb(71,232,177), RED=Color.rgb(255,102,116), REQ=51;
    private TextView roleState,wakeState,micState,voiceState;
    private JarvisVoiceManager voice;
    private int dp(float v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
    private TextView txt(String s,float sp){TextView v=new TextView(this);v.setText(s);v.setTextColor(WHITE);float fs=Math.max(9f,Math.min(sp,sp/Math.max(1f,getResources().getConfiguration().fontScale)));v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,fs);v.setIncludeFontPadding(false);v.setGravity(Gravity.CENTER_VERTICAL);v.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);return v;}
    private GradientDrawable bg(int stroke,int fill,float r){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(r));if(stroke>0)g.setStroke(dp(1),stroke);return g;}
    private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    private TextView button(String s,View.OnClickListener c){TextView v=txt(s,14);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setSingleLine(true);v.setEllipsize(android.text.TextUtils.TruncateAt.END);v.setPadding(dp(12),0,dp(12),0);v.setBackground(bg(BORDER,0xE4071420,18));v.setOnClickListener(c);if(Build.VERSION.SDK_INT>=26)v.setAutoSizeTextTypeUniformWithConfiguration(9,15,1,android.util.TypedValue.COMPLEX_UNIT_SP);v.setMinHeight(dp(52));return v;}
    private TextView iconButton(String s,View.OnClickListener c){TextView v=button(s,c);v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,24);v.setTextColor(CYAN);v.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE);return v;}
    private void immersive(){try{if(Build.VERSION.SDK_INT>=30){getWindow().setDecorFitsSystemWindows(false);WindowInsetsController c=getWindow().getInsetsController();if(c!=null){c.hide(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}}else getWindow().getDecorView().setSystemUiVisibility(5894|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}catch(Throwable ignored){}}

    @Override protected void onCreate(Bundle b){super.onCreate(b);immersive();getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);voice=new JarvisVoiceManager(this);voice.init(this::refresh);build();}
    private void build(){
        ScrollView sc=new ScrollView(this);sc.setBackgroundColor(BG);sc.setFillViewport(true);sc.setClipToPadding(false);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(10),dp(18),dp(36));
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(iconButton("‹",v->finish()),new LinearLayout.LayoutParams(dp(52),dp(52)));
        TextView title=txt("J A R V I S",23);title.setGravity(Gravity.CENTER);title.setTextColor(CYAN);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setLetterSpacing(.17f);head.addView(title,new LinearLayout.LayoutParams(0,dp(52),1));
        head.addView(iconButton("⚙",v->safeOpen(new Intent(Settings.ACTION_SETTINGS),null)),new LinearLayout.LayoutParams(dp(52),dp(52)));root.addView(head);
        TextView sub=txt("СИСТЕМА  •  ГОЛОС  •  ВЫЗОВ  •  ПРИВАТНОСТЬ",10);sub.setTextColor(MUTED);sub.setTypeface(Typeface.DEFAULT,Typeface.BOLD);sub.setLetterSpacing(.07f);root.addView(sub,lp(-1,24,2,6,0,8));

        section(root,"СИСТЕМНЫЙ АССИСТЕНТ");
        LinearLayout role=card();roleState=newState();row(role,"JARVIS как помощник Android","Назначает JARVIS системным голосовым помощником Android.",roleState);action(role,"ОТКРЫТЬ НАСТРОЙКУ",v->requestRole());root.addView(role,lp(-1,-2,0,0,0,10));
        section(root,"ВЫЗОВ «ДЖАРВИС»");
        LinearLayout wake=card();wakeState=newState();row(wake,"Фоновое ожидание фразы","JARVIS слушает короткими сегментами и ищет обращение «Джарвис». Для работы поверх других приложений служба должна быть запущена.",wakeState);action(wake,"ВКЛЮЧИТЬ / ВЫКЛЮЧИТЬ",v->toggleWake());action(wake,"РАЗРЕШИТЬ РАБОТУ БЕЗ ОГРАНИЧЕНИЙ БАТАРЕИ",v->openBatterySettings());root.addView(wake,lp(-1,-2,0,0,0,10));
        section(root,"МИКРОФОН");
        LinearLayout mic=card();micState=newState();row(mic,"Разрешение микрофона","Доступ к микрофону для распознавания голосовых команд.",micState);action(mic,"ОТКРЫТЬ РАЗРЕШЕНИЯ",v->requestMic());root.addView(mic,lp(-1,-2,0,0,0,10));
        section(root,"ГОЛОС JARVIS");
        LinearLayout vc=card();voiceState=newState();row(vc,"Профиль ответа","Используется реальный голос установленного Android TTS. Профиль влияет на тембр и темп насколько это позволяет движок.",voiceState);
        LinearLayout voices=new LinearLayout(this);voices.setGravity(Gravity.CENTER_VERTICAL);voices.addView(button("МУЖСКОЙ",v->{voice.setGender(JarvisVoiceManager.MALE);voice.speak("Профиль мужского голоса активирован.",null);refresh();}),new LinearLayout.LayoutParams(0,dp(52),1));voices.addView(button("ЖЕНСКИЙ",v->{voice.setGender(JarvisVoiceManager.FEMALE);voice.speak("Профиль женского голоса активирован.",null);refresh();}),lp(0,52,8,0,0,0));vc.addView(voices,lp(-1,52,0,10,0,0));action(vc,"АВТОМАТИЧЕСКИЙ ПРОФИЛЬ",v->{voice.setGender(JarvisVoiceManager.AUTO);refresh();});action(vc,"ОТКРЫТЬ НАСТРОЙКИ СИНТЕЗА РЕЧИ",v->openTtsSettings());root.addView(vc,lp(-1,-2,0,0,0,10));
        section(root,"СИСТЕМНЫЕ РАЗДЕЛЫ");
        addSetting(root,"ГОЛОСОВОЙ ВВОД",new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),new Intent(Settings.ACTION_SETTINGS));
        addSetting(root,"УВЕДОМЛЕНИЯ JARVIS",new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()).setData(Uri.parse("package:"+getPackageName())),new Intent(Settings.ACTION_SETTINGS));
        addSetting(root,"РАЗРЕШЕНИЯ JARVIS",new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:"+getPackageName())),new Intent(Settings.ACTION_APPLICATION_SETTINGS));
        if(Build.VERSION.SDK_INT>=31)addSetting(root,"ТОЧНЫЕ БУДИЛЬНИКИ",new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())),new Intent(Settings.ACTION_SETTINGS));
        addSetting(root,"ОПТИМИЗАЦИЯ БАТАРЕИ",new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:"+getPackageName())),new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        addSetting(root,"НАСТРОЙКИ ЭКРАНА",new Intent(Settings.ACTION_DISPLAY_SETTINGS),new Intent(Settings.ACTION_SETTINGS));
        addSetting(root,"BLUETOOTH",new Intent(Settings.ACTION_BLUETOOTH_SETTINGS),new Intent(Settings.ACTION_SETTINGS));
        addSetting(root,"WI-FI",new Intent(Settings.ACTION_WIFI_SETTINGS),new Intent(Settings.ACTION_SETTINGS));
        TextView foot=txt("Настройки JARVIS не имитируют системные переключатели: действие либо меняет функцию помощника, либо открывает соответствующий экран Android.",11);foot.setTextColor(MUTED);foot.setLineSpacing(0,1.15f);root.addView(foot,lp(-1,-2,0,14,0,0));
        sc.addView(root);setContentView(sc);refresh();
    }
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(15),dp(14),dp(15),dp(14));c.setBackground(bg(BORDER,0xE8071420,22));return c;}
    private void row(LinearLayout c,String title,String desc,TextView state){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);TextView t=txt(title,15);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setMaxLines(2);t.setEllipsize(android.text.TextUtils.TruncateAt.END);tx.addView(t,new LinearLayout.LayoutParams(0,dp(34),1));TextView d=txt(desc,11);d.setTextColor(MUTED);d.setMaxLines(4);d.setEllipsize(android.text.TextUtils.TruncateAt.END);tx.addView(d,new LinearLayout.LayoutParams(0,-2,1));r.addView(tx,new LinearLayout.LayoutParams(0,-2,1));r.addView(state,lp(86,38,10,0,0,0));c.addView(r);}
    private void action(LinearLayout c,String label,View.OnClickListener l){c.addView(button(label,l),lp(-1,52,0,10,0,0));}
    private TextView newState(){TextView v=txt("—",10);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setSingleLine(true);return v;}
    private void section(LinearLayout root,String s){TextView v=txt(s,10);v.setTextColor(CYAN);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setLetterSpacing(.1f);root.addView(v,lp(-1,22,2,6,0,4));}
    private void addSetting(LinearLayout root,String label,Intent primary,Intent fallback){root.addView(button(label,v->safeOpen(primary,fallback)),lp(-1,52,0,0,0,7));}
    private void requestRole(){if(Build.VERSION.SDK_INT>=29){try{RoleManager rm=getSystemService(RoleManager.class);if(rm!=null&&rm.isRoleAvailable(RoleManager.ROLE_ASSISTANT)){if(!rm.isRoleHeld(RoleManager.ROLE_ASSISTANT))startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT),41);else refresh();return;}}catch(Throwable ignored){}}safeOpen(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),new Intent(Settings.ACTION_SETTINGS));}
    private void requestMic(){if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ);else safeOpen(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:"+getPackageName())),new Intent(Settings.ACTION_SETTINGS));}
    private void toggleWake(){if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){getSharedPreferences("jarvis",0).edit().putBoolean("wake_pending",true).apply();requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ);return;}boolean on=getSharedPreferences("jarvis",0).getBoolean("wake_enabled",false);getSharedPreferences("jarvis",0).edit().putBoolean("wake_enabled",!on).apply();Intent i=new Intent(this,JarvisWakeWordService.class);try{if(!on){if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}else stopService(i);}catch(Throwable e){getSharedPreferences("jarvis",0).edit().putBoolean("wake_enabled",false).apply();}refresh();}
    private void openBatterySettings(){safeOpen(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:"+getPackageName())),new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));}
    private void openTtsSettings(){safeOpen(new Intent("com.android.settings.TTS_SETTINGS"),new Intent(Settings.ACTION_SETTINGS));}
    private void safeOpen(Intent primary,Intent fallback){try{if(primary!=null&&primary.resolveActivity(getPackageManager())!=null){startActivity(primary);return;}}catch(Throwable ignored){}try{if(fallback!=null&&fallback.resolveActivity(getPackageManager())!=null)startActivity(fallback);else startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Throwable ignored){}}
    private void refresh(){if(roleState==null)return;boolean role=JarvisVoiceInteractionService.isActive(this);roleState.setText(role?"АКТИВЕН":"НЕ ВЫБРАН");roleState.setTextColor(role?GREEN:MUTED);boolean wake=getSharedPreferences("jarvis",0).getBoolean("wake_enabled",false);wakeState.setText(wake?"ВКЛЮЧЕН":"ВЫКЛЮЧЕН");wakeState.setTextColor(wake?GREEN:MUTED);boolean mic=Build.VERSION.SDK_INT<23||checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;micState.setText(mic?"РАЗРЕШЁН":"НЕТ");micState.setTextColor(mic?GREEN:RED);String g=voice==null?JarvisVoiceManager.AUTO:voice.getGender();voiceState.setText(JarvisVoiceManager.MALE.equals(g)?"МУЖСКОЙ":JarvisVoiceManager.FEMALE.equals(g)?"ЖЕНСКИЙ":"АВТО");voiceState.setTextColor(CYAN);}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED&&getSharedPreferences("jarvis",0).getBoolean("wake_pending",false)){getSharedPreferences("jarvis",0).edit().putBoolean("wake_pending",false).putBoolean("wake_enabled",true).apply();try{Intent i=new Intent(this,JarvisWakeWordService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}catch(Throwable ignored){}}refresh();}
    @Override protected void onResume(){super.onResume();immersive();refresh();}
    @Override protected void onDestroy(){if(voice!=null)voice.shutdown();super.onDestroy();}
}
