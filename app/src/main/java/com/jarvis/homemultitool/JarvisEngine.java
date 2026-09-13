package com.jarvis.homemultitool;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.*;
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

/** Local command router with real device actions and real web retrieval for live data. */
public final class JarvisEngine {
    public interface Callback { void reply(String text); void state(String state); }
    private final Context context; private final Callback cb; private final SharedPreferences prefs;
    private final WebSearchEngine web=new WebSearchEngine();
    private String lastCity="Москва";
    private String lastTopic="";

    public JarvisEngine(Context c,Callback callback){context=c.getApplicationContext();cb=callback;prefs=context.getSharedPreferences("jarvis_local",Context.MODE_PRIVATE);}

    public void handle(final String raw){
        if(raw==null||raw.trim().isEmpty())return;
        final String original=raw.trim();
        final String c=normalize(original);
        cb.state("ОБРАБОТКА");

        if(isWakeOnly(c)){reply("На связи, сэр. Слушаю вас.");return;}
        if(c.matches(".*\\b(привет|здравствуй|доброе утро|добрый вечер)\\b.*")){reply("На связи, сэр. Чем могу помочь?");return;}
        if(c.contains("который час")||c.equals("время")||c.contains("сколько времени")){reply("Сейчас "+new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date())+".");return;}
        if(c.contains("какая дата")||c.contains("какое сегодня число")||c.contains("сегодняшняя дата")){reply("Сегодня "+new SimpleDateFormat("d MMMM yyyy",new Locale("ru","RU")).format(new Date())+".");return;}
        if(c.contains("заряд")||c.contains("батаре")){battery();return;}
        if(c.contains("погода")||c.contains("температур")||c.contains("осадк")||c.contains("дождь")||c.contains("снег")){handleWeather(original,c);return;}
        if((c.contains("завтра")||c.contains("послезавтра"))&&lastTopic.equals("weather")){web.forecastWeather(lastCity,c.contains("послезавтра")?2:1,new WebSearchEngine.Callback(){public void result(String t,String s){reply(t);}public void state(String s){cb.state(s);}});return;}
        if(c.contains("таймер")){if(c.contains("отмен")||c.contains("сброс")){TimerTool.cancel(context,cb);}else{int sec=parseDuration(c);if(sec<=0)sec=300;TimerTool.start(context,sec,cb);}return;}
        if(c.contains("будильник")){AlarmTool.schedule(context,original,cb);return;}
        if(c.startsWith("посчитай")||c.startsWith("вычисли")||c.contains("сколько будет")||c.matches(".*\\d+\\s*(плюс|минус|умнож|подел|делить).*")){calculator(c);return;}
        if(c.contains("фонарик")||c.contains("вспышк")){toggleTorch();return;}
        if(c.contains("громче")||c.contains("увеличь громкость")||c.contains("тише")||c.contains("уменьши громкость")){adjustVolume(c);return;}
        if(c.contains("открой камеру")||c.contains("запусти камеру")){open(new Intent("android.media.action.IMAGE_CAPTURE"),"Открываю камеру.");return;}
        if(c.contains("настройки wi-fi")||c.contains("настройки вайфай")||c.equals("wifi")||c.equals("wi-fi")){open(new Intent(Settings.ACTION_WIFI_SETTINGS),"Открываю настройки Wi-Fi.");return;}
        if(c.contains("bluetooth")||c.contains("блютуз")){open(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS),"Открываю настройки Bluetooth.");return;}
        if(c.contains("настройки экрана")||c.contains("яркость экрана")){open(new Intent(Settings.ACTION_DISPLAY_SETTINGS),"Открываю настройки экрана.");return;}
        if(c.contains("открой настройки")||c.equals("настройки")){open(new Intent(Settings.ACTION_SETTINGS),"Открываю системные настройки.");return;}
        if(c.contains("разрешени")&&c.contains("приложени")){open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:"+context.getPackageName())),"Открываю разрешения JARVIS.");return;}
        if(c.contains("календар")){open(new Intent(Intent.ACTION_VIEW,Uri.parse("content://com.android.calendar/time/")),"Открываю календарь.");return;}
        if(c.contains("музык")||c.contains("яндекс музыку")||c.contains("яндекс музыка")){
            launchNamedApp(original, c, "музыку", "ru.yandex.music", "com.yandex.music", "Яндекс Музыка", "Открываю Яндекс Музыку.");
            return;
        }
        if(c.matches(".*\\b(ютуб|youtube)\\b.*")){launchNamedApp(original,c,"youtube","com.google.android.youtube","com.google.android.youtube.tv","YouTube","Открываю YouTube.");return;}
        if(c.matches(".*\\b(телеграм|telegram)\\b.*")){launchNamedApp(original,c,"telegram","org.telegram.messenger","Telegram","Открываю Telegram.");return;}
        if(c.matches(".*\\b(ватсап|whatsapp)\\b.*")){launchNamedApp(original,c,"whatsapp","com.whatsapp","WhatsApp","Открываю WhatsApp.");return;}
        if(c.matches(".*\\b(хром|chrome)\\b.*")){launchNamedApp(original,c,"chrome","com.android.chrome","Chrome","Открываю Chrome.");return;}
        if(c.matches(".*\\b(карты|карта)\\b.*")){launchNamedApp(original,c,"карты","com.google.android.apps.maps","Google Maps","Открываю карты.");return;}
        if(c.matches(".*\\b(калькулятор|калькулятор)\\b.*")){launchNamedApp(original,c,"калькулятор","com.google.android.calculator","Калькулятор","Открываю калькулятор.");return;}
        if(c.matches("^(открой|запусти|включи)\\s+.+")){
            if(launchInstalledApp(original,c)){return;}
        }
        if(c.contains("позвони")||c.contains("набери номер")){dial(original);return;}
        if(c.contains("смс")||c.contains("сообщение")){sms(original);return;}
        if(c.contains("новости сейчас")||c.equals("новости")||c.startsWith("новости ")){web.news(new WebSearchEngine.Callback(){public void result(String t,String src){reply(t);}public void state(String st){cb.state(st);}});return;}
        if(c.contains("кто ты")||c.contains("что ты умеешь")){reply("Я JARVIS — голосовой помощник. Я понимаю голосовые и текстовые команды, получаю актуальную погоду и веб-информацию, управляю доступными функциями телефона, ставлю системные таймеры и будильники и поддерживаю короткий контекст диалога.");return;}
        if(c.contains("что ты помнишь")||c.contains("мои заметки")){String n=prefs.getString("notes","");reply(n.isEmpty()?"В локальной памяти пока ничего нет.":n);return;}
        if(c.contains("забудь всё")||c.contains("очисти память")){prefs.edit().remove("notes").apply();reply("Локальная память очищена.");return;}
        if(c.matches("^(запиши|запомни|сохрани).*")){save(original.replaceFirst("(?iu)^(запиши|запомни|сохрани)\\s*:??\\s*",""));return;}

        web.search(original,new WebSearchEngine.Callback(){public void result(String t,String s){reply((s==null||s.isEmpty())?t:t+"\n\nИсточник: "+s);}public void state(String s){cb.state(s);}});
    }

    private boolean isWakeOnly(String c){return c.matches("^(джарвис|джарвису|джарвис а|джарвисом)$");}
    private String normalize(String s){return s.toLowerCase(new Locale("ru")).replace('ё','е').replaceAll("\\s+"," ").trim();}

    private void handleWeather(String original,String c){
        if((c.contains("завтра")||c.contains("послезавтра"))){int d=c.contains("послезавтра")?2:1;web.forecastWeather(extractCity(original),d,new WebSearchEngine.Callback(){public void result(String t,String s){reply(t);}public void state(String s){cb.state(s);}});return;}
        lastCity=extractCity(original);lastTopic="weather";
        web.currentWeather(lastCity,new WebSearchEngine.Callback(){public void result(String t,String s){reply(t);}public void state(String s){cb.state(s);}});
    }

    private String extractCity(String q){
        String x=q==null?"":q.trim();String l=normalize(x);
        if(l.contains("москв"))return "Москва";
        if(l.contains("санкт-петербург")||l.contains("петербург"))return "Санкт-Петербург";
        if(l.contains("нью-йорк")||l.contains("new york"))return "Нью-Йорк";
        Matcher m=Pattern.compile("(?iu)(?:в|для|города?|городе)\\s+([А-ЯЁA-Z][А-ЯЁа-яёA-Za-z-]{2,}(?:\\s+[А-ЯЁA-Z][А-ЯЁа-яёA-Za-z-]{2,})?)").matcher(x);
        if(m.find())return m.group(1).trim();
        return lastCity==null||lastCity.isEmpty()?"Москва":lastCity;
    }

    private void battery(){try{android.os.BatteryManager bm=(android.os.BatteryManager)context.getSystemService(Context.BATTERY_SERVICE);int p=bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);reply("Заряд батареи: "+p+" процентов.");}catch(Throwable e){reply("Не удалось получить уровень заряда.");}}
    private void reply(String s){cb.reply(s);}
    private void save(String s){if(s.trim().isEmpty()){reply("Что именно сохранить?");return;}String old=prefs.getString("notes","");prefs.edit().putString("notes",old.isEmpty()?"• "+s:old+"\n• "+s).apply();reply("Сохранил в локальную память.");}
    private int parseDuration(String s){Matcher m=Pattern.compile("(\\d+)\\s*(секунд|секунды|сек|минут|мин|час|часа|часов|ч)").matcher(s);if(!m.find())return 0;int n=Integer.parseInt(m.group(1));String u=m.group(2);if(u.startsWith("сек"))return n;if(u.startsWith("час")||u.equals("ч"))return n*3600;return n*60;}
    private void calculator(String c){
        String x=c.replace("умножить на","*").replace("умножить","*").replace("помножить","*").replace("поделить на","/").replace("поделить","/").replace("делить на","/").replace("плюс","+").replace("минус","-").replace(',','.').replaceAll("[^0-9+*/.\\-]","");
        Matcher m=Pattern.compile("(-?\\d+(?:\\.\\d+)?)([+*/-])(-?\\d+(?:\\.\\d+)?)").matcher(x);
        if(!m.find()){reply("Скажите выражение, например: 125 умножить на 8.");return;}
        try{double a=Double.parseDouble(m.group(1)),b=Double.parseDouble(m.group(3));if("/".equals(m.group(2))&&b==0){reply("На ноль делить нельзя.");return;}double r="+".equals(m.group(2))?a+b:"-".equals(m.group(2))?a-b:"*".equals(m.group(2))?a*b:a/b;reply("Результат: "+(r==Math.rint(r)?Long.toString((long)r):String.format(Locale.US,"%.6f",r).replaceAll("0+$","").replaceAll("\\.$","")));}catch(Exception e){reply("Не удалось вычислить выражение.");}}
    private void toggleTorch(){if(Build.VERSION.SDK_INT<23){reply("Фонарик не поддерживается.");return;}try{CameraManager cm=(CameraManager)context.getSystemService(Context.CAMERA_SERVICE);String id=cm.getCameraIdList()[0];boolean on=prefs.getBoolean("torch",false);cm.setTorchMode(id,!on);prefs.edit().putBoolean("torch",!on).apply();reply(!on?"Фонарик включён.":"Фонарик выключен.");}catch(Exception e){reply("Не удалось управлять фонариком.");}}
    private void adjustVolume(String c){AudioManager am=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);if(c.contains("увелич")||c.contains("громче")){am.adjustVolume(AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI);reply("Громкость увеличена.");}else if(c.contains("умень")||c.contains("тише")){am.adjustVolume(AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI);reply("Громкость уменьшена.");}else reply("Скажите: громче или тише.");}
    private void launchNamedApp(String original,String normalized,String hint,String... packagesAndAnswer){
        String answer=packagesAndAnswer[packagesAndAnswer.length-1];
        for(int i=0;i<packagesAndAnswer.length-1;i++){
            String pkg=packagesAndAnswer[i];
            if(pkg==null||pkg.trim().isEmpty())continue;
            try{Intent launch=context.getPackageManager().getLaunchIntentForPackage(pkg);if(launch!=null){launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);context.startActivity(launch);reply(answer);return;}}catch(Throwable ignored){}
        }
        if(launchInstalledApp(original,normalized))return;
        reply("Не нашёл приложение «"+hint+"» на этом телефоне.");
    }

    private boolean launchInstalledApp(String original,String normalized){
        try{
            PackageManager pm=context.getPackageManager();
            Intent probe=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<android.content.pm.ResolveInfo> apps=pm.queryIntentActivities(probe,PackageManager.MATCH_ALL);
            String q=normalized.replaceAll("(?iu)^(открой|запусти|включи|открывай|запускай)\\s+","").trim();
            if(q.isEmpty())return false;
            android.content.pm.ResolveInfo best=null; int score=0;
            for(android.content.pm.ResolveInfo ri:apps){
                if(ri.activityInfo==null||context.getPackageName().equals(ri.activityInfo.packageName))continue;
                String label=String.valueOf(ri.loadLabel(pm)).toLowerCase(new Locale("ru")).replace('ё','е');
                String pkg=ri.activityInfo.packageName.toLowerCase(Locale.ROOT);
                String qq=q.replace('ё','е'); int sc=0;
                if(label.equals(qq))sc=100; else if(label.contains(qq)||qq.contains(label))sc=70;
                String[] words=qq.split("\\s+"); for(String w:words)if(w.length()>2&&label.contains(w))sc+=20;
                if(pkg.contains(qq.replace(' ','.')))sc+=30;
                if(sc>score){score=sc;best=ri;}
            }
            if(best!=null&&score>=40){Intent launch=new Intent(Intent.ACTION_MAIN);launch.addCategory(Intent.CATEGORY_LAUNCHER);launch.setComponent(new ComponentName(best.activityInfo.packageName,best.activityInfo.name));launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);context.startActivity(launch);reply("Открываю "+best.loadLabel(pm)+".");return true;}
        }catch(Throwable ignored){}
        return false;
    }

    private void dial(String raw){String d=raw.replaceAll("[^0-9+]","");if(d.length()<5){reply("Назовите номер телефона.");return;}open(new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+d)),"Открываю набор номера.");}
    private void sms(String raw){String d=raw.replaceAll("[^0-9+]","");Intent i=new Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:"+(d.length()>4?d:"")));i.putExtra("sms_body",raw);open(i,"Открываю сообщение.");}
    private void open(Intent i,String answer){try{i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);context.startActivity(i);reply(answer);}catch(Exception e){reply("Не удалось открыть системное действие.");}}
    public void shutdown(){web.shutdown();}

    static final class TimerTool {
        private static final int TIMER_ID=78;
        static void start(Context c,int seconds,Callback cb){
            if(seconds<=0){cb.reply("Не удалось определить длительность таймера.");return;}
            try{AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);Intent i=new Intent(c,AlarmReceiver.class).setAction("JARVIS_TIMER");PendingIntent pi=PendingIntent.getBroadcast(c,TIMER_ID,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);long at=System.currentTimeMillis()+seconds*1000L;
                if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms()){c.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+c.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));cb.reply("Android требует разрешение «Будильники и напоминания». После выдачи разрешения повторите команду.");return;}
                if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);else am.setExact(AlarmManager.RTC_WAKEUP,at,pi);c.getSharedPreferences("jarvis_timer",Context.MODE_PRIVATE).edit().putLong("end",at).putInt("seconds",seconds).apply();cb.reply("Таймер установлен на "+format(seconds)+". Он сработает даже если JARVIS закрыт.");
            }catch(Throwable e){cb.reply("Не удалось установить таймер. Проверьте разрешение JARVIS на точные будильники.");}}
        static void cancel(Context c,Callback cb){try{AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);Intent i=new Intent(c,AlarmReceiver.class).setAction("JARVIS_TIMER");PendingIntent pi=PendingIntent.getBroadcast(c,TIMER_ID,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);am.cancel(pi);pi.cancel();c.getSharedPreferences("jarvis_timer",Context.MODE_PRIVATE).edit().clear().apply();cb.reply("Таймер отменён.");}catch(Throwable e){cb.reply("Не удалось отменить таймер.");}}
        static String format(int s){if(s>=3600)return(s/3600)+" ч";if(s%60==0)return(s/60)+" мин";return s+" сек";}
    }

    static final class AlarmTool {
        static void schedule(Context c,String text,Callback cb){Matcher m=Pattern.compile("(?:на|в)\\s*(\\d{1,2})(?::(\\d{2}))?").matcher(text);if(!m.find()){cb.reply("Скажите время, например: будильник на 07:00.");return;}try{int hh=Integer.parseInt(m.group(1)),mm=m.group(2)==null?0:Integer.parseInt(m.group(2));if(hh>23||mm>59)throw new Exception();Calendar cal=Calendar.getInstance();cal.set(Calendar.HOUR_OF_DAY,hh);cal.set(Calendar.MINUTE,mm);cal.set(Calendar.SECOND,0);cal.set(Calendar.MILLISECOND,0);if(cal.before(Calendar.getInstance()))cal.add(Calendar.DAY_OF_YEAR,1);AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);Intent i=new Intent(c,AlarmReceiver.class).setAction("JARVIS_ALARM");PendingIntent pi=PendingIntent.getBroadcast(c,77,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms()){c.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+c.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));cb.reply("Откройте разрешение на точные будильники, затем повторите команду.");return;}if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);else am.setExact(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);cb.reply(String.format(Locale.getDefault(),"Будильник установлен на %02d:%02d.",hh,mm));}catch(Exception e){cb.reply("Не удалось установить будильник.");}}
    }
}
