package com.jarvis.homemultitool;

import android.app.*;
import android.content.*;
import android.os.Build;

/** Restores the wake preference where Android permits microphone foreground-service startup after boot. */
public final class JarvisBootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if(intent==null||!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))return;
        // Restore persistent interval timers after reboot.
        JarvisEngine.IntervalTimerTool.rearm(context);
        if(!context.getSharedPreferences("jarvis",0).getBoolean("wake_enabled",false))return;
        // Android 14+ restricts starting microphone foreground services directly from BOOT_COMPLETED.
        // Do not fake success or crash: the user can re-enable the service from JARVIS settings.
        if(Build.VERSION.SDK_INT>=34)return;
        try{Intent i=new Intent(context,JarvisWakeWordService.class);if(Build.VERSION.SDK_INT>=26)context.startForegroundService(i);else context.startService(i);}catch(Throwable ignored){}
    }
}
