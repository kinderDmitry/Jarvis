package com.jarvis.homemultitool;

import android.content.Intent;
import android.os.Bundle;
import android.service.voice.VoiceInteractionService;
import android.view.WindowManager;

/** System-level assistant entry point. Android keeps the selected assistant service alive. */
public class JarvisVoiceInteractionService extends VoiceInteractionService {
    @Override public void onReady() {
        super.onReady();
        try { setInvocationEffectEnabled(true); } catch (Throwable ignored) {}
    }

    @Override public void onLaunchVoiceAssistFromKeyguard() {
        Intent i = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        i.putExtra("LOCKSCREEN_ASSIST", true);
        i.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        startActivity(i);
    }
}
