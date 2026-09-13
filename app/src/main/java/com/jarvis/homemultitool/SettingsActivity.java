package com.jarvis.homemultitool;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    private static final int BG=Color.rgb(1,5,11), CYAN=Color.rgb(92,224,255), WHITE=Color.WHITE, MUTED=Color.rgb(125,160,190), GREEN=Color.rgb(65,240,178);
    private TextView roleState, wakeState;
    private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(l,t,r,b);return p;}
    private TextView text(String s,float z){TextView v=new TextView(this);v.setText(s);v.setTextColor(WHITE);v.setTextSize(z);return v;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(WHITE);b.setTextSize(13);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackgroundColor(Color.rgb(5,20,35));return b;}
    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);build();refresh();}
    private void build(){
        ScrollView sv=new ScrollView(this);sv.setBackgroundColor(BG);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(18,12,18,24);
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=text("‹",34);back.setTextColor(CYAN);back.setGravity(Gravity.CENTER);back.setOnClickListener(v->finish());head.addView(back,lp(52,54,0,0,8,0));
        TextView title=text("НАСТРОЙКИ JARVIS",20);title.setTextColor(CYAN);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);head.addView(title,lp(0,54,0,0,0,0));((LinearLayout.LayoutParams)title.getLayoutParams()).weight=1;root.addView(head);
        TextView sub=text("Здесь находятся системные и голосовые настройки.",12);sub.setTextColor(MUTED);root.addView(sub,lp(-1,30,60,0,0,10));
        section(root,"СИСТЕМНЫЙ ПОМОЩНИК");
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(14,10,14,12);card.setBackgroundColor(Color.rgb(4,14,25));
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView t=text("JARVIS как помощник Android",15);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);row.addView(t,lp(0,48,0,0,0,0));((LinearLayout.LayoutParams)t.getLayoutParams()).weight=1;roleState=text("ПРОВЕРКА",11);roleState.setTextColor(MUTED);row.addView(roleState);card.addView(row);
        Button role=button("СДЕЛАТЬ JARVIS СИСТЕМНЫМ АССИСТЕНТОМ");role.setOnClickListener(v->requestRole());card.addView(role,lp(-1,50,0,6,0,0));root.addView(card,lp(-1,-2,0,0,0,12));
        section(root,"ГОЛОСОВОЙ ВЫЗОВ");
        LinearLayout wc=new LinearLayout(this);wc.setOrientation(LinearLayout.VERTICAL);wc.setPadding(14,10,14,12);wc.setBackgroundColor(Color.rgb(4,14,25));
        LinearLayout wr=new LinearLayout(this);wr.setGravity(Gravity.CENTER_VERTICAL);TextView wt=text("Фоновый вызов «Джарвис»",15);wt.setTypeface(Typeface.DEFAULT,Typeface.BOLD);wr.addView(wt,lp(0,48,0,0,0,0));((LinearLayout.LayoutParams)wt.getLayoutParams()).weight=1;wakeState=text("ВЫКЛЮЧЕН",11);wakeState.setTextColor(MUTED);wr.addView(wakeState);wc.addView(wr);
        Button wake=button("ВКЛЮЧИТЬ / ВЫКЛЮЧИТЬ");wake.setOnClickListener(v->toggleWake());wc.addView(wake,lp(-1,50,0,6,0,0));root.addView(wc,lp(-1,-2,0,0,0,12));
        section(root,"РЕЧЬ");
        Button voice=button("НАСТРОЙКИ СИНТЕЗА РЕЧИ ANDROID");voice.setOnClickListener(v->{try{startActivity(new Intent("com.android.settings.TTS_SETTINGS"));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}});root.addView(voice,lp(-1,52,0,0,0,8));
        Button assist=button("НАСТРОЙКИ ГОЛОСОВОГО ВВОДА ANDROID");assist.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}});root.addView(assist,lp(-1,52,0,0,0,12));
        section(root,"ПРИВАТНОСТЬ");TextView privacy=text("JARVIS использует интернет для актуальных ответов и поиска. Команды телефона выполняются через Android. Вы можете в любой момент отключить разрешения приложения в системных настройках.",12);privacy.setTextColor(MUTED);privacy.setPadding(2,4,2,12);root.addView(privacy);
        Button app=button("РАЗРЕШЕНИЯ ПРИЛОЖЕНИЯ");app.setOnClickListener(v->{try{Intent i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);i.setData(android.net.Uri.parse("package:"+getPackageName()));startActivity(i);}catch(Exception ignored){}});root.addView(app,lp(-1,52,0,0,0,0));sv.addView(root);setContentView(sv);
    }
    private void section(LinearLayout r,String s){TextView v=text(s,11);v.setTextColor(CYAN);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setLetterSpacing(.12f);r.addView(v,lp(-1,28,2,8,0,3));}
    private void requestRole(){if(Build.VERSION.SDK_INT>=29){try{RoleManager rm=getSystemService(RoleManager.class);if(rm!=null&&rm.isRoleAvailable(RoleManager.ROLE_ASSISTANT)&&!rm.isRoleHeld(RoleManager.ROLE_ASSISTANT)){startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT),41);return;} }catch(Throwable ignored){}}try{startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}
    private void refresh(){boolean active=JarvisVoiceInteractionService.isActive(this);roleState.setText(active?"АКТИВЕН":"НЕ ВЫБРАН");roleState.setTextColor(active?GREEN:MUTED);boolean on=getSharedPreferences("jarvis",0).getBoolean("wake_enabled",false);wakeState.setText(on?"ВКЛЮЧЕН":"ВЫКЛЮЧЕН");wakeState.setTextColor(on?GREEN:MUTED);}
    private void toggleWake(){boolean on=getSharedPreferences("jarvis",0).getBoolean("wake_enabled",false);getSharedPreferences("jarvis",0).edit().putBoolean("wake_enabled",!on).apply();Intent i=new Intent(this,JarvisWakeWordService.class);try{if(!on){if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}else stopService(i);}catch(Exception ignored){}refresh();}
    @Override protected void onResume(){super.onResume();if(roleState!=null)refresh();}
}
