package com.jarvis.homemultitool;
import android.app.*;import android.content.*;import android.os.*;
public class AlarmReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){
  String action=i==null?"":i.getAction();
  NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
  if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel("jarvis_alarm","JARVIS",NotificationManager.IMPORTANCE_HIGH));
  String text="Сэр, время вышло.";
  if("JARVIS_ALARM".equals(action)) text="Сэр, время будильника.";
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,"jarvis_alarm"):new Notification.Builder(c);
  b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("JARVIS").setContentText(text).setAutoCancel(true).setPriority(Notification.PRIORITY_HIGH);
  nm.notify("JARVIS_TIMER".equals(action)?78:77,b.build());
 }
}
