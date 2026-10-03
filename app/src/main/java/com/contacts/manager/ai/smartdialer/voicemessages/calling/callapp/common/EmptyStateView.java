package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ViewEmptyStateBinding;

public class EmptyStateView extends LinearLayout {

    private final ViewEmptyStateBinding binding;

    public EmptyStateView(Context context) {
        this(context, null);
    }

    public EmptyStateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        int padding = getResources().getDimensionPixelSize(R.dimen.space_32);
        setPadding(padding, padding, padding, padding);
        binding = ViewEmptyStateBinding.inflate(LayoutInflater.from(context), this);
    }

    public void setContent(@DrawableRes int icon, @StringRes int title, @StringRes int body) {
        binding.emptyIcon.setImageResource(icon);
        binding.emptyTitle.setText(title);
        binding.emptyBody.setText(body);
        binding.emptyAction.setVisibility(GONE);
    }

    public void setAction(@StringRes int text, View.OnClickListener listener) {
        binding.emptyAction.setText(text);
        binding.emptyAction.setOnClickListener(listener);
        binding.emptyAction.setVisibility(VISIBLE);
    }

    public void show(boolean visible) {
        if (visible == (getVisibility() == VISIBLE)) {
            return;
        }
        if (visible) {
            setAlpha(0f);
            setTranslationY(getResources().getDimension(R.dimen.space_16));
            setVisibility(VISIBLE);
            animate().alpha(1f).translationY(0f).setDuration(260).start();
        } else {
            animate().cancel();
            setVisibility(GONE);
        }
    }
}
