package com.jarvis.homemultitool;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.service.voice.VoiceInteractionService;

/** Lightweight system entry point. Heavy UI work is delegated to the session/activity. */
public class JarvisVoiceInteractionService extends VoiceInteractionService {
    @Override public void onReady() { super.onReady(); }

    public static boolean isActive(Context context) {
        return VoiceInteractionService.isActiveService(context,
                new ComponentName(context, JarvisVoiceInteractionService.class));
    }

    @Override public void onLaunchVoiceAssistFromKeyguard() {
        try {
            showSession(new Bundle(), 0);
        } catch (Throwable ignored) {
            // Some OEMs do not allow session launch from this entry point; the session/activity path remains available.
        }
    }
}
