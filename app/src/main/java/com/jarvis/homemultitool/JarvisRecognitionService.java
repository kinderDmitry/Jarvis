package com.jarvis.homemultitool;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionService;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;

/**
 * RecognitionService required by the Android assistant-role contract.
 * It prefers Android's on-device recognizer and forwards its callbacks. This keeps
 * JARVIS eligible for the system assistant role without requiring a cloud API.
 */
public class JarvisRecognitionService extends RecognitionService {
    private SpeechRecognizer delegate;

    @Override
    protected void onStartListening(Intent intent, Callback callback) {
        destroyDelegate();
        try {
            if (android.os.Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
                delegate = SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
            } else {
                delegate = createFallbackRecognizer();
            }
            if (delegate == null) { callback.error(SpeechRecognizer.ERROR_CLIENT); return; }
            delegate.setRecognitionListener(new RecognitionListenerBridge(callback));
            delegate.startListening(intent != null ? intent : new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH));
        } catch (Throwable t) {
            callback.error(SpeechRecognizer.ERROR_CLIENT);
            destroyDelegate();
        }
    }

    private SpeechRecognizer createFallbackRecognizer() {
        try {
            android.content.pm.PackageManager pm = getPackageManager();
            Intent probe = new Intent(RecognitionService.SERVICE_INTERFACE);
            java.util.List<android.content.pm.ResolveInfo> services = pm.queryIntentServices(probe, android.content.pm.PackageManager.GET_META_DATA);
            for (android.content.pm.ResolveInfo ri : services) {
                if (ri.serviceInfo == null || getPackageName().equals(ri.serviceInfo.packageName)) continue;
                return SpeechRecognizer.createSpeechRecognizer(this,
                        new ComponentName(ri.serviceInfo.packageName, ri.serviceInfo.name));
            }
        } catch (Throwable ignored) { }
        return null;
    }

    @Override protected void onStopListening(Callback callback) {
        if (delegate != null) { try { delegate.stopListening(); } catch (Throwable ignored) { } }
    }

    @Override protected void onCancel(Callback callback) {
        destroyDelegate();
    }

    private void destroyDelegate() {
        if (delegate != null) { try { delegate.cancel(); } catch (Throwable ignored) { } try { delegate.destroy(); } catch (Throwable ignored) { } delegate = null; }
    }

    private static final class RecognitionListenerBridge implements android.speech.RecognitionListener {
        private final Callback cb;
        RecognitionListenerBridge(Callback callback) { cb = callback; }
        public void onReadyForSpeech(Bundle p) { cb.readyForSpeech(p); }
        public void onBeginningOfSpeech() { cb.beginningOfSpeech(); }
        public void onRmsChanged(float r) { cb.rmsChanged(r); }
        public void onBufferReceived(byte[] b) { cb.bufferReceived(b); }
        public void onEndOfSpeech() { cb.endOfSpeech(); }
        public void onError(int e) { cb.error(e); }
        public void onResults(Bundle b) { cb.results(b); }
        public void onPartialResults(Bundle b) { cb.partialResults(b); }
    }
}
