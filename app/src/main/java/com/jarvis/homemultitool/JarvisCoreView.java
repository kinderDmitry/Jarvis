package com.jarvis.homemultitool;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class JarvisCoreView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float pulse = 0f;
    private ValueAnimator animator;
    public JarvisCoreView(Context c) { super(c); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
    public void startPulse() {
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(0f, 1f); animator.setDuration(1800); animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new DecelerateInterpolator()); animator.addUpdateListener(a -> { pulse=(float)a.getAnimatedValue(); invalidate(); }); animator.start();
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); float cx=getWidth()/2f, cy=getHeight()/2f, base=Math.min(getWidth(),getHeight())*.24f;
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2f); p.setColor(Color.rgb(70,150,255)); p.setShadowLayer(24,0,0,Color.rgb(30,120,255));
        c.drawCircle(cx,cy,base, p); p.clearShadowLayer();
        p.setStrokeWidth(1.5f); p.setColor(Color.rgb(35,90,145));
        c.drawCircle(cx,cy,base+18+10*pulse,p); c.drawCircle(cx,cy,base+34+18*(1-pulse),p);
        p.setStyle(Paint.Style.FILL); p.setColor(Color.WHITE); p.setShadowLayer(18,0,0,Color.rgb(80,170,255)); c.drawCircle(cx,cy,5+4*pulse,p); p.clearShadowLayer();
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1f); p.setColor(Color.rgb(80,160,255));
        for(int i=0;i<8;i++){ double a=(Math.PI*2*i/8.0)+pulse*.8; float x1=cx+(base+48)*((float)Math.cos(a)); float y1=cy+(base+48)*((float)Math.sin(a)); float x2=cx+(base+62)*((float)Math.cos(a)); float y2=cy+(base+62)*((float)Math.sin(a)); c.drawLine(x1,y1,x2,y2,p); }
    }
}
