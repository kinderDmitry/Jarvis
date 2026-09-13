package com.jarvis.homemultitool;
import android.app.*;import android.content.*;import android.media.RingtoneManager;import android.net.Uri;import android.os.*;
public class AlarmReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){
  String action=i==null?"":i.getAction(); NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
  if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel("jarvis_alarm","JARVIS • таймеры",NotificationManager.IMPORTANCE_HIGH);ch.setDescription("Системные таймеры и будильники JARVIS");ch.enableVibration(true);nm.createNotificationChannel(ch);}
  boolean timer="JARVIS_TIMER".equals(action);String text=timer?"Сэр, время таймера истекло.":"Сэр, время будильника.";
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,"jarvis_alarm"):new Notification.Builder(c);
  Uri sound=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
  b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("JARVIS").setContentText(text).setAutoCancel(true).setPriority(Notification.PRIORITY_HIGH).setSound(sound).setVibrate(new long[]{0,500,250,500,250,800});
  nm.notify(timer?78:77,b.build());
  if(timer)c.getSharedPreferences("jarvis_timer",Context.MODE_PRIVATE).edit().clear().apply();
 }
}
