package com.jarvis.homemultitool;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.AlarmClock;
import android.provider.Settings;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

/** Fast, dependency-free local command engine. */
public final class JarvisEngine {
    public interface Callback { void reply(String text); void state(String state); }
    private final Context context; private final Callback cb; private final SharedPreferences prefs; private final WebSearchEngine web = new WebSearchEngine();
    public JarvisEngine(Context c, Callback callback){ context=c.getApplicationContext(); cb=callback; prefs=c.getSharedPreferences("jarvis_local",Context.MODE_PRIVATE); }

    public void handle(final String raw){
        if(raw==null||raw.trim().isEmpty()) return;
        final String original=raw.trim(); final String c=original.toLowerCase(new Locale("ru")); cb.state("ОБРАБОТКА");
        if(c.matches(".*\\b(привет|здравствуй|доброе утро|добрый вечер|джарвис)\\b.*")){reply("На связи, сэр. Чем могу помочь?");return;}
        if(c.contains("который час")||c.equals("время")||c.contains("сколько времени")){reply("Сейчас "+new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date())+".");return;}
        if(c.contains("сколько звезд") && (c.contains("на земле") || c.contains("во вселенной"))){reply("Точного числа звёзд на Земле нет: Земля сама по себе не содержит звёзд. Если вы имеете в виду наблюдаемое небо, невооружённым глазом при очень тёмном небе видно несколько тысяч звёзд одновременно. Для наблюдаемой Вселенной оценка порядка 10^22–10^24 звёзд.");return;}
        if(c.contains("сколько звезд") && c.contains("млечн")){reply("В Млечном Пути, по современным оценкам, примерно 100–400 миллиардов звёзд. Точного подсчёта нет.");return;}
        if(c.contains("сколько планет") && c.contains("солнечн")){reply("В Солнечной системе официально признаны 8 планет: от Меркурия до Нептуна.");return;}
        if(c.contains("скорость света")){reply("В вакууме скорость света составляет 299 792 458 метров в секунду.");return;}
        if(c.contains("температура солнца") || c.contains("сколько градусов на солнце")){reply("Температура фотосферы Солнца — примерно 5 500 градусов Цельсия. В ядре — около 15 миллионов градусов.");return;}
        if(c.contains("самая большая планета")){reply("Самая большая планета Солнечной системы — Юпитер.");return;}
        if(c.contains("самая маленькая планета")){reply("Самая маленькая планета Солнечной системы — Меркурий.");return;}
        if(c.contains("сколько океанов")){reply("Обычно выделяют пять океанов: Тихий, Атлантический, Индийский, Южный и Северный Ледовитый.");return;}
        if(c.contains("столица россии")){reply("Столица России — Москва.");return;}
        if(c.contains("сколько дней в году")){reply("В обычном году 365 дней, а в високосном — 366.");return;}
        if(c.contains("какая дата")||c.contains("какое сегодня число")||c.contains("сегодняшняя дата")){reply("Сегодня "+new SimpleDateFormat("d MMMM yyyy",new Locale("ru","RU")).format(new Date())+".");return;}
        if(c.contains("батаре")||c.contains("заряд")){android.os.BatteryManager bm=(android.os.BatteryManager)context.getSystemService(Context.BATTERY_SERVICE);int p=bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);reply("Заряд батареи: "+p+" процентов.");return;}
        if(c.contains("таймер")){
            if(c.contains("отмени")||c.contains("отключи")||c.contains("сбрось")){TimerTool.cancel(context,cb);return;}
            int seconds=parseDuration(c); if(seconds>0){TimerTool.start(context,seconds,cb);return;} reply("Назовите длительность: например, «Джарвис, поставь таймер на 15 минут».");return;
        }
        if(c.contains("будильник")||c.contains("разбуди")){AlarmTool.schedule(context,c,cb);return;}
        if(c.contains("фонар")){toggleTorch();return;}
        if(c.contains("громк")||c.contains("звук")){adjustVolume(c);return;}
        if(c.contains("посчитай")||c.contains("калькулятор")||c.matches(".*\\d+(\\s*[+\\-*/]\\s*| умножить | поделить | плюс | минус )\\d+.*")){calculator(c);return;}
        if(c.startsWith("запиши")||c.startsWith("запомни")||c.startsWith("сохрани")){save(original.replaceFirst("(?i)^(запиши|запомни|сохрани)\\s*:??\\s*",""));return;}
        if(c.contains("что ты помнишь")||c.contains("что ты запомнил")||c.contains("мои заметки")){reply(prefs.getString("notes","").isEmpty()?"В локальной памяти пока ничего нет.":prefs.getString("notes",""));return;}
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
        if(c.contains("системным ассистентом")||c.contains("экран блокировки")){reply("Откройте настройки JARVIS и выберите его системным ассистентом Android. После этого вызов ассистента с экрана блокировки будет передаваться JARVIS.");return;}
        if(c.contains("кто ты")||c.contains("что ты умеешь")){reply("Я JARVIS. Я могу разговаривать, выполнять команды телефона, ставить системные таймеры и получать актуальную информацию из интернета.");return;}
        if(c.contains("погод")||c.contains("температур")||c.contains("осадк")){
            web.currentWeather(extractCity(original), new WebSearchEngine.Callback(){ public void result(String text,String source){reply(text);} public void state(String state){cb.state(state);} }); return;
        }
        web.search(original, new WebSearchEngine.Callback(){ public void result(String text,String source){reply((source==null||source.isEmpty())?text:text+"\n\nИсточник: "+source);} public void state(String state){cb.state(state);} });
    }
    private String extractCity(String q){
        String x=q==null?"":q.trim();
        String l=x.toLowerCase(new Locale("ru"));
        if(l.contains("москв")) return "Москва";
        if(l.contains("санкт-петербург")||l.contains("петербург")) return "Санкт-Петербург";
        Matcher m=Pattern.compile("(?iu)(?:в|для|города?|городе)\\s+([А-ЯЁA-Z][А-ЯЁа-яёA-Za-z-]{2,}(?:\\s+[А-ЯЁA-Z][А-ЯЁа-яёA-Za-z-]{2,})?)").matcher(x);
        if(m.find()) return m.group(1).trim();
        return "Москва";
    }
    private void reply(String s){cb.reply(s);}
    private void save(String s){if(s.trim().isEmpty()){reply("Что именно сохранить?");return;}String old=prefs.getString("notes","");prefs.edit().putString("notes",old.isEmpty()?s:old+"\n• "+s).apply();reply("Сохранил в локальную память.");}
    private int parseDuration(String s){Matcher m=Pattern.compile("(\\d+)\\s*(секунд|секунды|сек|минут|мин|час|часа|ч)").matcher(s);if(!m.find())return 0;int n=Integer.parseInt(m.group(1));String u=m.group(2);if(u.startsWith("сек"))return n;if(u.startsWith("час")||u.equals("ч"))return n*3600;return n*60;}
    private void calculator(String c){String x=c.replace("умножить","*").replace("помножить","*").replace("поделить","/").replace("плюс","+").replace("минус","-").replace(',','.').replaceAll("[^0-9+*/.\\-]","");Matcher m=Pattern.compile("(-?\\d+(?:\\.\\d+)?)([+*/-])(-?\\d+(?:\\.\\d+)?)").matcher(x);if(!m.find()){reply("Скажите выражение, например: 125 умножить на 8.");return;}try{double a=Double.parseDouble(m.group(1)),b=Double.parseDouble(m.group(3));if("/".equals(m.group(2))&&b==0){reply("На ноль делить нельзя.");return;}double r="+".equals(m.group(2))?a+b:"-".equals(m.group(2))?a-b:"*".equals(m.group(2))?a*b:a/b;reply("Результат: "+(r==Math.rint(r)?Long.toString((long)r):String.format(Locale.US,"%.6f",r).replaceAll("0+$","").replaceAll("\\.$","")));}catch(Exception e){reply("Не удалось вычислить выражение.");}}
    private void toggleTorch(){if(Build.VERSION.SDK_INT<23){reply("Фонарик не поддерживается.");return;}try{CameraManager cm=(CameraManager)context.getSystemService(Context.CAMERA_SERVICE);String id=cm.getCameraIdList()[0];boolean on=prefs.getBoolean("torch",false);cm.setTorchMode(id,!on);prefs.edit().putBoolean("torch",!on).apply();reply(!on?"Фонарик включён.":"Фонарик выключен.");}catch(Exception e){reply("Не удалось управлять фонариком.");}}
    private void adjustVolume(String c){AudioManager am=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);if(c.contains("увелич")||c.contains("громче")){am.adjustVolume(AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI);reply("Громкость увеличена.");}else if(c.contains("умень")||c.contains("тише")){am.adjustVolume(AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI);reply("Громкость уменьшена.");}else reply("Скажите: громче или тише.");}
    private void dial(String raw){String d=raw.replaceAll("[^0-9+]","");if(d.length()<5){reply("Назовите номер телефона.");return;}open(new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+d)),"Открываю набор номера.");}
    private void sms(String raw){String d=raw.replaceAll("[^0-9+]","");Intent i=new Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:"+(d.length()>4?d:"")));i.putExtra("sms_body",raw);open(i,"Открываю сообщение.");}
    private void open(Intent i,String answer){try{i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);context.startActivity(i);reply(answer);}catch(Exception e){reply("Не удалось открыть системное действие.");}}
    public void shutdown(){web.shutdown();}

    static final class TimerTool {
        private static final int TIMER_ID=78;
        static void start(Context c,int seconds,Callback cb){
            if(seconds<=0){cb.reply("Не удалось определить длительность таймера.");return;}
            try {
                AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
                Intent i=new Intent(c,AlarmReceiver.class).setAction("JARVIS_TIMER").putExtra("duration",seconds);
                PendingIntent pi=PendingIntent.getBroadcast(c,TIMER_ID,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                long at=System.currentTimeMillis()+seconds*1000L;
                if(Build.VERSION.SDK_INT>=31 && !am.canScheduleExactAlarms()){
                    c.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+c.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    cb.reply("Android требует разрешение «Будильники и напоминания». После выдачи разрешения повторите команду — таймер будет установлен.");
                    return;
                }
                if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);else am.setExact(AlarmManager.RTC_WAKEUP,at,pi);
                c.getSharedPreferences("jarvis_timer",Context.MODE_PRIVATE).edit().putLong("end",at).putInt("seconds",seconds).apply();
                cb.reply("Таймер действительно установлен на "+format(seconds)+". Он сработает даже если JARVIS закрыт.");
            } catch(Throwable e){cb.reply("Не удалось установить таймер. Проверьте разрешение JARVIS на «Будильники и напоминания».");}
        }
        static void cancel(Context c,Callback cb){
            try{AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);Intent i=new Intent(c,AlarmReceiver.class).setAction("JARVIS_TIMER");PendingIntent pi=PendingIntent.getBroadcast(c,TIMER_ID,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);am.cancel(pi);pi.cancel();c.getSharedPreferences("jarvis_timer",Context.MODE_PRIVATE).edit().clear().apply();cb.reply("Таймер отменён.");}catch(Throwable e){cb.reply("Не удалось отменить таймер.");}
        }
        static String format(int s){if(s>=3600)return(s/3600)+" ч";if(s%60==0)return(s/60)+" мин";return s+" сек";}
    }
    static final class AlarmTool { static void schedule(Context c,String text,Callback cb){Matcher m=Pattern.compile("(?:на|в)\\s*(\\d{1,2})(?::(\\d{2}))?").matcher(text);if(!m.find()){cb.reply("Скажите время, например: будильник на 07:00.");return;}try{int hh=Integer.parseInt(m.group(1)),mm=m.group(2)==null?0:Integer.parseInt(m.group(2));if(hh>23||mm>59)throw new Exception();Calendar cal=Calendar.getInstance();cal.set(Calendar.HOUR_OF_DAY,hh);cal.set(Calendar.MINUTE,mm);cal.set(Calendar.SECOND,0);cal.set(Calendar.MILLISECOND,0);if(cal.before(Calendar.getInstance()))cal.add(Calendar.DAY_OF_YEAR,1);AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);Intent i=new Intent(c,AlarmReceiver.class).setAction("JARVIS_ALARM");PendingIntent pi=PendingIntent.getBroadcast(c,77,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);if(Build.VERSION.SDK_INT>=31 && !am.canScheduleExactAlarms()) { c.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+c.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); cb.reply("Откройте разрешение на точные будильники, затем повторите команду."); return; } if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);else am.setExact(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);cb.reply(String.format(Locale.getDefault(),"Будильник установлен на %02d:%02d.",hh,mm));}catch(Exception e){cb.reply("Не удалось установить будильник.");}} }
}
