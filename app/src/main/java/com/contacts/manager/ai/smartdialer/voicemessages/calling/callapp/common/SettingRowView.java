package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ViewSettingRowBinding;

public class SettingRowView extends LinearLayout {

    public interface OnCheckedListener {
        void onChecked(boolean checked);
    }

    private final ViewSettingRowBinding binding;
    private OnCheckedListener checkedListener;

    public SettingRowView(Context context) {
        this(context, null);
    }

    public SettingRowView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.setting_row_height));
        int horizontal = getResources().getDimensionPixelSize(R.dimen.space_16);
        int vertical = getResources().getDimensionPixelSize(R.dimen.space_12);
        setPadding(horizontal, vertical, horizontal, vertical);
        setBackgroundResource(R.drawable.bg_row);
        setClickable(true);
        setFocusable(true);
        binding = ViewSettingRowBinding.inflate(LayoutInflater.from(context), this);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SettingRowView);
            int icon = a.getResourceId(R.styleable.SettingRowView_rowIcon, 0);
            if (icon != 0) {
                binding.rowIcon.setImageResource(icon);
            } else {
                binding.rowIcon.setVisibility(GONE);
            }
            if (a.hasValue(R.styleable.SettingRowView_rowIconTint)) {
                binding.rowIcon.setImageTintList(ColorStateList.valueOf(
                        a.getColor(R.styleable.SettingRowView_rowIconTint, 0)));
            }
            binding.rowTitle.setText(a.getString(R.styleable.SettingRowView_rowTitle));
            setSubtitle(a.getString(R.styleable.SettingRowView_rowSubtitle));
            setValue(a.getString(R.styleable.SettingRowView_rowValue));
            boolean showSwitch = a.getBoolean(R.styleable.SettingRowView_rowShowSwitch, false);
            binding.rowSwitch.setVisibility(showSwitch ? VISIBLE : GONE);
            binding.rowChevron.setVisibility(a.getBoolean(R.styleable.SettingRowView_rowShowChevron, false)
                    ? VISIBLE : GONE);
            a.recycle();
            if (showSwitch) {
                super.setOnClickListener(v -> {
                    boolean checked = !binding.rowSwitch.isChecked();
                    binding.rowSwitch.setChecked(checked);
                    if (checkedListener != null) {
                        checkedListener.onChecked(checked);
                    }
                });
            }
        }
    }

    public void setTitle(CharSequence title) {
        binding.rowTitle.setText(title);
    }

    public void setSubtitle(CharSequence subtitle) {
        binding.rowSubtitle.setText(subtitle);
        binding.rowSubtitle.setVisibility(TextUtils.isEmpty(subtitle) ? GONE : VISIBLE);
    }

    public void setValue(CharSequence value) {
        binding.rowValue.setText(value);
        binding.rowValue.setVisibility(TextUtils.isEmpty(value) ? GONE : VISIBLE);
    }

    public void setChecked(boolean checked) {
        binding.rowSwitch.setChecked(checked);
    }

    public boolean isChecked() {
        return binding.rowSwitch.isChecked();
    }

    public void setOnCheckedListener(OnCheckedListener listener) {
        this.checkedListener = listener;
    }

    public View getSwitchView() {
        return binding.rowSwitch;
    }
}
