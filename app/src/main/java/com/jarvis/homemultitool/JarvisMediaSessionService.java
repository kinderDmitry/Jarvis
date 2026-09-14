package com.jarvis.homemultitool;

import android.content.ComponentName;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.List;

/** Gives JARVIS access to active media sessions after the user grants notification-listener access. */
public final class JarvisMediaSessionService extends NotificationListenerService {
    private static JarvisMediaSessionService instance;
    @Override public void onListenerConnected(){instance=this;}
    @Override public void onListenerDisconnected(){if(instance==this)instance=null;}
    @Override public void onNotificationPosted(StatusBarNotification s){}
    @Override public void onNotificationRemoved(StatusBarNotification s){}

    public static boolean isConnected(){return instance!=null;}
    public static boolean control(String command){
        JarvisMediaSessionService s=instance;
        if(s==null)return false;
        try{
            MediaSessionManager msm=(MediaSessionManager)s.getSystemService(MEDIA_SESSION_SERVICE);
            List<MediaController> list=msm.getActiveSessions(new ComponentName(s,s.getClass()));
            if(list==null||list.isEmpty())return false;
            for(MediaController c:list){
                MediaController.TransportControls t=c.getTransportControls();
                if("play".equals(command))t.play();
                else if("pause".equals(command))t.pause();
                else if("next".equals(command))t.skipToNext();
                else if("previous".equals(command))t.skipToPrevious();
                else return false;
                return true;
            }
        }catch(Throwable ignored){}
        return false;
    }
}
