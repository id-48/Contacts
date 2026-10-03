package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;

import java.util.ArrayList;
import java.util.List;

public class AlphabetIndexView extends View {

    public interface OnLetterListener {
        void onLetter(String letter);
    }

    private final List<String> letters = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int normalColor;
    private final int selectedColor;
    private final float maxTextSize;
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
        normalColor = ContextCompat.getColor(context, R.color.text_secondary);
        selectedColor = ContextCompat.getColor(context, R.color.primary);
        maxTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics());
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(ResourcesCompat.getFont(context, R.font.roboto_medium));
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
        paint.setTextSize(Math.min(maxTextSize, itemHeight * 0.75f));
        float centerX = getWidth() / 2f;
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baselineOffset = (itemHeight - metrics.ascent - metrics.descent) / 2f;
        for (int i = 0; i < letters.size(); i++) {
            String letter = letters.get(i);
            paint.setColor(letter.equals(selected) ? selectedColor : normalColor);
            canvas.drawText(letter, centerX, getPaddingTop() + i * itemHeight + baselineOffset, paint);
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
