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
    private final Context context; private final Callback cb; private final SharedPreferences prefs; private final WebSearchEngine web=new WebSearchEngine(); private final JarvisMemory memory; private final JarvisAdaptiveBrain adaptive; private final VideoSearchEngine video=new VideoSearchEngine(); private final JarvisLocalAI localAI=new JarvisLocalAI();
    private String lastCity="Москва", lastTopic="", lastUserMessage="";
    private String previousUserMessage="";
    private boolean executingLearned=false;
    private String lastAssistantQuestion="";
    private String pendingMusicTrack="";
    private String pendingMusicApp="";
    private String lastIntentCommand="";
    public JarvisEngine(Context c,Callback callback){context=c.getApplicationContext();cb=callback;prefs=context.getSharedPreferences("jarvis_local",Context.MODE_PRIVATE);memory=new JarvisMemory(context);adaptive=new JarvisAdaptiveBrain(context);}

    public void handle(final String raw){
        try { handleInternal(raw); } catch (Throwable fatal) { cb.state("ГОТОВ"); cb.reply("Я не смог безопасно выполнить эту команду. Попробуйте сказать её иначе."); }
    }

    private void handleInternal(final String raw){
        if(raw==null||raw.trim().isEmpty())return;
        final String original=raw.trim(); String c=JarvisSmartRouter.normalize(original); previousUserMessage=lastUserMessage; lastUserMessage=original; lastIntentCommand=c; learnPreference(original,c); cb.state("ОБРАБОТКА");
        JarvisLocalAI.Decision ai=localAI.analyze(original,c,lastTopic,prefs.getString("preferred_music_app",""),memory);
        JarvisAdaptiveBrain.Match learnedBrain=adaptive.predict(original);
        if(learnedBrain.confidence>=0.84 && !learnedBrain.intent.isEmpty() && !"UNKNOWN".equals(learnedBrain.intent)) {
            c=JarvisSmartRouter.normalize(learnedBrain.intent);
            cb.state("АДАПТИВНОЕ ПОНИМАНИЕ");
        }
        if(ai.confidence>=0.72 && !"UNKNOWN".equals(ai.intent) && !"LEARNED".equals(ai.intent)){
            if(JarvisLocalAI.NEXT.equals(ai.intent)||JarvisLocalAI.PREVIOUS.equals(ai.intent)||JarvisLocalAI.PAUSE.equals(ai.intent)||JarvisLocalAI.PLAY.equals(ai.intent)) c=ai.canonical;
            else if(JarvisLocalAI.WEATHER.equals(ai.intent) && (c.equals("завтра")||c.equals("послезавтра")||c.equals("а завтра")||c.equals("а послезавтра"))) c=ai.canonical;
        }
        if("LEARNED".equals(ai.intent) && !ai.canonical.isEmpty()) { c=JarvisSmartRouter.normalize(ai.canonical); cb.state("ОБУЧЕННАЯ КОМАНДА"); }
        lastIntentCommand=c;
        if(isWakeOnly(c)){reply("Я на связи, сэр. Слушаю вас.");return;}
        if(handleConversation(c))return;
        if(isGreeting(c)&&!(c.contains("как дела")||c.contains("как ты")||c.contains("как поживаешь"))){reply(greeting(c));return;}
        if(c.contains("как дела")||c.contains("как ты")||c.contains("как поживаешь")){lastAssistantQuestion="how_user";reply("Отлично, сэр. Работаю стабильно и готов помочь. А у вас как дела?");return;}
        if(c.contains("спасибо")||c.contains("благодарю")){reply("Всегда пожалуйста, сэр. Рад быть полезным.");return;}
        if(c.matches(".*\\b(что нового|как настроение|ты устал|ты занят|чем занимаешься|что делаешь)\\b.*")){reply(c.contains("что нового")?"Я здесь, слежу за вашим контекстом и готов помочь.":"У меня всё штатно. Я готов работать с вами.");return;}
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

        if(c.matches(".*(что сейчас играет|что играет|какая песня играет|название песни).*")){reply(currentTrack());return;}
        if(c.matches(".*(поставь лайк|лайк|мне нравится этот трек).*")){reply(rateCurrent(true)?"Поставил лайк текущему треку.":"Текущий плеер не поддерживает установку лайка через Android.");return;}
        if(c.matches(".*(поставь дизлайк|дизлайк|мне не нравится этот трек).*")){reply(rateCurrent(false)?"Поставил дизлайк текущему треку.":"Текущий плеер не поддерживает установку дизлайка через Android.");return;}
        if(JarvisSmartRouter.isMediaControl(c)){handleMediaControl(c);return;}
        if(c.matches(".*(фильм|сериал|кино|видео).*") && c.matches(".*(найди|покажи|ищи|где посмотреть|включи).*") ){String q=extractVideoQuery(original);video.search(q,new VideoSearchEngine.Callback(){public void result(String t){reply(t);}public void state(String s){cb.state(s);}});return;}
        if(c.matches(".*(управление плеером|доступ к медиасеансам|доступ к медиа).*")) {open(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),"Открываю доступ к управлению медиаплеером.");return;}
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
        if(c.contains("кто ты")||c.contains("что ты умеешь")){reply("Я JARVIS — персональный голосовой помощник. Я понимаю естественные фразы, веду контекстный диалог, запоминаю ваши предпочтения, управляю приложениями и мультимедиа, выполняю команды телефона и использую интернет, когда действительно нужен внешний ответ.");return;}
        if(c.contains("что ты помнишь")||c.contains("мои заметки")){String n=prefs.getString("notes","");reply(n.isEmpty()?"В локальной памяти пока ничего нет.":n);return;}
        if(c.contains("забудь всё")||c.contains("очисти память")){prefs.edit().remove("notes").apply();memory.clear();reply("Локальная память очищена.");return;}
        if(c.matches("^(запиши|запомни|сохрани).*")){save(original.replaceFirst("(?iu)^(запиши|запомни|сохрани)\\s*:??\\s*",""));return;}
        if(c.matches(".*(научи|запомни команду|если я говорю).*(делай|выполняй).*") ){String z=original.replaceFirst("(?iu).*?(?:научи|запомни команду|если я говорю)\\s*","");Matcher lm=Pattern.compile("(?iu)^(.+?)\\s+(?:то\\s+)?(?:делай|выполняй)\\s+(.+)$").matcher(z);if(lm.find()){String phrase=lm.group(1).trim(),action=lm.group(2).trim();memory.learnAlias(phrase,action);reply("Запомнил. Когда вы скажете «"+phrase+"», я буду выполнять: «"+action+"»." );}else reply("Скажите: «Научи: когда я говорю открыть музыку, выполняй открой Яндекс Музыку»." );return;}
        String learned=memory.alias(c);
        if(!executingLearned&&!learned.isEmpty()&&!learned.equalsIgnoreCase(original)){cb.state("ОБУЧЕННАЯ КОМАНДА");executingLearned=true;try{handleInternal(learned);}finally{executingLearned=false;}return;}
        String inferred=memory.inferAlias(c);
        if(!executingLearned&&!inferred.isEmpty()&&!inferred.equalsIgnoreCase(c)){cb.state("АДАПТИРОВАЛ КОМАНДУ");executingLearned=true;try{handleInternal(inferred);}finally{executingLearned=false;}return;}
        if(isCasual(c)||isShortConversation(c)){reply(casualReply(c));return;}
        if(isExplicitInformationRequest(c)){ searchKnowledge(original); return; }
        // Unknown natural-language questions are routed to the web knowledge layer instead of being rejected.
        if(c.endsWith("?") || c.length()>12){ searchKnowledge(original); return; }
        reply("Я понял вас, но для выполнения действия мне не хватает контекста. Уточните, что именно сделать.");
    }
    private void searchKnowledge(String query){
        String q=query==null?"":query.trim();
        if(q.isEmpty()){reply("Скажите, что именно нужно узнать.");return;}
        boolean volatileInfo=isExplicitInformationRequest(JarvisSmartRouter.normalize(q));
        String cached=adaptive.cachedKnowledge(q,volatileInfo);
        if(!cached.isEmpty()){cb.state("ЗНАНИЕ ИЗ ПАМЯТИ");reply(cached);return;}
        final String fq=q;
        web.search(fq,new WebSearchEngine.Callback(){
            public void result(String t,String s){
                String out=(s==null||s.isEmpty())?t:t+"\n\nИсточник: "+s;
                if(t!=null&&!t.toLowerCase(new Locale("ru")).contains("не удалось")) adaptive.cacheKnowledge(fq,out,s);
                reply(out);
            }
            public void state(String s){cb.state(s);}
        });
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
        if(c.matches("^(понятно|ясно|ладно|хорошо|окей|ок|ага|угу|нормально|все нормально|все хорошо|хорошо, спасибо|нормально, спасибо)$")){lastAssistantQuestion="";reply(c.contains("нормально")?"Рад это слышать, сэр.":"Принято, сэр.");return true;}
        if(c.matches("^(кто я|ты меня знаешь|ты меня помнишь)$")){String n=prefs.getString("user_name","");reply(n.isEmpty()?"Пока я не знаю, как к вам обращаться. Скажите: запомни, что меня зовут ...":"Конечно, сэр. Я помню, что вас зовут "+n+".");return true;}
        if(c.matches("^запомни,? что меня зовут .+")){String n=c.replaceFirst("(?iu)^запомни,? что меня зовут\\s+","").trim();if(!n.isEmpty()){prefs.edit().putString("user_name",n).apply();reply("Запомнил. Буду обращаться к вам соответственно.");}return true;}
        return false;
    }

    /**
     * Context-aware music controller. The standard Android media-session path is
     * provider-neutral; provider-specific URLs are used only when the user asks
     * to search/open content in an app.
     */
    private void handleMusic(String original,String c){
        String app="";
        if(c.contains("яндекс")) app="yandex";
        else if(c.matches(".*\\b(вк|vk)\\b.*")) app="vk";
        else if(c.contains("spotify")) app="spotify";
        else if(c.contains("youtube music")||c.contains("ютуб музыку")||c.contains("ютуб музыка")) app="youtube";

        boolean liked=c.matches(".*(моя любим|мои любим|любим(ая|ое|ые)|понравивш|избранн|лайкнут|моя музыка|мои песни).*");
        boolean continuePlay=c.matches(".*(продолж|возобнов|дальше|включи обратно|сними с пауз).*");
        boolean shuffle=c.matches(".*(вперемешку|перемешай|случайн|рандом).*");
        boolean playlist=c.contains("плейлист");

        // "В дороге / для танцев / для спорта / для работы" are semantic modes,
        // not fixed playlists. The mode is remembered and can be replaced later.
        String mode=extractMusicMode(c);
        if(!mode.isEmpty()) prefs.edit().putString("music_mode",mode).apply();
        else mode=prefs.getString("music_mode","");

        String track=extractMusicTarget(original);
        if(isMusicModeOnly(track))track="";

        // "переключи с танцев на дорогу" should replace the mode and search again,
        // rather than merely skipping the current song.
        if(!mode.isEmpty() && (track.isEmpty() || track.matches("(?iu).*(с|из)\\s+(?:режима\\s+)?(танцев|дороги|спорта|работы|релаксации|сна).*")) && !liked && !continuePlay && !playlist){
            track=mode;
        }

        if(continuePlay && track.isEmpty() && !liked){
            boolean ok=JarvisMediaSessionService.control("play");
            if(!ok) scheduleMediaPlay(150);
            reply(ok?"Продолжаю воспроизведение.":"Пытаюсь продолжить воспроизведение в активном плеере.");
            return;
        }

        if(liked && track.isEmpty()){
            String preferred=prefs.getString("preferred_music_app","");
            String pkg=findMusicPackage(preferred);
            if(pkg==null){
                String active=JarvisMediaSessionService.activePackage();
                if(!active.isEmpty())pkg=active;
            }
            if(pkg==null){
                pendingMusicTrack="";
                pendingMusicApp=preferred;
                lastAssistantQuestion="music_app";
                reply("Какое музыкальное приложение использовать для ваших понравившихся?");
                return;
            }
            prefs.edit().putString("preferred_music_app",providerKey(pkg)).apply();
            openMusic(pkg,"",true,false,false,false);
            return;
        }

        if(app.isEmpty()) app=prefs.getString("preferred_music_app","");
        String pkg=findMusicPackage(app);

        // If no provider is selected, prefer the currently active media app.
        if(pkg==null && app.isEmpty()){
            String active=JarvisMediaSessionService.activePackage();
            if(!active.isEmpty())pkg=active;
        }

        // If the user named an installed app we do not know, resolve it by label.
        if(pkg==null && !app.isEmpty()) pkg=findInstalledAppByText(app);

        if(pkg==null){
            pendingMusicTrack=track;
            pendingMusicApp=app;
            lastAssistantQuestion="music_app";
            reply("Какое музыкальное приложение использовать? Можно назвать любое установленное приложение.");
            return;
        }

        prefs.edit().putString("preferred_music_app",providerKey(pkg)).apply();
        openMusic(pkg,track,false,false,shuffle,playlist);
    }

    private String extractMusicTarget(String original){
        String q=original==null?"":original.trim();
        q=q.replaceFirst("(?iu)^.*?(включи|поставь|запусти|проиграй|сыграй|найди|поищи|воспроизведи|переключи|измени|смени)\\s*","");
        q=q.replaceFirst("(?iu)^(?:мне\\s+)?(?:музыку|музыка|песни|песню|трек)\\s*","");
        q=q.replaceFirst("(?iu)^(?:в|для)\\s+(дороге|танцев|спорта|работы|фона|сна|релаксации|отдыха)\\s*$","");
        q=q.replaceFirst("(?iu)^.*?(?:с|из)\\s+(?:режима\\s+)?(?:танцев|дороги|спорта|работы|релаксации|сна)\\s+(?:на|в|для)\\s+(?:режим\\s+)?(?:дорог[уе]|танцев|спорта|работы|релаксаци[ию]|сна).*$","");
        return q.trim();
    }

    private String extractMusicMode(String c){
        if(c.contains("в дороге")||c.contains("для дороги")||c.contains("дорожн")||c.contains("за рулем")||c.contains("за рулём")) return "в дорогу";
        if(c.contains("для танцев")||c.contains("танцевальн")||c.contains("потанцевать")) return "для танцев";
        if(c.contains("для спорта")||c.contains("трениров")||c.contains("в спортзале")) return "для спорта";
        if(c.contains("для работы")||c.contains("работать")||c.contains("концентрац")) return "для работы";
        if(c.contains("расслаб")||c.contains("релакс")) return "для расслабления";
        if(c.contains("для сна")||c.contains("уснуть")) return "для сна";
        return "";
    }

    private boolean isMusicModeOnly(String q){
        if(q==null||q.isEmpty())return true;
        String x=q.toLowerCase(new Locale("ru")).trim();
        return x.matches("^(в\\s+дорогу|для\\s+дороги|для\\s+танцев|для\\s+спорта|для\\s+работы|для\\s+расслабления|для\\s+сна)$");
    }

    private String providerKey(String pkg){
        String p=pkg==null?"":pkg.toLowerCase(Locale.ROOT);
        if(p.contains("yandex")&&p.contains("music"))return "yandex";
        if(p.contains("spotify"))return "spotify";
        if(p.contains("uma.musicvk")||p.contains("vk"))return "vk";
        if(p.contains("youtube")&&p.contains("music"))return "youtube";
        return pkg==null?"":pkg;
    }

    private String findInstalledAppByText(String text){
        if(text==null||text.trim().isEmpty())return null;
        try{
            PackageManager pm=context.getPackageManager();
            Intent probe=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            String q=text.toLowerCase(new Locale("ru")).replace('ё','е').trim();
            android.content.pm.ResolveInfo best=null;int score=0;
            for(android.content.pm.ResolveInfo ri:pm.queryIntentActivities(probe,PackageManager.MATCH_ALL)){
                if(ri.activityInfo==null||context.getPackageName().equals(ri.activityInfo.packageName))continue;
                String label=String.valueOf(ri.loadLabel(pm)).toLowerCase(new Locale("ru")).replace('ё','е');
                String pkg=ri.activityInfo.packageName.toLowerCase(Locale.ROOT);
                int sc=0;
                if(label.equals(q))sc=120;
                else if(label.contains(q)||q.contains(label))sc=85;
                for(String w:q.split("\\s+"))if(w.length()>2&&label.contains(w))sc+=15;
                if(pkg.contains(q.replace(' ','.')))sc+=25;
                if(sc>score){score=sc;best=ri;}
            }
            return best!=null&&score>=35?best.activityInfo.packageName:null;
        }catch(Throwable ignored){return null;}
    }

    private String findMusicPackage(String preferred){
        try{
            PackageManager pm=context.getPackageManager();
            if(preferred==null)preferred="";
            String p=preferred.toLowerCase(Locale.ROOT);
            if(preferred.contains(".") && pm.getLaunchIntentForPackage(preferred)!=null) return preferred;
            if("yandex".equals(p)&&pm.getLaunchIntentForPackage("ru.yandex.music")!=null)return "ru.yandex.music";
            if("vk".equals(p)&&pm.getLaunchIntentForPackage("com.uma.musicvk")!=null)return "com.uma.musicvk";
            if("spotify".equals(p)&&pm.getLaunchIntentForPackage("com.spotify.music")!=null)return "com.spotify.music";
            if("youtube".equals(p)&&pm.getLaunchIntentForPackage("com.google.android.apps.youtube.music")!=null)return "com.google.android.apps.youtube.music";

            String active=JarvisMediaSessionService.activePackage();
            if(p.isEmpty()&&!active.isEmpty()&&pm.getLaunchIntentForPackage(active)!=null)return active;

            Intent probe=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            String best=null;int score=0;
            for(android.content.pm.ResolveInfo ri:pm.queryIntentActivities(probe,PackageManager.MATCH_ALL)){
                if(ri.activityInfo==null)continue;
                String label=String.valueOf(ri.loadLabel(pm)).toLowerCase(new Locale("ru")).replace('ё','е');
                String pkg=ri.activityInfo.packageName.toLowerCase(Locale.ROOT);
                boolean media=label.contains("музык")||label.contains("music")||label.contains("spotify")||
                        label.contains("радио")||label.contains("podcast")||pkg.contains("music")||pkg.contains("spotify");
                if(!media)continue;
                int sc=60;
                if(p.contains("yandex")&&(label.contains("яндекс")||pkg.contains("yandex")))sc+=120;
                if(p.contains("vk")&&(label.contains("vk")||label.contains("вк")||pkg.contains("vk")))sc+=120;
                if(p.contains("spotify")&&(label.contains("spotify")||pkg.contains("spotify")))sc+=120;
                if(p.contains("youtube")&&(label.contains("youtube")||pkg.contains("youtube")))sc+=120;
                if(sc>score){score=sc;best=ri.activityInfo.packageName;}
            }
            return p.isEmpty()&&score>=60?best:(score>=150?best:null);
        }catch(Throwable ignored){return null;}
    }

    private void openMusic(String pkg,String track){openMusic(pkg,track,false,false,false,false);}

    private void openMusic(String pkg,String track,boolean liked,boolean continuePlay,boolean shuffle,boolean playlist){
        try{
            Intent launch=context.getPackageManager().getLaunchIntentForPackage(pkg);
            if(launch==null){reply("Музыкальное приложение установлено некорректно.");return;}
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launch);

            String p=pkg.toLowerCase(Locale.ROOT),tr=track==null?"":track.trim();
            String url=null;
            if(liked){
                if(p.contains("yandex"))url="https://music.yandex.ru/collection/track-likes";
                else if(p.contains("com.uma.musicvk"))url="https://vk.com/audio";
                else if(p.contains("spotify"))url="https://open.spotify.com/collection/tracks";
                else if(p.contains("youtube"))url="https://music.youtube.com/playlist?list=LM";
            }else if(playlist){
                String q=tr.replaceFirst("(?iu)^плейлист\\s*","").trim();
                if(!q.isEmpty())url=providerSearchUrl(p,q+" плейлист");
            }else if(!tr.isEmpty()){
                String query=tr;
                if(query.matches("(?iu).*(новин|новую музыку|новинки музыки).*"))query="новинки";
                url=providerSearchUrl(p,query);
            }else if(shuffle){
                String mode=prefs.getString("music_mode","");
                if(!mode.isEmpty())url=providerSearchUrl(p,mode);
            }

            if(url!=null){
                try{
                    Intent search=new Intent(Intent.ACTION_VIEW,Uri.parse(url));
                    search.setPackage(pkg);
                    search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(search);
                }catch(Throwable ignored){
                    try{context.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Throwable ignored2){}
                }
            }

            // Give the app time to publish its MediaSession, then issue play.
            scheduleMediaPlay(1100);
            if(liked)reply("Открываю понравившуюся музыку в "+providerName(p)+".");
            else if(continuePlay)reply("Продолжаю воспроизведение.");
            else if(shuffle)reply("Включаю музыку в режиме "+(prefs.getString("music_mode","случайный выбор"))+".");
            else if(!tr.isEmpty())reply("Ищу «"+tr+"» и запускаю воспроизведение.");
            else reply("Открываю музыкальный плеер.");
        }catch(Throwable e){reply("Не удалось открыть музыкальное приложение.");}
    }

    private String providerSearchUrl(String p,String query){
        String q=Uri.encode(query==null?"":query);
        if(p.contains("yandex")&&p.contains("music"))return "https://music.yandex.ru/search/?text="+q;
        if(p.contains("com.uma.musicvk")||p.contains("vk"))return "https://vk.com/audio?q="+q;
        if(p.contains("spotify"))return "https://open.spotify.com/search/"+q;
        if(p.contains("youtube")&&p.contains("music"))return "https://music.youtube.com/search?q="+q;
        return "https://www.google.com/search?q="+q+"%20музыка";
    }

    private String providerName(String p){
        if(p.contains("yandex"))return "Яндекс Музыке";
        if(p.contains("spotify"))return "Spotify";
        if(p.contains("youtube"))return "YouTube Music";
        if(p.contains("vk"))return "VK Музыке";
        return "выбранном плеере";
    }

    private void scheduleMediaPlay(long delay){
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->{
            try{
                if(JarvisMediaSessionService.control("play")) return;
                AudioManager am=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
                if(am!=null){
                    am.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_MEDIA_PLAY));
                    am.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP,android.view.KeyEvent.KEYCODE_MEDIA_PLAY));
                }
            }catch(Throwable ignored){}
        },delay);
    }

    private void handleMediaControl(String c){
        String action;
        if(c.contains("пауза")||c.contains("стоп")) action="pause";
        else if(c.contains("предыдущ")||c.contains("назад")||c.contains("верни песню")) action="previous";
        else if(c.contains("следующ")||c.contains("дальше")) action="next";
        else action="play";
        boolean ok=JarvisMediaSessionService.control(action);
        if(!ok){
            try{
                AudioManager am=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
                if(am!=null){
                    int key="play".equals(action)?android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                            "next".equals(action)?android.view.KeyEvent.KEYCODE_MEDIA_NEXT:android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS;
                    am.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,key));
                    am.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP,key));
                    ok=true;
                }
            }catch(Throwable ignored){}
        }
        if(ok){
            String answer="pause".equals(action)?"Поставил воспроизведение на паузу.":"play".equals(action)?"Продолжаю воспроизведение.":"next".equals(action)?"Переключаю на следующий трек.":"Возвращаю предыдущий трек.";
            reply(answer);
        }else reply("Не вижу активного плеера. Откройте музыкальное приложение или дайте JARVIS доступ к медиасеансам.");
    }
    private String currentTrack(){
        try{ JarvisMediaSessionService.TrackInfo info=JarvisMediaSessionService.currentTrack(); if(info!=null){ if(info.artist!=null&&!info.artist.isEmpty()) return "Сейчас играет: "+info.title+" — "+info.artist+"."; return "Сейчас играет: "+info.title+"."; } }catch(Throwable ignored){}
        return "Не удалось определить текущий трек через активный медиасеанс.";
    }
    private boolean rateCurrent(boolean like){ return JarvisMediaSessionService.rateCurrent(like); }

    private String extractVideoQuery(String original){String q=original.replaceFirst("(?iu)^(.*?)(найди|покажи|ищи|где посмотреть|включи)\\s*"," ").trim();q=q.replaceFirst("(?iu)\\s+(фильм|сериал|кино|видео)\\s*"," ").trim();return q.isEmpty()?original:q;}
    private boolean isShortConversation(String c){return c.length()<=28 && c.matches(".*\\b(нормально|хорошо|плохо|отлично|спасибо|пожалуйста|устал|занят|скучно|ясно|понятно)\\b.*");}
    private boolean isExplicitInformationRequest(String c){return c.matches(".*\\b(найди|ищи|поищи|что такое|кто такой|кто такая|где|когда|почему|сколько стоит|курс|цена|новости|погода|информация|расскажи про)\\b.*");}

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
    private void learnPreference(String raw,String normalized){
        String x=JarvisSmartRouter.normalize(raw);
        try{
            Matcher m=Pattern.compile("(?iu)^(?:я\\s+)?(?:очень\\s+)?(?:люблю|нравится\\s+мне|мне\\s+нравится)\\s+(.+)$").matcher(x);
            if(m.find()){String v=m.group(1).trim();if(v.length()>=2&&v.length()<=80)prefs.edit().putString("favorite_preference",v).apply();}
            m=Pattern.compile("(?iu)^(?:я\\s+)?(?:не\\s+люблю|мне\\s+не\\s+нравится)\\s+(.+)$").matcher(x);
            if(m.find()){String v=m.group(1).trim();if(v.length()>=2&&v.length()<=80)prefs.edit().putString("disliked_preference",v).apply();}
        }catch(Throwable ignored){}
    }

    private boolean shouldAutoLearn(String phrase){
        if(phrase==null)return false; String q=JarvisSmartRouter.normalize(phrase);
        if(q.length()<5||q.length()>180)return false;
        return q.matches(".*(включ|выключ|открой|запуст|постав|продолж|возобнов|пауза|стоп|следующ|предыдущ|дальше|громче|тише|таймер|будильник|фонарик|камера|позвони|напиши|найди|покажи|музык|плейлист|лайк|дизлайк|запиши|сохрани|запомни|сделай).*" );
    }

    private void reply(String s){
        if(s==null)s="";
        String low=s.toLowerCase(new Locale("ru"));
        boolean failure=low.contains("не удалось")||low.contains("не смог")||low.contains("не наш")||low.contains("не вижу")||low.contains("ошиб")||low.contains("недоступ")||low.contains("не удалось");
        if(!failure && shouldAutoLearn(lastUserMessage)){
            if(!lastIntentCommand.isEmpty()&&!lastUserMessage.equalsIgnoreCase(lastIntentCommand)){ memory.learnSuccessful(lastUserMessage,lastIntentCommand); adaptive.learn(lastUserMessage,lastIntentCommand); }
        } else if(failure && !lastUserMessage.isEmpty()) { adaptive.reject(lastUserMessage); }
        memory.addTurn(lastUserMessage,s);cb.reply(s);
    }
    private void save(String s){if(s.trim().isEmpty()){reply("Что именно сохранить?");return;}String old=prefs.getString("notes","");prefs.edit().putString("notes",old.isEmpty()?"• "+s:old+"\n• "+s).apply();reply("Сохранил в локальную память.");}
    private int parseDuration(String s){Matcher m=Pattern.compile("(\\d+)\\s*(секунд|секунды|сек|минут|мин|час|часа|часов|ч)").matcher(s);if(!m.find())return 0;int n=Integer.parseInt(m.group(1));String u=m.group(2);if(u.startsWith("сек"))return n;if(u.startsWith("час")||u.equals("ч"))return n*3600;return n*60;}
    private void calculator(String c){String x=c.replace("умножить на","*").replace("умножить","*").replace("помножить","*").replace("поделить на","/").replace("поделить","/").replace("делить на","/").replace("плюс","+").replace("минус","-").replace(',','.').replaceAll("[^0-9+*/.\\-]","");Matcher m=Pattern.compile("(-?\\d+(?:\\.\\d+)?)([+*/-])(-?\\d+(?:\\.\\d+)?)").matcher(x);if(!m.find()){reply("Скажите выражение, например: 125 умножить на 8.");return;}try{double a=Double.parseDouble(m.group(1)),b=Double.parseDouble(m.group(3));if("/".equals(m.group(2))&&b==0){reply("На ноль делить нельзя.");return;}double r="+".equals(m.group(2))?a+b:"-".equals(m.group(2))?a-b:"*".equals(m.group(2))?a*b:a/b;reply("Результат: "+(r==Math.rint(r)?Long.toString((long)r):String.format(Locale.US,"%.6f",r).replaceAll("0+$","" ).replaceAll("\\.$","")));}catch(Exception e){reply("Не удалось вычислить выражение.");}}
    private void toggleTorch(){if(Build.VERSION.SDK_INT<23){reply("Фонарик не поддерживается.");return;}try{CameraManager cm=(CameraManager)context.getSystemService(Context.CAMERA_SERVICE);String id=cm.getCameraIdList()[0];boolean on=prefs.getBoolean("torch",false);cm.setTorchMode(id,!on);prefs.edit().putBoolean("torch",!on).apply();reply(!on?"Фонарик включён.":"Фонарик выключен.");}catch(Exception e){reply("Не удалось управлять фонариком.");}}
    private void adjustVolume(String c){AudioManager am=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);if(c.contains("увелич")||c.contains("громче")){am.adjustVolume(AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI);reply("Громкость увеличена.");}else if(c.contains("умень")||c.contains("тише")){am.adjustVolume(AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI);reply("Громкость уменьшена.");}else reply("Скажите: громче или тише.");}
    private void launchNamedApp(String original,String normalized,String hint,String... packagesAndAnswer){
        String answer=packagesAndAnswer[packagesAndAnswer.length-1];
        if(isLocked()){reply("Сэр, чтобы открыть приложение, сначала разблокируйте телефон.");return;}
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
    private boolean launchInstalledApp(String original,String normalized){if(isLocked()){reply("Сэр, открытие приложений с экрана блокировки требует разблокировки.");return true;}try{PackageManager pm=context.getPackageManager();Intent probe=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);List<android.content.pm.ResolveInfo> apps=pm.queryIntentActivities(probe,PackageManager.MATCH_ALL);String q=normalized.replaceFirst("(?iu)^(открой|запусти|включи|открывай|запускай|зайди в)\\s+","").replaceFirst("(?iu)^приложение\\s+","").trim();if(q.isEmpty())return false;android.content.pm.ResolveInfo best=null;int score=0;for(android.content.pm.ResolveInfo ri:apps){if(ri.activityInfo==null||context.getPackageName().equals(ri.activityInfo.packageName))continue;String label=String.valueOf(ri.loadLabel(pm)).toLowerCase(new Locale("ru")).replace('ё','е');String pkg=ri.activityInfo.packageName.toLowerCase(Locale.ROOT);String qq=q.replace('ё','е');int sc=0;if(label.equals(qq))sc=120;else if(label.contains(qq)||qq.contains(label))sc=85;for(String w:qq.split("\\s+"))if(w.length()>2&&label.contains(w))sc+=18;if(pkg.contains(qq.replace(' ','.')))sc+=30;if(sc>score){score=sc;best=ri;}}if(best!=null&&score>=35){Intent launch=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(new ComponentName(best.activityInfo.packageName,best.activityInfo.name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);try{context.startActivity(launch);memory.remember("last_app",best.activityInfo.packageName);reply("Открываю "+best.loadLabel(pm)+".");return true;}catch(Throwable blocked){if(bridgeLaunch(best.activityInfo.packageName)){reply("Открываю "+best.loadLabel(pm)+".");return true;}}}}catch(Throwable ignored){}return false;}
    private void dial(String raw){String d=raw.replaceAll("[^0-9+]","");if(d.length()<5){reply("Назовите номер телефона.");return;}open(new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+d)),"Открываю набор номера.");}
    private void sms(String raw){String d=raw.replaceAll("[^0-9+]","");Intent i=new Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:"+d));i.putExtra("sms_body",raw);open(i,"Открываю сообщения.");}
    private boolean isLocked(){try{android.app.KeyguardManager km=(android.app.KeyguardManager)context.getSystemService(Context.KEYGUARD_SERVICE);return km!=null&&km.isKeyguardLocked();}catch(Throwable e){return false;}}
    private void open(Intent i,String answer){if(isLocked()){reply("Сэр, для открытия этого экрана разблокируйте телефон. Голосовые и медиакоманды продолжают работать с экрана блокировки.");return;}try{i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);context.startActivity(i);reply(answer);}catch(Throwable e){reply("Не удалось открыть системное действие.");}}
    public void shutdown(){web.shutdown();video.shutdown();}

    static final class TimerTool {private static final int TIMER_ID=78;static void start(Context c,int seconds,Callback cb){if(seconds<=0){cb.reply("Не удалось определить длительность таймера.");return;}try{AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);Intent i=new Intent(c,AlarmReceiver.class).setAction("JARVIS_TIMER");PendingIntent pi=PendingIntent.getBroadcast(c,TIMER_ID,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);long at=System.currentTimeMillis()+seconds*1000L;if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms()){c.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+c.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));cb.reply("Android требует разрешение на точные будильники. После выдачи разрешения повторите команду.");return;}if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);else am.setExact(AlarmManager.RTC_WAKEUP,at,pi);c.getSharedPreferences("jarvis_timer",Context.MODE_PRIVATE).edit().putLong("end",at).putInt("seconds",seconds).apply();cb.reply("Таймер установлен на "+format(seconds)+".");}catch(Throwable e){cb.reply("Не удалось установить таймер. Проверьте разрешение на точные будильники.");}}static void cancel(Context c,Callback cb){try{AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);Intent i=new Intent(c,AlarmReceiver.class).setAction("JARVIS_TIMER");PendingIntent pi=PendingIntent.getBroadcast(c,TIMER_ID,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);am.cancel(pi);pi.cancel();c.getSharedPreferences("jarvis_timer",Context.MODE_PRIVATE).edit().clear().apply();cb.reply("Таймер отменён.");}catch(Throwable e){cb.reply("Не удалось отменить таймер.");}}static String format(int s){if(s>=3600)return(s/3600)+" ч";if(s%60==0)return(s/60)+" мин";return s+" сек";}}
    static final class AlarmTool {static void schedule(Context c,String text,Callback cb){Matcher m=Pattern.compile("(?:на|в)\\s*(\\d{1,2})(?::(\\d{2}))?").matcher(text);if(!m.find()){cb.reply("Скажите время, например: будильник на 07:00.");return;}try{int hh=Integer.parseInt(m.group(1)),mm=m.group(2)==null?0:Integer.parseInt(m.group(2));if(hh>23||mm>59)throw new Exception();Calendar cal=Calendar.getInstance();cal.set(Calendar.HOUR_OF_DAY,hh);cal.set(Calendar.MINUTE,mm);cal.set(Calendar.SECOND,0);cal.set(Calendar.MILLISECOND,0);if(cal.before(Calendar.getInstance()))cal.add(Calendar.DAY_OF_YEAR,1);AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);Intent i=new Intent(c,AlarmReceiver.class).setAction("JARVIS_ALARM");PendingIntent pi=PendingIntent.getBroadcast(c,77,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms()){c.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+c.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));cb.reply("Откройте разрешение на точные будильники, затем повторите команду.");return;}if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);else am.setExact(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);cb.reply(String.format(Locale.getDefault(),"Будильник установлен на %02d:%02d.",hh,mm));}catch(Exception e){cb.reply("Не удалось установить будильник.");}}}
}
