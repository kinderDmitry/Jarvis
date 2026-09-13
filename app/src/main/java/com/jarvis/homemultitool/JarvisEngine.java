package com.jarvis.homemultitool;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

/** Fast, dependency-free local command engine. Heavy AI is intentionally not put on the UI thread. */
public final class JarvisEngine {
    public interface Callback { void reply(String text); void state(String state); }
    private final Context context; private final Callback cb; private final SharedPreferences prefs;
    public JarvisEngine(Context c, Callback callback){ context=c.getApplicationContext(); cb=callback; prefs=c.getSharedPreferences("jarvis_local",Context.MODE_PRIVATE); }
    public void handle(final String raw){
        if(raw==null||raw.trim().isEmpty()) return;
        final String original=raw.trim(); final String c=original.toLowerCase(new Locale("ru")); cb.state("ОБРАБОТКА");
        // Fast path: no LLM/network for deterministic operations.
        if(c.matches(".*\\b(привет|здравствуй|доброе утро|добрый вечер|джарвис)\\b.*")){reply("На связи, сэр. Чем могу помочь?");return;}
        if(c.contains("который час")||c.equals("время")||c.contains("сколько времени")){reply("Сейчас "+new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date())+".");return;}
        if(c.contains("какая дата")||c.contains("какое сегодня число")||c.contains("сегодняшняя дата")){reply("Сегодня "+new SimpleDateFormat("d MMMM yyyy",new Locale("ru","RU")).format(new Date())+".");return;}
        if(c.contains("батаре")||c.contains("заряд")){android.os.BatteryManager bm=(android.os.BatteryManager)context.getSystemService(Context.BATTERY_SERVICE);int p=bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);reply("Заряд батареи: "+p+" процентов.");return;}
        if(c.contains("таймер")){int seconds=parseDuration(c); if(seconds>0){TimerStore.start(context,seconds,cb);return;} reply("Скажите длительность, например: таймер на 15 минут.");return;}
        if(c.contains("будильник")||c.contains("разбуди")){AlarmTool.schedule(context,c,cb);return;}
        if(c.contains("фонар")){toggleTorch();return;}
        if(c.contains("громк")||c.contains("звук")){adjustVolume(c);return;}
        if(c.contains("посчитай")||c.contains("калькулятор")||c.matches(".*\\d+(\\s*[+\\-*/]\\s*| умножить | поделить | плюс | минус )\\d+.*")){calculator(c);return;}
        if(c.startsWith("запиши")||c.startsWith("запомни")||c.startsWith("сохрани")){save(original.replaceFirst("(?i)^(запиши|запомни|сохрани)\\s*:??\\s*",""));return;}
        if(c.contains("что ты помнишь")||c.contains("что ты запомнил")||c.contains("мои заметки")){reply(prefs.getString("notes","" ).isEmpty()?"В локальной памяти пока ничего нет.":prefs.getString("notes",""));return;}
        if(c.contains("забудь всё")||c.contains("очисти память")){prefs.edit().remove("notes").apply();reply("Локальная память очищена.");return;}
        if(c.contains("открой камеру")||c.contains("запусти камеру")){open(new Intent("android.media.action.IMAGE_CAPTURE"),"Открываю камеру.");return;}
        if(c.contains("открой настройки")){open(new Intent(Settings.ACTION_SETTINGS),"Открываю настройки.");return;}
        if(c.contains("настройки wi-fi")||c.contains("настройки вайфай")){open(new Intent(Settings.ACTION_WIFI_SETTINGS),"Открываю настройки Wi-Fi.");return;}
        if(c.contains("настройки bluetooth")||c.contains("блютуз")){open(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS),"Открываю настройки Bluetooth.");return;}
        if(c.contains("настройки экрана")){open(new Intent(Settings.ACTION_DISPLAY_SETTINGS),"Открываю настройки экрана.");return;}
        if(c.contains("календар")){open(new Intent(Intent.ACTION_VIEW,Uri.parse("content://com.android.calendar/time/")),"Открываю календарь.");return;}
        if(c.contains("музык")){open(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC),"Открываю музыкальное приложение.");return;}
        if(c.contains("позвони")||c.contains("набери номер")){dial(original);return;}
        if(c.contains("смс")||c.contains("сообщение")){sms(original);return;}
        if(c.contains("выбери меня")||c.contains("системным ассистентом")||c.contains("экран блокировки")){reply("Для вызова JARVIS с экрана блокировки выберите JARVIS системным ассистентом Android в настройках голосового ввода.");return;}
        if(c.contains("кто ты")||c.contains("что ты умеешь")){reply("Я JARVIS. Работаю локально: голос, диалог, память, быстрые команды Android, таймеры, будильники, расчёты, камера, настройки, звонки и сообщения. Сетевые и облачные сервисы не используются.");return;}
        reply("Я услышал: «"+original+"». Эта команда пока не относится к моим локальным инструментам. Я не буду имитировать выполнение того, чего реально не умею.");
    }
    private void reply(String s){cb.reply(s);}
    private void save(String s){if(s.trim().isEmpty()){reply("Что именно сохранить?");return;}String old=prefs.getString("notes","");prefs.edit().putString("notes",old.isEmpty()?s:old+"\n• "+s).apply();reply("Сохранил в локальную память.");}
    private int parseDuration(String s){Matcher m=Pattern.compile("(\\d+)\\s*(секунд|секунды|сек|минут|мин|час|часа|ч)").matcher(s);if(!m.find())return 0;int n=Integer.parseInt(m.group(1));String u=m.group(2);if(u.startsWith("сек"))return n;if(u.startsWith("час")||u.equals("ч"))return n*3600;return n*60;}
    private void calculator(String c){String x=c.replace("умножить","*").replace("помножить","*").replace("поделить","/").replace("плюс","+").replace("минус","-").replace(',','.').replaceAll("[^0-9+*/.\\-]","");Matcher m=Pattern.compile("(-?\\d+(?:\\.\\d+)?)([+*/-])(-?\\d+(?:\\.\\d+)?)").matcher(x);if(!m.find()){reply("Скажите выражение, например: 125 умножить на 8.");return;}try{double a=Double.parseDouble(m.group(1)),b=Double.parseDouble(m.group(3));if("/".equals(m.group(2))&&b==0){reply("На ноль делить нельзя.");return;}double r="+".equals(m.group(2))?a+b:"-".equals(m.group(2))?a-b:"*".equals(m.group(2))?a*b:a/b;reply("Результат: "+(r==Math.rint(r)?Long.toString((long)r):String.format(Locale.US,"%.6f",r).replaceAll("0+$","" ).replaceAll("\\.$","")));}catch(Exception e){reply("Не удалось вычислить выражение.");}}
    private void toggleTorch(){if(Build.VERSION.SDK_INT<23){reply("Фонарик не поддерживается.");return;}try{CameraManager cm=(CameraManager)context.getSystemService(Context.CAMERA_SERVICE);String id=cm.getCameraIdList()[0];boolean on=prefs.getBoolean("torch",false);cm.setTorchMode(id,!on);prefs.edit().putBoolean("torch",!on).apply();reply(!on?"Фонарик включён.":"Фонарик выключен.");}catch(Exception e){reply("Не удалось управлять фонариком.");}}
    private void adjustVolume(String c){AudioManager am=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);if(c.contains("увелич")||c.contains("громче")){am.adjustVolume(AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI);reply("Громкость увеличена.");}else if(c.contains("умень")||c.contains("тише")){am.adjustVolume(AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI);reply("Громкость уменьшена.");}else reply("Скажите: громче или тише.");}
    private void dial(String raw){String d=raw.replaceAll("[^0-9+]","");if(d.length()<5){reply("Назовите номер телефона.");return;}open(new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+d)),"Открываю набор номера.");}
    private void sms(String raw){String d=raw.replaceAll("[^0-9+]","");Intent i=new Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:"+(d.length()>4?d:"")));i.putExtra("sms_body",raw);open(i,"Открываю сообщение.");}
    private void open(Intent i,String answer){try{ i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);context.startActivity(i);reply(answer);}catch(Exception e){reply("Не удалось открыть системное действие.");}}

    static final class TimerStore { static void start(Context c,int seconds,Callback cb){new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->cb.reply("Сэр, время вышло."),seconds*1000L);cb.reply("Принято. Таймер запущен на "+(seconds>=3600?(seconds/3600)+" ч":(seconds/60)+" мин")+".");} }
    static final class AlarmTool { static void schedule(Context c,String text,Callback cb){Matcher m=Pattern.compile("(?:на|в)\\s*(\\d{1,2})(?::(\\d{2}))?").matcher(text);if(!m.find()){cb.reply("Скажите время, например: будильник на 07:00.");return;}try{int hh=Integer.parseInt(m.group(1)),mm=m.group(2)==null?0:Integer.parseInt(m.group(2));if(hh>23||mm>59)throw new Exception();Calendar cal=Calendar.getInstance();cal.set(Calendar.HOUR_OF_DAY,hh);cal.set(Calendar.MINUTE,mm);cal.set(Calendar.SECOND,0);cal.set(Calendar.MILLISECOND,0);if(cal.before(Calendar.getInstance()))cal.add(Calendar.DAY_OF_YEAR,1);android.app.AlarmManager am=(android.app.AlarmManager)c.getSystemService(Context.ALARM_SERVICE);Intent i=new Intent(c,AlarmReceiver.class);android.app.PendingIntent pi=android.app.PendingIntent.getBroadcast(c,77,i,android.app.PendingIntent.FLAG_UPDATE_CURRENT|android.app.PendingIntent.FLAG_IMMUTABLE);if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);else am.setExact(android.app.AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);cb.reply(String.format(Locale.getDefault(),"Будильник установлен на %02d:%02d.",hh,mm));}catch(Exception e){cb.reply("Не удалось установить будильник.");}} }
}
