package com.jarvis.homemultitool.agent;

import android.content.Context;

/** Metadata contract for an executable JARVIS capability. Execution remains permission-aware. */
public interface JarvisTool {
    String name();
    String description();
    boolean requiresConfirmation();
    boolean isAvailable(Context context);
}
