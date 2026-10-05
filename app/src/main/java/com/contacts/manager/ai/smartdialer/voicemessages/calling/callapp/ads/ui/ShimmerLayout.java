package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Sweeps a soft highlight across its children's placeholder shapes while an ad loads. */
public class ShimmerLayout extends FrameLayout {

    private static final long DURATION_MS = 1300L;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();
    @Nullable
    private ValueAnimator animator;
    @Nullable
    private LinearGradient gradient;
    private float progress;

    public ShimmerLayout(@NonNull Context context) {
        this(context, null);
    }

    public ShimmerLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float band = Math.max(1f, w * 0.5f);
        gradient = new LinearGradient(0, 0, band, 0,
                new int[]{Color.TRANSPARENT, Color.argb(110, 255, 255, 255), Color.TRANSPARENT},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
        paint.setShader(gradient);
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (gradient == null || animator == null) {
            super.dispatchDraw(canvas);
            return;
        }
        int width = getWidth();
        int save = canvas.saveLayer(0, 0, width, getHeight(), null);
        super.dispatchDraw(canvas);
        float band = width * 0.5f;
        matrix.setTranslate(-band + (width + band) * progress, 0);
        gradient.setLocalMatrix(matrix);
        canvas.drawRect(0, 0, width, getHeight(), paint);
        canvas.restoreToCount(save);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (!isAttachedToWindow()) return;
        if (visibility == VISIBLE) start();
        else stop();
    }

    private void start() {
        if (animator != null) return;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(DURATION_MS);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private void stop() {
        if (animator == null) return;
        animator.cancel();
        animator = null;
    }
}
