package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;

public final class Accent {

    private Accent() {
    }

    public static void icon(ImageView view) {
        Context context = view.getContext();
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(context, 12));
        shape.setColor(ContextCompat.getColor(context, R.color.primary_container));
        view.setBackground(shape);
        view.setBackgroundTintList(null);
        view.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary)));
    }

    public static void soft(View view, @ColorInt int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(ColorUtils.setAlphaComponent(color, 0x1F));
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, 0x40)),
                shape, null));
    }

    private static float dp(Context context, float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics());
    }
}
