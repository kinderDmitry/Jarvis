package com.jarvis.homemultitool;

import android.content.ComponentName;
import android.media.MediaMetadata;
import android.media.Rating;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * System-wide media bridge.
 *
 * It deliberately uses Android MediaSession instead of hard-coding a single
 * music provider. Any player that exposes a media session can be controlled:
 * music, podcasts, radio, video and other media apps.
 */
public final class JarvisMediaSessionService extends NotificationListenerService {
    private static volatile JarvisMediaSessionService instance;

    @Override public void onListenerConnected(){instance=this;}
    @Override public void onListenerDisconnected(){if(instance==this)instance=null;}
    @Override public void onNotificationPosted(StatusBarNotification s){}
    @Override public void onNotificationRemoved(StatusBarNotification s){}

    public static boolean isConnected(){return instance!=null;}

    private static List<MediaController> controllers(){
        JarvisMediaSessionService s=instance;
        if(s==null)return new ArrayList<>();
        try{
            MediaSessionManager msm=(MediaSessionManager)s.getSystemService(MEDIA_SESSION_SERVICE);
            if(msm==null)return new ArrayList<>();
            List<MediaController> list=msm.getActiveSessions(new ComponentName(s,s.getClass()));
            return list==null?new ArrayList<>():new ArrayList<>(list);
        }catch(Throwable ignored){return new ArrayList<>();}
    }

    /** Prefer a playing session, then a paused session, then the first active session. */
    private static MediaController bestController(String preferredPackage){
        List<MediaController> list=controllers();
        MediaController fallback=null;
        for(MediaController c:list){
            if(c==null)continue;
            if(preferredPackage!=null&&!preferredPackage.isEmpty() &&
                    preferredPackage.equalsIgnoreCase(c.getPackageName())) return c;
            if(fallback==null)fallback=c;
            PlaybackState ps=c.getPlaybackState();
            if(ps!=null && ps.getState()==PlaybackState.STATE_PLAYING){
                if(preferredPackage==null||preferredPackage.isEmpty()) return c;
                if(c.getPackageName().toLowerCase(Locale.ROOT).contains(preferredPackage.toLowerCase(Locale.ROOT))) return c;
            }
        }
        return fallback;
    }

    public static String activePackage(){
        MediaController c=bestController("");
        return c==null?"":c.getPackageName();
    }

    public static boolean control(String command){return control(command,"");}

    /**
     * Ask the active media session to resolve and start a search request.
     * Android exposes this specifically for voice-assistant style commands.
     * An empty query means "play any music" according to the public API.
     */
    public static boolean playFromSearch(String query, String preferredPackage){
        MediaController c=bestController(preferredPackage);
        if(c==null)return false;
        try{
            MediaController.TransportControls t=c.getTransportControls();
            if(t==null)return false;
            String q=query==null?"":query.trim();
            t.playFromSearch(q,new android.os.Bundle());
            return true;
        }catch(Throwable ignored){return false;}
    }

    public static boolean playFromSearch(String query){return playFromSearch(query,"");}

    /** Controls the best active media session, regardless of the provider. */
    public static boolean control(String command,String preferredPackage){
        MediaController c=bestController(preferredPackage);
        if(c==null)return false;
        try{
            MediaController.TransportControls t=c.getTransportControls();
            if(t==null)return false;
            String a=command==null?"":command.toLowerCase(Locale.ROOT);
            if("play".equals(a)||"resume".equals(a))t.play();
            else if("pause".equals(a)||"stop".equals(a))t.pause();
            else if("next".equals(a))t.skipToNext();
            else if("previous".equals(a))t.skipToPrevious();
            else if("rewind".equals(a))t.rewind();
            else if("fast_forward".equals(a))t.fastForward();
            else return false;
            return true;
        }catch(Throwable ignored){return false;}
    }

    public static final class TrackInfo {
        public final String title,artist,album,packageName;
        TrackInfo(String t,String a,String al,String p){title=t;artist=a;album=al;packageName=p;}
    }

    public static TrackInfo currentTrack(){
        MediaController c=bestController("");
        if(c==null)return null;
        try{
            MediaMetadata m=c.getMetadata();
            if(m==null)return null;
            String t=m.getString(MediaMetadata.METADATA_KEY_TITLE);
            String a=m.getString(MediaMetadata.METADATA_KEY_ARTIST);
            String al=m.getString(MediaMetadata.METADATA_KEY_ALBUM);
            if(t==null||t.trim().isEmpty())return null;
            return new TrackInfo(t,a==null?"":a,al==null?"":al,c.getPackageName());
        }catch(Throwable ignored){return null;}
    }

    public static boolean rateCurrent(boolean like){
        MediaController c=bestController("");
        if(c==null)return false;
        try{
            PlaybackState ps=c.getPlaybackState();
            if(ps!=null && (ps.getActions() & PlaybackState.ACTION_SET_RATING)==0)return false;
            c.getTransportControls().setRating(Rating.newThumbRating(like));
            return true;
        }catch(Throwable ignored){return false;}
    }

    public static boolean setShuffle(boolean enabled, String preferredPackage){
        MediaController c=bestController(preferredPackage);
        if(c==null || android.os.Build.VERSION.SDK_INT<26)return false;
        try{
            if(android.os.Build.VERSION.SDK_INT<29)return false;
            java.lang.reflect.Method m=c.getTransportControls().getClass().getMethod("setShuffleMode", int.class);
            m.invoke(c.getTransportControls(), enabled?1:0);
            return true;
        }catch(Throwable ignored){return false;}
    }

    public static boolean setRepeat(int mode, String preferredPackage){
        MediaController c=bestController(preferredPackage);
        if(c==null || android.os.Build.VERSION.SDK_INT<26)return false;
        try{
            if(android.os.Build.VERSION.SDK_INT<29)return false;
            java.lang.reflect.Method m=c.getTransportControls().getClass().getMethod("setRepeatMode", int.class);
            m.invoke(c.getTransportControls(), mode);
            return true;
        }catch(Throwable ignored){return false;}
    }

    public static boolean seekRelative(long deltaMs, String preferredPackage){
        MediaController c=bestController(preferredPackage);
        if(c==null)return false;
        try{ PlaybackState ps=c.getPlaybackState(); long pos=ps==null?0:Math.max(0,ps.getPosition()); c.getTransportControls().seekTo(Math.max(0,pos+deltaMs)); return true; }catch(Throwable ignored){return false;}
    }

    public static boolean isPlaying(){
        MediaController c=bestController("");
        if(c==null)return false;
        try{
            PlaybackState ps=c.getPlaybackState();
            return ps!=null && ps.getState()==PlaybackState.STATE_PLAYING;
        }catch(Throwable ignored){return false;}
    }
}
