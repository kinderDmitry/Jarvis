package com.jarvis.homemultitool;
import android.content.*;
import android.media.*;
import android.app.*;
public class AlarmReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c, Intent i){
  NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
  if(android.os.Build.VERSION.SDK_INT>=26) nm.createNotificationChannel(new NotificationChannel("jarvis_alarm","JARVIS Alarm",NotificationManager.IMPORTANCE_HIGH));
  Notification.Builder b=android.os.Build.VERSION.SDK_INT>=26?new Notification.Builder(c,"jarvis_alarm"):new Notification.Builder(c);
  b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("JARVIS").setContentText("Сэр, время будильника.").setAutoCancel(true);
  nm.notify(77,b.build());
  try{((AudioManager)c.getSystemService(Context.AUDIO_SERVICE)).adjustVolume(AudioManager.ADJUST_RAISE,AudioManager.FLAG_PLAY_SOUND);}catch(Exception ignored){}
 }
}
