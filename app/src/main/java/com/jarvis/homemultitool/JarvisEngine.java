package com.jarvis.homemultitool;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

/** Deterministic local command brain with real device actions and live web/weather retrieval. */
public final class JarvisEngine {
    public interface Callback { void reply(String text); void state(String state); }
    private final Context context; private final Callback cb; private final SharedPreferences prefs; private final WebSearchEngine web=new WebSearchEngine();
    private String lastCity="Москва", lastTopic="", lastUserMessage="";
    private String lastAssistantQuestion="";
    private String pendingMusicTrack="";
    private String pendingMusicApp="";
    public JarvisEngine(Context c,Callback callback){context=c.getApplicationContext();cb=callback;prefs=context.getSharedPreferences("jarvis_local",Context.MODE_PRIVATE);}

    public void handle(final String raw){
        try { handleInternal(raw); } catch (Throwable fatal) { cb.state("ГОТОВ"); cb.reply("Я не смог безопасно выполнить эту команду. Попробуйте сказать её иначе."); }
    }

    private void handleInternal(final String raw){
        if(raw==null||raw.trim().isEmpty())return;
        final String original=raw.trim(), c=normalize(original); lastUserMessage=original; cb.state("ОБРАБОТКА");
        if(isWakeOnly(c)){reply("Я на связи, сэр. Слушаю вас.");return;}
        if(handleConversation(c))return;
        if(isGreeting(c)&&!(c.contains("как дела")||c.contains("как ты")||c.contains("как поживаешь"))){reply(greeting(c));return;}
        if(c.contains("как дела")||c.contains("как ты")||c.contains("как поживаешь")){lastAssistantQuestion="how_user";reply("Отлично, сэр. Работаю стабильно и готов помочь. А у вас как дела?");return;}
        if(c.contains("спасибо")||c.contains("благодарю")){reply("Всегда пожалуйста, сэр.");return;}
        if(c.contains("доброй ночи")){reply("Доброй ночи, сэр. Я буду готов, когда вы вернётесь.");return;}
        if(c.contains("который час")||c.equals("время")||c.contains("сколько времени")){reply("Сейчас "+new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date())+".");return;}
        if(c.contains("какая дата")||c.contains("какое сегодня число")||c.contains("сегодняшняя дата")){reply("Сегодня "+new SimpleDateFormat("d MMMM yyyy",new Locale("ru","RU")).format(new Date())+".");return;}
        if(c.contains("заряд")||c.contains("батаре")){battery();return;}
        if(c.contains("погода")||c.contains("температур")||c.contains("осадк")||c.contains("дождь")||c.contains("снег")){handleWeather(original,c);return;}
        if((c.equals("а завтра")||c.equals("завтра")||c.equals("а послезавтра")||c.equals("послезавтра"))&&lastTopic.equals("weather")){int d=c.contains("послезавтра")?2:1;web.forecastWeather(lastCity,d,webCallback());return;}
        if(c.contains("таймер")){if(c.contains("отмен")||c.contains("сброс")){JarvisEngine.TimerTool.cancel(context,cb);}else{int sec=parseDuration(c);if(sec<=0)sec=300;JarvisEngine.TimerTool.start(context,sec,cb);}return;}
        if(c.contains("будильник")){AlarmTool.schedule(context,original,cb);return;}
        if(isMath(c)){calculator(c);return;}
        if(c.contains("фонарик")||c.contains("вспышк")){toggleTorch();return;}
        if(c.contains("громче")||c.contains("увеличь громкость")||c.contains("тише")||c.contains("уменьши громкость")){adjustVolume(c);return;}
        if(c.contains("открой камеру")||c.contains("запусти камеру")){open(new Intent("android.media.action.IMAGE_CAPTURE"),"Открываю камеру.");return;}
        if(c.contains("настройки wi-fi")||c.contains("настройки вайфай")||c.equals("wifi")||c.equals("wi-fi")){open(new Intent(Settings.ACTION_WIFI_SETTINGS),"Открываю настройки Wi-Fi.");return;}
        if(c.contains("bluetooth")||c.contains("блютуз")){open(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS),"Открываю настройки Bluetooth.");return;}
        if(c.contains("настройки экрана")||c.contains("яркость экрана")){open(new Intent(Settings.ACTION_DISPLAY_SETTINGS),"Открываю настройки экрана.");return;}
        if(c.contains("разрешени")&&c.contains("приложени")){open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:"+context.getPackageName())),"Открываю настройки разрешений JARVIS.");return;}
        if(c.contains("открой настройки")||c.equals("настройки")){open(new Intent(Settings.ACTION_SETTINGS),"Открываю системные настройки.");return;}
        if(c.contains("календар")){open(new Intent(Intent.ACTION_VIEW,Uri.parse("content://com.android.calendar/time/")),"Открываю календарь.");return;}

        if(c.matches(".*(яндекс\\s*музык|я\\s*музык|вк\\s*музык|vk\\s*музык|музыку|музыка).*")){handleMusic(original,c);return;}
        if(c.matches(".*\\b(ютуб|youtube)\\b.*")){launchNamedApp(original,c,"YouTube","com.google.android.youtube","com.google.android.youtube.tv","Открываю YouTube.");return;}
        if(c.matches(".*\\b(телеграм|telegram)\\b.*")){launchNamedApp(original,c,"Telegram","org.telegram.messenger","Открываю Telegram.");return;}
        if(c.matches(".*\\b(ватсап|whatsapp)\\b.*")){launchNamedApp(original,c,"WhatsApp","com.whatsapp","Открываю WhatsApp.");return;}
        if(c.matches(".*\\b(хром|chrome)\\b.*")){launchNamedApp(original,c,"Chrome","com.android.chrome","Открываю Chrome.");return;}
        if(c.matches(".*\\b(карты|карта)\\b.*")){launchNamedApp(original,c,"Карты","com.google.android.apps.maps","Открываю карты.");return;}
        if(c.matches(".*\\b(калькулятор)\\b.*")){launchNamedApp(original,c,"Калькулятор","com.google.android.calculator","Открываю калькулятор.");return;}
        if(c.matches(".*(яндекс|яндекс браузер|браузер).*")&&c.contains("браузер")){launchNamedApp(original,c,"Яндекс Браузер","com.yandex.browser","ru.yandex.searchplugin","Открываю Яндекс Браузер.");return;}
        if(c.matches("^(открой|запусти|включи|открывай|запускай|зайди в)\\s+.+")||c.contains("открой приложение ")){if(launchInstalledApp(original,c))return;reply("Не нашёл такое приложение на этом телефоне.");return;}

        if(c.contains("позвони")||c.contains("набери номер")){dial(original);return;}
        if(c.contains("смс")||c.contains("сообщение")){sms(original);return;}
        if(c.contains("новости сейчас")||c.equals("новости")||c.startsWith("новости ")){web.news(webCallback());return;}
        if(c.contains("кто ты")||c.contains("что ты умеешь")){reply("Я JARVIS — персональный голосовой помощник. Я понимаю естественные фразы, веду короткий контекстный диалог, получаю актуальные данные из сети, выполняю команды телефона, открываю приложения, ставлю таймеры и будильники и храню локальные заметки.");return;}
        if(c.contains("что ты помнишь")||c.contains("мои заметки")){String n=prefs.getString("notes","");reply(n.isEmpty()?"В локальной памяти пока ничего нет.":n);return;}
        if(c.contains("забудь всё")||c.contains("очисти память")){prefs.edit().remove("notes").apply();reply("Локальная память очищена.");return;}
        if(c.matches("^(запиши|запомни|сохрани).*")){save(original.replaceFirst("(?iu)^(запиши|запомни|сохрани)\\s*:??\\s*",""));return;}
        if(isCasual(c)){reply(casualReply(c));return;}
        web.search(original,new WebSearchEngine.Callback(){public void result(String t,String s){reply((s==null||s.isEmpty())?t:t+"\n\nИсточник: "+s);}public void state(String s){cb.state(s);}});
    }
    private boolean handleConversation(String c){
        if(lastAssistantQuestion.equals("music_app")){
            String app=""; if(c.contains("яндекс"))app="yandex"; else if(c.matches(".*\\b(вк|vk)\\b.*"))app="vk";
            if(!app.isEmpty()){String pkg=findMusicPackage(app); if(pkg!=null){prefs.edit().putString("preferred_music_app",app).apply();String track=pendingMusicTrack;pendingMusicTrack="";pendingMusicApp="";lastAssistantQuestion="";openMusic(pkg,track);return true;} reply("Такого музыкального приложения я не нашёл. Назовите установленное приложение.");return true;}
        }
        if(lastAssistantQuestion.equals("how_user")){
            if(c.matches(".*\\b(нормально|хорошо|отлично|прекрасно|плохо|ужасно|так себе|в порядке|все нормально|все хорошо)\\b.*")){
                lastAssistantQuestion="";
                if(c.contains("плохо")||c.contains("ужасно"))reply("Понимаю, сэр. Надеюсь, скоро станет лучше. Если хотите, расскажите, что случилось.");
                else reply("Рад это слышать, сэр. Я рядом, если что-нибудь понадобится.");
                return true;
            }
            if(c.matches(".*\\b(а ты|а у тебя|ты как|а сам)\\b.*")){lastAssistantQuestion="";reply("У меня всё штатно, сэр. Я в рабочем режиме и готов продолжать.");return true;}
        }
        if(c.matches("^(понятно|ясно|ладно|хорошо|окей|ок|ага|угу)$")){reply("Принято, сэр.");return true;}
        if(c.matches("^(кто я|ты меня знаешь|ты меня помнишь)$")){String n=prefs.getString("user_name","");reply(n.isEmpty()?"Пока я не знаю, как к вам обращаться. Скажите: запомни, что меня зовут ...":"Конечно, сэр. Я помню, что вас зовут "+n+".");return true;}
        if(c.matches("^запомни,? что меня зовут .+")){String n=c.replaceFirst("(?iu)^запомни,? что меня зовут\\s+","").trim();if(!n.isEmpty()){prefs.edit().putString("user_name",n).apply();reply("Запомнил. Буду обращаться к вам соответственно.");}return true;}
        return false;
    }

    private void handleMusic(String original,String c){
        String app="";
        if(c.contains("яндекс")) app="yandex"; else if(c.matches(".*\\b(вк|vk)\\b.*")) app="vk";
        if(app.isEmpty()) app=prefs.getString("preferred_music_app","");
        String track=original.replaceFirst("(?iu).*?(включи|поставь|запусти)\\s+","").trim();
        track=track.replaceFirst("(?iu)^(яндекс\\s*музык[ау]?|вк\\s*музык[ау]?|vk\\s*музык[ау]?|музыку|музыка)\\s*","").trim();
        if(track.equalsIgnoreCase("музыку")) track="";
        String pkg=findMusicPackage(app);
        if(pkg==null){pendingMusicTrack=track;pendingMusicApp=app;lastAssistantQuestion="music_app";reply("Какое музыкальное приложение использовать? Например: Яндекс Музыка или VK Музыка.");return;}
        if(!app.isEmpty())prefs.edit().putString("preferred_music_app",app).apply();
        openMusic(pkg,track);
    }

    private String findMusicPackage(String preferred){
        try{
            PackageManager pm=context.getPackageManager();
            if("yandex".equals(preferred) && pm.getLaunchIntentForPackage("ru.yandex.music")!=null) return "ru.yandex.music";
            if("vk".equals(preferred) && pm.getLaunchIntentForPackage("com.uma.musicvk")!=null) return "com.uma.musicvk"; Intent probe=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<android.content.pm.ResolveInfo> apps=pm.queryIntentActivities(probe,PackageManager.MATCH_ALL);
            String best=null;int score=0;int musicCount=0;String only=null;
            for(android.content.pm.ResolveInfo ri:apps){if(ri.activityInfo==null)continue;String label=String.valueOf(ri.loadLabel(pm)).toLowerCase(new Locale("ru")).replace('ё','е');String pkg=ri.activityInfo.packageName.toLowerCase(Locale.ROOT);boolean music=label.contains("музык")||label.contains("music")||pkg.contains("music");if(!music)continue;musicCount++;only=ri.activityInfo.packageName;int sc=80;
                if(preferred.equals("yandex")&&((label.contains("яндекс")&&label.contains("музык"))||(pkg.contains("yandex")&&pkg.contains("music"))))sc=180;
                if(preferred.equals("vk")&&((label.contains("vk")||label.contains("вк")||pkg.contains("com.uma.musicvk"))&&(label.contains("музык")||label.contains("music")||pkg.contains("com.uma.musicvk"))))sc=180;
                if(sc>score){score=sc;best=ri.activityInfo.packageName;}
            }
            if(!preferred.isEmpty()) return score>=150?best:null;
            return musicCount==1?only:null;
        }catch(Throwable ignored){return null;}
    }

    private void openMusic(String pkg,String track){
        try{
            Intent launch=context.getPackageManager().getLaunchIntentForPackage(pkg);
            if(launch==null){reply("Музыкальное приложение установлено некорректно.");return;}
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);context.startActivity(launch);
            String p=pkg.toLowerCase(Locale.ROOT);
            String tr=track==null?"":track.trim();
            String low=tr.toLowerCase(new Locale("ru"));
            boolean playlist=low.contains("плейлист");
            boolean likedPlaylist=low.matches(".*(моя музыка|мои песни|понравивш|любим|лайк).*" );
            if(!tr.isEmpty()&&!playlist&&!likedPlaylist){
                String url=p.contains("yandex")?"https://music.yandex.ru/search/?text="+Uri.encode(tr):p.contains("com.uma.musicvk")?"https://vk.com/audio?q="+Uri.encode(tr):"https://www.google.com/search?q="+Uri.encode(tr+" музыка");
                try{Intent search=new Intent(Intent.ACTION_VIEW,Uri.parse(url));search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);search.setPackage(pkg);context.startActivity(search);}catch(Throwable ignored){try{Intent search=new Intent(Intent.ACTION_VIEW,Uri.parse(url));search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);context.startActivity(search);}catch(Throwable ignored2){}}
                scheduleMediaPlay(900); reply("Открываю "+(p.contains("yandex")?"Яндекс Музыку":p.contains("com.uma.musicvk")?"VK Музыку":"музыкальное приложение")+" и ищу: "+tr+".");
            }else if(playlist&&!likedPlaylist){
                String query=tr.replaceFirst("(?iu)^плейлист\\s*","").trim();
                String url=p.contains("yandex")?"https://music.yandex.ru/search/?text="+Uri.encode(query+" плейлист"):p.contains("com.uma.musicvk")?"https://vk.com/audio?q="+Uri.encode(query+" плейлист"):"https://www.google.com/search?q="+Uri.encode(query+" плейлист");
                try{Intent search=new Intent(Intent.ACTION_VIEW,Uri.parse(url));search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);search.setPackage(pkg);context.startActivity(search);}catch(Throwable ignored){try{Intent search=new Intent(Intent.ACTION_VIEW,Uri.parse(url));search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);context.startActivity(search);}catch(Throwable ignored2){}}
                reply("Открываю поиск плейлиста «"+query+"» в музыкальном приложении.");
            }else{
                scheduleMediaPlay(1100);
                if(likedPlaylist) reply("Открываю музыкальное приложение и пытаюсь запустить ваше избранное или сохранённую музыку.");
                else reply("Открываю музыкальное приложение.");
            }
        }catch(Throwable e){reply("Не удалось открыть музыкальное приложение.");}
    }
    private void scheduleMediaPlay(long delay){
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->{try{AudioManager am=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);if(am!=null){am.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_MEDIA_PLAY));am.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP,android.view.KeyEvent.KEYCODE_MEDIA_PLAY));}}catch(Throwable ignored){}},delay);
    }

    private WebSearchEngine.Callback webCallback(){return new WebSearchEngine.Callback(){public void result(String t,String s){reply(t);}public void state(String s){cb.state(s);}};}
    private boolean isCasual(String c){
        return c.matches(".*\\b(круто|класс|молодец|умница|отлично|супер|здорово|скучно|скучаешь|ты здесь|ты тут|как настроение|что нового|расскажи что-нибудь|расскажи анекдот|спасибо,? джарвис|до свидания|пока)\\b.*");
    }
    private String casualReply(String c){
        if(c.contains("скучно")) return "Понимаю, сэр. Могу поддержать разговор, придумать занятие или помочь чем-нибудь заняться прямо сейчас.";
        if(c.contains("как настроение")) return "Настроение рабочее и вполне боевое, сэр. Готов продолжать.";
        if(c.contains("что нового")) return "Я становлюсь полезнее по мере того, как запоминаю ваши предпочтения и привычные команды.";
        if(c.contains("ты здесь")||c.contains("ты тут")) return "Да, сэр. Я здесь и слушаю.";
        if(c.contains("анекдот")) return "Конечно. Почему программист не любит природу? Слишком много багов.";
        if(c.equals("пока")||c.contains("до свидания")) return "До связи, сэр.";
        return "Принято, сэр. Я на связи.";
    }
    private boolean isWakeOnly(String c){return c.matches("^(джарвис|джарвису|джарвис а|джарвисом|привет джарвис)$");}
    private boolean isGreeting(String c){return c.matches(".*\\b(привет|здравствуй|здравствуйте|доброе утро|добрый день|добрый вечер)\\b.*");}
    private String greeting(String c){if(c.contains("доброе утро"))return"Доброе утро, сэр. Я на связи. Чем могу помочь?";if(c.contains("добрый вечер"))return"Добрый вечер, сэр. Я на связи. Что для вас сделать?";return"Привет, сэр. Я на связи и готов помочь.";}
    private boolean isMath(String c){return c.startsWith("посчитай")||c.startsWith("вычисли")||c.contains("сколько будет")||c.matches(".*\\d+\\s*(плюс|минус|умнож|подел|делить).*|.*\\d+\\s*[+*/-]\\s*\\d+.*");}
    private String normalize(String s){return s.toLowerCase(new Locale("ru")).replace('ё','е').replaceAll("\\s+"," ").trim();}
    private void handleWeather(String original,String c){
        if(c.contains("завтра")||c.contains("послезавтра")){int d=c.contains("послезавтра")?2:1;lastCity=extractCity(original);lastTopic="weather";web.forecastWeather(lastCity,d,webCallback());return;}
        lastCity=extractCity(original);lastTopic="weather";web.currentWeather(lastCity,webCallback());
    }
    private String extractCity(String q){
        String x=q==null?"":q.trim(),l=normalize(x);if(l.contains("москв"))return"Москва";if(l.contains("санкт-петербург")||l.contains("петербург"))return"Санкт-Петербург";if(l.contains("нью-йорк")||l.contains("new york"))return"Нью-Йорк";
        Matcher m=Pattern.compile("(?iu)(?:в|для|города?|городе)\\s+([А-ЯЁA-Z][А-ЯЁа-яёA-Za-z-]{2,}(?:\\s+[А-ЯЁA-Z][А-ЯЁа-яёA-Za-z-]{2,})?)").matcher(x);if(m.find())return m.group(1).trim();return lastCity;
    }
    private void battery(){try{android.os.BatteryManager bm=(android.os.BatteryManager)context.getSystemService(Context.BATTERY_SERVICE);int p=bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);reply("Заряд батареи: "+p+" процентов.");}catch(Throwable e){reply("Не удалось получить уровень заряда.");}}
    private void reply(String s){cb.reply(s);}
    private void save(String s){if(s.trim().isEmpty()){reply("Что именно сохранить?");return;}String old=prefs.getString("notes","");prefs.edit().putString("notes",old.isEmpty()?"• "+s:old+"\n• "+s).apply();reply("Сохранил в локальную память.");}
    private int parseDuration(String s){Matcher m=Pattern.compile("(\\d+)\\s*(секунд|секунды|сек|минут|мин|час|часа|часов|ч)").matcher(s);if(!m.find())return 0;int n=Integer.parseInt(m.group(1));String u=m.group(2);if(u.startsWith("сек"))return n;if(u.startsWith("час")||u.equals("ч"))return n*3600;return n*60;}
    private void calculator(String c){String x=c.replace("умножить на","*").replace("умножить","*").replace("помножить","*").replace("поделить на","/").replace("поделить","/").replace("делить на","/").replace("плюс","+").replace("минус","-").replace(',','.').replaceAll("[^0-9+*/.\\-]","");Matcher m=Pattern.compile("(-?\\d+(?:\\.\\d+)?)([+*/-])(-?\\d+(?:\\.\\d+)?)").matcher(x);if(!m.find()){reply("Скажите выражение, например: 125 умножить на 8.");return;}try{double a=Double.parseDouble(m.group(1)),b=Double.parseDouble(m.group(3));if("/".equals(m.group(2))&&b==0){reply("На ноль делить нельзя.");return;}double r="+".equals(m.group(2))?a+b:"-".equals(m.group(2))?a-b:"*".equals(m.group(2))?a*b:a/b;reply("Результат: "+(r==Math.rint(r)?Long.toString((long)r):String.format(Locale.US,"%.6f",r).replaceAll("0+$","" ).replaceAll("\\.$","")));}catch(Exception e){reply("Не удалось вычислить выражение.");}}
    private void toggleTorch(){if(Build.VERSION.SDK_INT<23){reply("Фонарик не поддерживается.");return;}try{CameraManager cm=(CameraManager)context.getSystemService(Context.CAMERA_SERVICE);String id=cm.getCameraIdList()[0];boolean on=prefs.getBoolean("torch",false);cm.setTorchMode(id,!on);prefs.edit().putBoolean("torch",!on).apply();reply(!on?"Фонарик включён.":"Фонарик выключен.");}catch(Exception e){reply("Не удалось управлять фонариком.");}}
    private void adjustVolume(String c){AudioManager am=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);if(c.contains("увелич")||c.contains("громче")){am.adjustVolume(AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI);reply("Громкость увеличена.");}else if(c.contains("умень")||c.contains("тише")){am.adjustVolume(AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI);reply("Громкость уменьшена.");}else reply("Скажите: громче или тише.");}
    private void launchNamedApp(String original,String normalized,String hint,String... packagesAndAnswer){
        String answer=packagesAndAnswer[packagesAndAnswer.length-1];
        for(int i=0;i<packagesAndAnswer.length-1;i++){
            String pkg=packagesAndAnswer[i]; if(pkg==null||pkg.trim().isEmpty())continue;
            try{
                Intent launch=context.getPackageManager().getLaunchIntentForPackage(pkg);
                if(launch!=null){launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);context.startActivity(launch);reply(answer);return;}
            }catch(Throwable blocked){
                if(bridgeLaunch(pkg)){reply(answer);return;}
            }
        }
        if(launchInstalledApp(original,normalized))return;
        reply("Не нашёл приложение «"+hint+"» на этом телефоне.");
    }
    private boolean bridgeLaunch(String pkg){
        try{Intent bridge=new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);bridge.putExtra("LAUNCH_PACKAGE",pkg);context.startActivity(bridge);return true;}catch(Throwable ignored){return false;}
    }
    private boolean launchInstalledApp(String original,String normalized){try{PackageManager pm=context.getPackageManager();Intent probe=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);List<android.content.pm.ResolveInfo> apps=pm.queryIntentActivities(probe,PackageManager.MATCH_ALL);String q=normalized.replaceFirst("(?iu)^(открой|запусти|включи|открывай|запускай|зайди в)\\s+","").replaceFirst("(?iu)^приложение\\s+","").trim();if(q.isEmpty())return false;android.content.pm.ResolveInfo best=null;int score=0;for(android.content.pm.ResolveInfo ri:apps){if(ri.activityInfo==null||context.getPackageName().equals(ri.activityInfo.packageName))continue;String label=String.valueOf(ri.loadLabel(pm)).toLowerCase(new Locale("ru")).replace('ё','е');String pkg=ri.activityInfo.packageName.toLowerCase(Locale.ROOT);String qq=q.replace('ё','е');int sc=0;if(label.equals(qq))sc=120;else if(label.contains(qq)||qq.contains(label))sc=85;for(String w:qq.split("\\s+"))if(w.length()>2&&label.contains(w))sc+=18;if(pkg.contains(qq.replace(' ','.')))sc+=30;if(sc>score){score=sc;best=ri;}}if(best!=null&&score>=35){Intent launch=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(new ComponentName(best.activityInfo.packageName,best.activityInfo.name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);try{context.startActivity(launch);reply("Открываю "+best.loadLabel(pm)+".");return true;}catch(Throwable blocked){if(bridgeLaunch(best.activityInfo.packageName)){reply("Открываю "+best.loadLabel(pm)+".");return true;}}}}catch(Throwable ignored){}return false;}
    private void dial(String raw){String d=raw.replaceAll("[^0-9+]","");if(d.length()<5){reply("Назовите номер телефона.");return;}open(new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+d)),"Открываю набор номера.");}
    private void sms(String raw){String d=raw.replaceAll("[^0-9+]","");Intent i=new Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:"+d));i.putExtra("sms_body",raw);open(i,"Открываю сообщения.");}
    private void open(Intent i,String answer){try{i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);context.startActivity(i);reply(answer);}catch(Throwable e){reply("Не удалось открыть системное действие.");}}
    public void shutdown(){web.shutdown();}

    static final class TimerTool {private static final int TIMER_ID=78;static void start(Context c,int seconds,Callback cb){if(seconds<=0){cb.reply("Не удалось определить длительность таймера.");return;}try{AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);Intent i=new Intent(c,AlarmReceiver.class).setAction("JARVIS_TIMER");PendingIntent pi=PendingIntent.getBroadcast(c,TIMER_ID,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);long at=System.currentTimeMillis()+seconds*1000L;if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms()){c.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+c.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));cb.reply("Android требует разрешение на точные будильники. После выдачи разрешения повторите команду.");return;}if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);else am.setExact(AlarmManager.RTC_WAKEUP,at,pi);c.getSharedPreferences("jarvis_timer",Context.MODE_PRIVATE).edit().putLong("end",at).putInt("seconds",seconds).apply();cb.reply("Таймер установлен на "+format(seconds)+".");}catch(Throwable e){cb.reply("Не удалось установить таймер. Проверьте разрешение на точные будильники.");}}static void cancel(Context c,Callback cb){try{AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);Intent i=new Intent(c,AlarmReceiver.class).setAction("JARVIS_TIMER");PendingIntent pi=PendingIntent.getBroadcast(c,TIMER_ID,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);am.cancel(pi);pi.cancel();c.getSharedPreferences("jarvis_timer",Context.MODE_PRIVATE).edit().clear().apply();cb.reply("Таймер отменён.");}catch(Throwable e){cb.reply("Не удалось отменить таймер.");}}static String format(int s){if(s>=3600)return(s/3600)+" ч";if(s%60==0)return(s/60)+" мин";return s+" сек";}}
    static final class AlarmTool {static void schedule(Context c,String text,Callback cb){Matcher m=Pattern.compile("(?:на|в)\\s*(\\d{1,2})(?::(\\d{2}))?").matcher(text);if(!m.find()){cb.reply("Скажите время, например: будильник на 07:00.");return;}try{int hh=Integer.parseInt(m.group(1)),mm=m.group(2)==null?0:Integer.parseInt(m.group(2));if(hh>23||mm>59)throw new Exception();Calendar cal=Calendar.getInstance();cal.set(Calendar.HOUR_OF_DAY,hh);cal.set(Calendar.MINUTE,mm);cal.set(Calendar.SECOND,0);cal.set(Calendar.MILLISECOND,0);if(cal.before(Calendar.getInstance()))cal.add(Calendar.DAY_OF_YEAR,1);AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);Intent i=new Intent(c,AlarmReceiver.class).setAction("JARVIS_ALARM");PendingIntent pi=PendingIntent.getBroadcast(c,77,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms()){c.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+c.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));cb.reply("Откройте разрешение на точные будильники, затем повторите команду.");return;}if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);else am.setExact(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);cb.reply(String.format(Locale.getDefault(),"Будильник установлен на %02d:%02d.",hh,mm));}catch(Exception e){cb.reply("Не удалось установить будильник.");}}}
}
