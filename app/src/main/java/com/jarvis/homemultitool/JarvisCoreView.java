package com.jarvis.homemultitool;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** Premium reactive JARVIS core. Custom-drawn to remain sharp at any screen density. */
public final class JarvisCoreView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private float phase; private String state="ГОТОВ"; private ValueAnimator animator;
    public JarvisCoreView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);setFocusable(true);}
    public void setState(String v){state=v==null?"ГОТОВ":v.toUpperCase();invalidate();}
    public void startPulse(){if(animator!=null)animator.cancel();animator=ValueAnimator.ofFloat(0f,1f);animator.setDuration(3600);animator.setRepeatCount(ValueAnimator.INFINITE);animator.setInterpolator(new DecelerateInterpolator());animator.addUpdateListener(a->{phase=(float)a.getAnimatedValue();invalidate();});animator.start();}
    private void stroke(int c,float w){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(w);p.setColor(c);p.setShader(null);p.setStrokeCap(Paint.Cap.ROUND);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float cx=getWidth()/2f,cy=getHeight()*.45f;float r=Math.min(getWidth(),getHeight())*.215f;boolean active=state.contains("СЛУША")||state.contains("ОБРАБОТ")||state.contains("ОТВЕЧ")||state.contains("ИЩУ");float pulse=1f+(float)Math.sin(phase*Math.PI*2)*.035f;
        p.setStyle(Paint.Style.FILL);p.setShader(new RadialGradient(cx,cy,r*2.7f,new int[]{active?0x886DEBFF:0x5A3FC8FF,0x1D1788CC,0x00000000},null,Shader.TileMode.CLAMP));c.drawCircle(cx,cy,r*2.7f,p);p.setShader(null);
        stroke(0xFF153D5C,1.1f);c.drawCircle(cx,cy,r+35,p);c.drawCircle(cx,cy,r+53,p);c.drawCircle(cx,cy,r+72,p);
        stroke(active?0xFFB9F7FF:0xFF54D8FF,2.4f);RectF rr=new RectF(cx-r-14,cy-r-14,cx+r+14,cy+r+14);float sweep=phase*360;c.drawArc(rr,sweep-60,92,false,p);c.drawArc(rr,sweep+125,48,false,p);
        stroke(0xFF1B83BC,1.2f);RectF ro=new RectF(cx-r-43,cy-r-43,cx+r+43,cy+r+43);c.drawArc(ro,-sweep,70,false,p);c.drawArc(ro,180-sweep,38,false,p);
        p.setStyle(Paint.Style.FILL);p.setColor(0xFF6DE7FF);for(int i=0;i<24;i++){double a=Math.PI*2*i/24d+phase*.7;float rad=r+63;float x=cx+rad*(float)Math.cos(a),y=cy+rad*(float)Math.sin(a);float sz=(i%6==0?3.2f:1.5f)*(active?1.35f:1f);c.drawCircle(x,y,sz,p);}
        Path face=new Path();float q=r*pulse;face.moveTo(cx-q*.66f,cy-q*.24f);face.lineTo(cx-q*.42f,cy-q*.58f);face.lineTo(cx+q*.42f,cy-q*.58f);face.lineTo(cx+q*.66f,cy-q*.24f);face.lineTo(cx+q*.53f,cy+q*.48f);face.lineTo(cx,cy+q*.73f);face.lineTo(cx-q*.53f,cy+q*.48f);face.close();p.setStyle(Paint.Style.FILL);p.setColor(0xFF020914);p.setShadowLayer(18,0,0,0xAA3DDCFF);c.drawPath(face,p);p.clearShadowLayer();stroke(0xFF35C9FF,2.2f);c.drawPath(face,p);
        int eye=active?0xFFE7FCFF:0xFF66DFFF;stroke(eye,3.2f);Path eyes=new Path();eyes.moveTo(cx-q*.41f,cy-q*.04f);eyes.lineTo(cx-q*.10f,cy+q*.02f);eyes.moveTo(cx+q*.41f,cy-q*.04f);eyes.lineTo(cx+q*.10f,cy+q*.02f);c.drawPath(eyes,p);
        float base=cy+r+92;for(int i=0;i<31;i++){float x=cx-124+i*8,amp=(3.5f+(float)Math.abs(Math.sin(phase*Math.PI*2+i*.47))*14)*(active?1.35f:.85f);p.setStyle(Paint.Style.FILL);p.setColor(0xFF43D6FF);c.drawRoundRect(x,base-amp,x+3.5f,base+amp,2,2,p);}
    }
    @Override protected void onDetachedFromWindow(){if(animator!=null)animator.cancel();super.onDetachedFromWindow();}
}
