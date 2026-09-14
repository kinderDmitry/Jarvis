package com.jarvis.homemultitool;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.voice.VoiceInteractionService;

/**
 * System assistant entry point. When the user selects JARVIS as the Android
 * assistant, Android keeps this service alive and can invoke it from the
 * assistant gesture/keyguard. We use that privileged lifecycle to start the
 * user's explicitly enabled local wake listener without pretending that a
 * normal third-party SpeechRecognizer is Google's hardware hotword engine.
 */
public class JarvisVoiceInteractionService extends VoiceInteractionService {
    @Override public void onReady() {
        super.onReady();
        startWakeIfEnabled();
    }

    private void startWakeIfEnabled(){
        if(!getSharedPreferences("jarvis",MODE_PRIVATE).getBoolean("wake_enabled",false))return;
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission("android.permission.RECORD_AUDIO")!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
        try{
            Intent i=new Intent(this,JarvisWakeWordService.class);
            if(Build.VERSION.SDK_INT>=26)startForegroundService(i); else startService(i);
        }catch(Throwable ignored){}
    }

    public static boolean isActive(Context context) {
        return VoiceInteractionService.isActiveService(context,
                new ComponentName(context, JarvisVoiceInteractionService.class));
    }

    @Override public void onLaunchVoiceAssistFromKeyguard() {
        try { showSession(new Bundle(), 0); } catch (Throwable ignored) { }
    }

    @Override public void onShutdown() {
        // The wake preference remains intact. Android will recreate this service
        // when JARVIS is selected again; the foreground listener is independently
        // responsible for its own lifecycle.
        super.onShutdown();
    }
}
