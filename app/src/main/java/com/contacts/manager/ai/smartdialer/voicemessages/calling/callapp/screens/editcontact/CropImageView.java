package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.editcontact;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

public class CropImageView extends View {

    private static final float MAX_SCALE_FACTOR = 6f;

    private final Matrix matrix = new Matrix();
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF cropRect = new RectF();
    private final Path overlayPath = new Path();
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;
    private Bitmap bitmap;
    private float minScale = 1f;

    public CropImageView(Context context) {
        this(context, null);
    }

    public CropImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        overlayPaint.setColor(Color.argb(160, 0, 0, 0));
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(getResources().getDisplayMetrics().density * 2f);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float current = currentScale();
                float factor = detector.getScaleFactor();
                float target = Math.max(minScale, Math.min(minScale * MAX_SCALE_FACTOR, current * factor));
                factor = target / current;
                matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
                constrain();
                invalidate();
                return true;
            }
        });
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                matrix.postTranslate(-distanceX, -distanceY);
                constrain();
                invalidate();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                float current = currentScale();
                float target = current > minScale * 1.5f ? minScale : minScale * 2.5f;
                float factor = target / current;
                matrix.postScale(factor, factor, e.getX(), e.getY());
                constrain();
                invalidate();
                return true;
            }
        });
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        resetMatrix();
        invalidate();
    }

    public void rotate() {
        if (bitmap == null) {
            return;
        }
        Matrix rotation = new Matrix();
        rotation.postRotate(90);
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotation, true);
        if (rotated != bitmap) {
            bitmap.recycle();
        }
        setBitmap(rotated);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float size = Math.min(w, h) * 0.82f;
        float left = (w - size) / 2f;
        float top = (h - size) / 2f;
        cropRect.set(left, top, left + size, top + size);
        overlayPath.reset();
        overlayPath.setFillType(Path.FillType.EVEN_ODD);
        overlayPath.addRect(0, 0, w, h, Path.Direction.CW);
        overlayPath.addCircle(cropRect.centerX(), cropRect.centerY(), size / 2f, Path.Direction.CW);
        resetMatrix();
    }

    private void resetMatrix() {
        if (bitmap == null || cropRect.isEmpty()) {
            return;
        }
        float scale = Math.max(cropRect.width() / bitmap.getWidth(), cropRect.height() / bitmap.getHeight());
        minScale = scale;
        matrix.reset();
        matrix.postScale(scale, scale);
        float dx = cropRect.centerX() - bitmap.getWidth() * scale / 2f;
        float dy = cropRect.centerY() - bitmap.getHeight() * scale / 2f;
        matrix.postTranslate(dx, dy);
    }

    private float currentScale() {
        float[] values = new float[9];
        matrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    private void constrain() {
        if (bitmap == null) {
            return;
        }
        RectF bounds = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        matrix.mapRect(bounds);
        float dx = 0;
        float dy = 0;
        if (bounds.left > cropRect.left) {
            dx = cropRect.left - bounds.left;
        } else if (bounds.right < cropRect.right) {
            dx = cropRect.right - bounds.right;
        }
        if (bounds.top > cropRect.top) {
            dy = cropRect.top - bounds.top;
        } else if (bounds.bottom < cropRect.bottom) {
            dy = cropRect.bottom - bounds.bottom;
        }
        matrix.postTranslate(dx, dy);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, matrix, bitmapPaint);
        }
        canvas.drawPath(overlayPath, overlayPaint);
        canvas.drawCircle(cropRect.centerX(), cropRect.centerY(), cropRect.width() / 2f, borderPaint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        if (!scaleDetector.isInProgress()) {
            gestureDetector.onTouchEvent(event);
        }
        return true;
    }

    public Bitmap crop(int outputSize) {
        if (bitmap == null) {
            return null;
        }
        Matrix inverse = new Matrix();
        if (!matrix.invert(inverse)) {
            return null;
        }
        RectF source = new RectF(cropRect);
        inverse.mapRect(source);
        Bitmap output = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Matrix draw = new Matrix();
        draw.setRectToRect(source, new RectF(0, 0, outputSize, outputSize), Matrix.ScaleToFit.FILL);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bitmap, draw, bitmapPaint);
        return output;
    }
}
