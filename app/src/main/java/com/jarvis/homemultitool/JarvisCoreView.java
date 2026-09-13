package com.jarvis.homemultitool;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Lightweight hardware-accelerated JARVIS core. No bitmap assets and no software blur. */
public final class JarvisCoreView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase;
    private String state = "ГОТОВ";
    private ValueAnimator animator;

    public JarvisCoreView(Context context) {
        super(context);
        setFocusable(true);
    }

    public void setState(String value) {
        state = value == null ? "ГОТОВ" : value;
        invalidate();
    }

    public void startPulse() {
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2600);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> { phase = (float) a.getAnimatedValue(); invalidate(); });
        animator.start();
    }

    private void stroke(Canvas c, int color, float width) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(width);
        p.setColor(color);
        p.setShader(null);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float cx = getWidth() / 2f;
        float cy = getHeight() * .46f;
        float r = Math.min(getWidth(), getHeight()) * .205f;

        // Soft aura made from transparent circles: cheaper than software shadow blur.
        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(cx, cy, r * 2.25f,
                new int[]{0x6636C8FF, 0x183A8CFF, 0x00000000}, null, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r * 2.25f, p);
        p.setShader(null);

        // Thin HUD rings.
        stroke(c, 0xFF117BCB, 1.2f);
        c.drawCircle(cx, cy, r + 32, p);
        c.drawCircle(cx, cy, r + 52, p);
        stroke(c, 0xFF23BFFF, 2.0f);
        RectF ring = new RectF(cx-r-14, cy-r-14, cx+r+14, cy+r+14);
        float sweep = phase * 360f;
        c.drawArc(ring, sweep-40, 95, false, p);
        c.drawArc(ring, sweep+145, 62, false, p);
        stroke(c, 0xFF0B63AA, 1.0f);
        RectF outer = new RectF(cx-r-43, cy-r-43, cx+r+43, cy+r+43);
        c.drawArc(outer, -phase*360f, 70, false, p);
        c.drawArc(outer, 180-phase*360f, 42, false, p);

        // Rotating technical ticks.
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF47D8FF);
        for (int i=0; i<16; i++) {
            double a = (Math.PI*2*i/16.0) + phase*.9;
            float x = cx + (r+62)*(float)Math.cos(a);
            float y = cy + (r+62)*(float)Math.sin(a);
            float size = (i%4==0) ? 4f : 2f;
            c.drawCircle(x, y, size, p);
        }

        // JARVIS face mark.
        Path face = new Path();
        face.moveTo(cx-r*.62f, cy-r*.24f);
        face.lineTo(cx-r*.38f, cy-r*.58f);
        face.lineTo(cx+r*.38f, cy-r*.58f);
        face.lineTo(cx+r*.62f, cy-r*.24f);
        face.lineTo(cx+r*.52f, cy+r*.48f);
        face.lineTo(cx, cy+r*.72f);
        face.lineTo(cx-r*.52f, cy+r*.48f);
        face.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF020913);
        c.drawPath(face, p);
        stroke(c, 0xFF149FFF, 2.0f);
        c.drawPath(face, p);

        // Eyes react subtly to state.
        int eye = state.contains("СЛУША") ? 0xFF8AF2FF : 0xFF49CFFF;
        stroke(c, eye, 3.0f);
        Path eyes = new Path();
        eyes.moveTo(cx-r*.40f, cy-r*.03f);
        eyes.lineTo(cx-r*.10f, cy+r*.03f);
        eyes.moveTo(cx+r*.40f, cy-r*.03f);
        eyes.lineTo(cx+r*.10f, cy+r*.03f);
        c.drawPath(eyes, p);

        // Small animated waveform beneath the core.
        float baseY = cy + r + 104;
        p.setStyle(Paint.Style.FILL);
        for (int i=0; i<25; i++) {
            float x = cx - 96 + i*8;
            float amp = 4 + (float)Math.abs(Math.sin(phase*Math.PI*2 + i*.58))*18;
            if (state.contains("СЛУША") || state.contains("ГОВОР")) amp *= 1.35f;
            c.drawRoundRect(x, baseY-amp, x+4, baseY+amp, 2, 2, p);
        }
    }

    @Override protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }
}
