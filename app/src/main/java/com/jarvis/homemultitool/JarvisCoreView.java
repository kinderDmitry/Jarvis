package com.jarvis.homemultitool;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Lightweight neon HUD core inspired by the supplied JARVIS reference. */
public class JarvisCoreView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float t;
    private String state = "ГОТОВ";
    private ValueAnimator animator;
    public JarvisCoreView(Context c) { super(c); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
    public void setState(String s) { state = s == null ? "ГОТОВ" : s; invalidate(); }
    public void startPulse() {
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(0f,1f); animator.setDuration(2600); animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator()); animator.addUpdateListener(a->{t=(float)a.getAnimatedValue();invalidate();}); animator.start();
    }
    private void stroke(int color,float w,float glow){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(w);p.setColor(color);p.setShadowLayer(glow,0,0,color);}
    @Override protected void onDraw(Canvas c) {
        float cx=getWidth()/2f, cy=getHeight()/2f, r=Math.min(getWidth(),getHeight())*.235f;
        c.drawColor(Color.TRANSPARENT);
        // Deep radial aura
        p.setStyle(Paint.Style.FILL); p.setShader(new RadialGradient(cx,cy,r*1.8f, new int[]{0x5538A9FF,0x18206CFF,0x00000000},null,Shader.TileMode.CLAMP)); c.drawCircle(cx,cy,r*1.8f,p); p.setShader(null);
        // Technical rings
        stroke(0xFF159EFF,2.2f,16); c.drawCircle(cx,cy,r,p); p.clearShadowLayer();
        stroke(0xFF0A5FAE,1.2f,5); c.drawCircle(cx,cy,r+22,p); c.drawCircle(cx,cy,r+42,p);
        stroke(0xFF28C7FF,1.8f,10);
        RectF oval=new RectF(cx-r-10,cy-r-10,cx+r+10,cy+r+10); c.drawArc(oval,(t*360)-35,115,false,p); c.drawArc(oval,(t*360)+145,70,false,p);
        stroke(0xFF0B73D0,1.2f,4);
        for(int i=0;i<12;i++){double a=(Math.PI*2*i/12.0)+t*.55;float x1=cx+(r+50)*(float)Math.cos(a),y1=cy+(r+50)*(float)Math.sin(a);float x2=cx+(r+63)*(float)Math.cos(a),y2=cy+(r+63)*(float)Math.sin(a);c.drawLine(x1,y1,x2,y2,p);}
        // Minimal helmet/face emblem
        p.setStyle(Paint.Style.FILL);p.setColor(0xFF020812);p.setShadowLayer(18,0,0,0xFF168FFF);
        Path face=new Path(); face.moveTo(cx-r*.58f,cy-r*.32f);face.lineTo(cx-r*.34f,cy-r*.63f);face.lineTo(cx+r*.34f,cy-r*.63f);face.lineTo(cx+r*.58f,cy-r*.32f);face.lineTo(cx+r*.50f,cy+r*.46f);face.lineTo(cx,cy+r*.70f);face.lineTo(cx-r*.50f,cy+r*.46f);face.close();c.drawPath(face,p);p.clearShadowLayer();
        stroke(0xFF168FFF,2.0f,10);c.drawPath(face,p);p.clearShadowLayer();
        stroke(0xFFB8ECFF,2.5f,12);Path eyes=new Path();eyes.moveTo(cx-r*.40f,cy-r*.05f);eyes.lineTo(cx-r*.08f,cy+r*.02f);eyes.moveTo(cx+r*.40f,cy-r*.05f);eyes.lineTo(cx+r*.08f,cy+r*.02f);c.drawPath(eyes,p);p.clearShadowLayer();
        // Orbit ticks
        p.setStyle(Paint.Style.FILL);p.setColor(0xFF56D9FF);for(int i=0;i<6;i++){double a=(Math.PI*2*i/6.0)-t*.8;float x=cx+(r+32)*(float)Math.cos(a),y=cy+(r+32)*(float)Math.sin(a);c.drawCircle(x,y,2.4f,p);}
    }
}
