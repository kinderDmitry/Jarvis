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

/** Animated JARVIS HUD core. Pure Canvas, no placeholder bitmap. */
public final class JarvisCoreView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase;
    private String state = "ГОТОВ";
    private ValueAnimator animator;

    public JarvisCoreView(Context context) { super(context); setFocusable(true); }
    public void setState(String value) { state = value == null ? "ГОТОВ" : value; invalidate(); }

    public void startPulse() {
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(3200); animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> { phase = (float)a.getAnimatedValue(); invalidate(); });
        animator.start();
    }

    private void stroke(Canvas c, int color, float width) {
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(width); p.setColor(color); p.setShader(null);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float cx = getWidth()/2f, cy = getHeight()*.46f;
        float r = Math.min(getWidth(), getHeight())*.23f;
        float activity = state.contains("СЛУША") || state.contains("ИЩУ") || state.contains("ОБРАБОТ") || state.contains("ОТВЕЧ") ? 1.25f : 1f;

        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(cx, cy, r*2.6f,
                new int[]{0x7047D9FF,0x1C168BFF,0x00000000},null,Shader.TileMode.CLAMP));
        c.drawCircle(cx,cy,r*2.6f,p); p.setShader(null);

        stroke(c,0xFF155B91,1.2f); c.drawCircle(cx,cy,r+30,p); c.drawCircle(cx,cy,r+51,p);
        stroke(c,0xFF27C8FF,2f);
        RectF ring=new RectF(cx-r-13,cy-r-13,cx+r+13,cy+r+13);
        float sweep=phase*360f;
        c.drawArc(ring,sweep-48,105,false,p); c.drawArc(ring,sweep+142,64,false,p);
        stroke(c,0xFF0C73BC,1f);
        RectF outer=new RectF(cx-r-42,cy-r-42,cx+r+42,cy+r+42);
        c.drawArc(outer,-sweep,76,false,p); c.drawArc(outer,180-sweep,44,false,p);

        p.setStyle(Paint.Style.FILL); p.setColor(0xFF49D9FF);
        for(int i=0;i<20;i++){
            double a=(Math.PI*2*i/20d)+phase*.65;
            float rr=r+59;
            float x=cx+rr*(float)Math.cos(a), y=cy+rr*(float)Math.sin(a);
            float size=(i%5==0?3.5f:1.7f)*activity; c.drawCircle(x,y,size,p);
        }

        Path face=new Path();
        face.moveTo(cx-r*.62f,cy-r*.24f); face.lineTo(cx-r*.38f,cy-r*.58f);
        face.lineTo(cx+r*.38f,cy-r*.58f); face.lineTo(cx+r*.62f,cy-r*.24f);
        face.lineTo(cx+r*.52f,cy+r*.48f); face.lineTo(cx,cy+r*.72f); face.lineTo(cx-r*.52f,cy+r*.48f); face.close();
        p.setStyle(Paint.Style.FILL); p.setColor(0xFF020914); c.drawPath(face,p);
        stroke(c,0xFF19B8FF,2.1f); c.drawPath(face,p);

        int eye=state.contains("СЛУША")?0xFFB5F7FF:0xFF55D9FF;
        stroke(c,eye,3f); Path eyes=new Path();
        eyes.moveTo(cx-r*.40f,cy-r*.03f); eyes.lineTo(cx-r*.10f,cy+r*.03f);
        eyes.moveTo(cx+r*.40f,cy-r*.03f); eyes.lineTo(cx+r*.10f,cy+r*.03f); c.drawPath(eyes,p);

        float baseY=cy+r+88;
        p.setStyle(Paint.Style.FILL); p.setColor(0xFF39CFFF);
        for(int i=0;i<27;i++){
            float x=cx-108+i*8, amp=(4+(float)Math.abs(Math.sin(phase*Math.PI*2+i*.55))*16)*activity;
            c.drawRoundRect(x,baseY-amp,x+4,baseY+amp,2,2,p);
        }
    }

    @Override protected void onDetachedFromWindow(){if(animator!=null)animator.cancel();super.onDetachedFromWindow();}
}
