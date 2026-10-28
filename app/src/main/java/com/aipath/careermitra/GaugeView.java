package com.aipath.careermitra;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** Animated 270-degree gauge showing the knowledge-gap percentage. */
public class GaugeView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint numPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    private float density;
    private float shown = 0f;
    private int scorePercent = 0;
    private int color = 0xFF16A34A;
    private ValueAnimator animator;

    public GaugeView(Context context) {
        super(context);
        init();
    }

    public GaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GaugeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        density = getResources().getDisplayMetrics().density;

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(0xFFE8E8EF);

        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        numPaint.setTextAlign(Paint.Align.CENTER);
        numPaint.setFakeBoldText(true);
        numPaint.setColor(0xFF1C1C1E);
        numPaint.setTextSize(46 * density);

        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setColor(0xFF6E6E73);
        labelPaint.setTextSize(14 * density);

        subPaint.setTextAlign(Paint.Align.CENTER);
        subPaint.setColor(0xFF8E8E93);
        subPaint.setTextSize(13 * density);
    }

    /** Animates the arc from 0 to gapPercent with a count-up number. */
    public void setValues(int gapPercent, int scorePercent) {
        int gap = Math.max(0, Math.min(100, gapPercent));
        this.scorePercent = scorePercent;

        if (gap < 30) color = 0xFF16A34A;        // green
        else if (gap <= 60) color = 0xFFF59E0B;  // amber
        else color = 0xFFEF4444;                 // red

        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(0f, gap);
        animator.setDuration(1200L);
        animator.setInterpolator(new DecelerateInterpolator(1.5f));
        animator.addUpdateListener(a -> {
            shown = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float stroke = 14 * density;
        float size = Math.min(w, h) - stroke * 2;
        float cx = w / 2f;
        float cy = h / 2f;

        oval.set(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f);

        trackPaint.setStrokeWidth(stroke);
        arcPaint.setStrokeWidth(stroke);
        arcPaint.setColor(color);

        canvas.drawArc(oval, 135f, 270f, false, trackPaint);
        if (shown > 0f) {
            canvas.drawArc(oval, 135f, 270f * shown / 100f, false, arcPaint);
        }

        Paint.FontMetrics fm = numPaint.getFontMetrics();
        float numY = cy - (fm.ascent + fm.descent) / 2f - 12 * density;
        canvas.drawText(Math.round(shown) + "%", cx, numY, numPaint);
        canvas.drawText("Knowledge gap", cx, cy + 20 * density, labelPaint);
        canvas.drawText("Score: " + scorePercent + "%", cx, cy + 40 * density, subPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }
}