package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemDialKeyBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;

public class DialPadView extends LinearLayout {

    public interface Listener {
        void onKey(char key);

        default boolean onKeyLongPress(char key) {
            return false;
        }
    }

    private static final char[] KEYS = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '0', '#'};
    private static final String[] LETTERS = {"", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ", "", "+", ""};

    private Listener listener;

    public DialPadView(Context context) {
        this(context, null);
    }

    public DialPadView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        LayoutInflater inflater = LayoutInflater.from(context);
        int gap = getResources().getDimensionPixelSize(R.dimen.space_12);
        int horizontalGap = getResources().getDimensionPixelSize(R.dimen.space_24);
        for (int row = 0; row < 4; row++) {
            LinearLayout rowLayout = new LinearLayout(context);
            rowLayout.setOrientation(HORIZONTAL);
            rowLayout.setGravity(Gravity.CENTER);
            LayoutParams rowParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            if (row > 0) {
                rowParams.topMargin = gap;
            }
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                ItemDialKeyBinding key = ItemDialKeyBinding.inflate(inflater, rowLayout, false);
                bindKey(key, index);
                LayoutParams keyParams = (LayoutParams) key.getRoot().getLayoutParams();
                if (col > 0) {
                    keyParams.setMarginStart(horizontalGap);
                }
                rowLayout.addView(key.getRoot(), keyParams);
            }
            addView(rowLayout, rowParams);
        }
    }

    private void bindKey(ItemDialKeyBinding key, int index) {
        char digit = KEYS[index];
        key.keyDigit.setText(String.valueOf(digit));
        String letters = LETTERS[index];
        if (letters.isEmpty()) {
            key.keyLetters.setText(" ");
            key.keyLetters.setVisibility(digit == '1' ? View.INVISIBLE : View.GONE);
        } else {
            key.keyLetters.setText(letters);
        }
        key.getRoot().setContentDescription(letters.isEmpty() || digit == '0'
                ? String.valueOf(digit) : digit + " " + letters);
        key.getRoot().setOnClickListener(v -> {
            HapticUtils.tap(v);
            if (listener != null) {
                listener.onKey(digit);
            }
        });
        key.getRoot().setOnLongClickListener(v -> {
            if (listener != null && listener.onKeyLongPress(digit)) {
                HapticUtils.longPress(v);
                return true;
            }
            return false;
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }
}
