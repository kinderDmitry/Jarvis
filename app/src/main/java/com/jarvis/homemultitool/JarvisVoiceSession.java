package com.jarvis.homemultitool;

import android.content.Context;
import android.content.Intent;
import android.service.voice.VoiceInteractionSession;
import android.view.WindowManager;

public class JarvisVoiceSession extends VoiceInteractionSession {
    public JarvisVoiceSession(Context context) { super(context); }

    @Override public void onShow(android.os.Bundle args, int showFlags) {
        super.onShow(args, showFlags);
        Intent i = new Intent(getContext(), MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        i.putExtra("LOCKSCREEN_ASSIST", true);
        i.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getContext().startActivity(i);
        hide();
    }
}
