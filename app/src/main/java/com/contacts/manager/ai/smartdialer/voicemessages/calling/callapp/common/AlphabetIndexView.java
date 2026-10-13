package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AlphabetIndexView extends View {

    public interface OnLetterListener {
        void onLetter(String letter);
    }

    private final List<String> letters = new ArrayList<>();
    private final Set<String> available = new HashSet<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track = new RectF();
    private final int activeColor;
    private final int fadedColor;
    private final int selectedTextColor;
    private final float maxTextSize;
    private final float trackInset;
    private String selected;
    private OnLetterListener listener;

    public AlphabetIndexView(Context context) {
        this(context, null);
    }

    public AlphabetIndexView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        letters.add("#");
        for (char c = 'A'; c <= 'Z'; c++) {
            letters.add(String.valueOf(c));
        }
        activeColor = ContextCompat.getColor(context, R.color.primary);
        fadedColor = ColorUtils.setAlphaComponent(activeColor, 0x55);
        selectedTextColor = ContextCompat.getColor(context, R.color.on_primary);
        trackPaint.setColor(ColorUtils.setAlphaComponent(activeColor, 0x12));
        selectedPaint.setColor(activeColor);
        maxTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13, getResources().getDisplayMetrics());
        trackInset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(ResourcesCompat.getFont(context, R.font.roboto_bold));
        setContentDescription(context.getString(R.string.alphabet_index));
    }

    public void setOnLetterListener(OnLetterListener listener) {
        this.listener = listener;
    }

    public void setSelected(String letter) {
        if (letter != null && !letter.equals(selected)) {
            selected = letter;
            invalidate();
        }
    }

    public void setAvailableLetters(Collection<String> letters) {
        if (available.size() == letters.size() && available.containsAll(letters)) {
            return;
        }
        available.clear();
        available.addAll(letters);
        invalidate();
    }

    private float itemHeight() {
        return (getHeight() - getPaddingTop() - getPaddingBottom()) / (float) letters.size();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float itemHeight = itemHeight();
        if (itemHeight <= 0) {
            return;
        }
        float width = getWidth();
        track.set(0f, Math.max(0f, getPaddingTop() - trackInset), width,
                Math.min(getHeight(), getHeight() - getPaddingBottom() + trackInset));
        canvas.drawRoundRect(track, width / 2f, width / 2f, trackPaint);

        paint.setTextSize(Math.min(maxTextSize, itemHeight * 0.78f));
        float centerX = width / 2f;
        float radius = Math.min(itemHeight, width) / 2f;
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baselineOffset = (itemHeight - metrics.ascent - metrics.descent) / 2f;
        for (int i = 0; i < letters.size(); i++) {
            String letter = letters.get(i);
            float top = getPaddingTop() + i * itemHeight;
            if (letter.equals(selected)) {
                canvas.drawCircle(centerX, top + itemHeight / 2f, radius, selectedPaint);
                paint.setColor(selectedTextColor);
            } else {
                paint.setColor(available.isEmpty() || available.contains(letter) ? activeColor : fadedColor);
            }
            canvas.drawText(letter, centerX, top + baselineOffset, paint);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                getParent().requestDisallowInterceptTouchEvent(true);
                float itemHeight = itemHeight();
                if (itemHeight <= 0) {
                    return true;
                }
                int index = (int) ((event.getY() - getPaddingTop()) / itemHeight);
                index = Math.max(0, Math.min(letters.size() - 1, index));
                String letter = letters.get(index);
                if (!letter.equals(selected)) {
                    selected = letter;
                    invalidate();
                    HapticUtils.tick(this);
                    if (listener != null) {
                        listener.onLetter(letter);
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
                performClick();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public List<String> getLetters() {
        return letters;
    }
}
