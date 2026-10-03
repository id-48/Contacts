package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.DrawableRes;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemSheetOptionBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.SheetOptionsBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

public final class AppBottomSheet {

    public static class Option {
        public final int iconRes;
        public final CharSequence title;
        public final Runnable action;
        public boolean checked;

        public Option(@DrawableRes int iconRes, CharSequence title, Runnable action) {
            this.iconRes = iconRes;
            this.title = title;
            this.action = action;
        }

        public Option checked(boolean checked) {
            this.checked = checked;
            return this;
        }
    }

    private AppBottomSheet() {
    }

    public static BottomSheetDialog showOptions(Context context, CharSequence title, List<Option> options) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        SheetOptionsBinding binding = SheetOptionsBinding.inflate(LayoutInflater.from(context));
        binding.sheetTitle.setText(title);
        binding.sheetTitle.setVisibility(TextUtils.isEmpty(title) ? View.GONE : View.VISIBLE);
        for (Option option : options) {
            ItemSheetOptionBinding item = ItemSheetOptionBinding.inflate(LayoutInflater.from(context),
                    binding.sheetOptions, false);
            if (option.iconRes != 0) {
                item.optionIcon.setImageResource(option.iconRes);
            } else {
                item.optionIcon.setVisibility(View.GONE);
            }
            item.optionTitle.setText(option.title);
            item.optionCheck.setVisibility(option.checked ? View.VISIBLE : View.GONE);
            item.getRoot().setOnClickListener(v -> {
                dialog.dismiss();
                if (option.action != null) {
                    option.action.run();
                }
            });
            binding.sheetOptions.addView(item.getRoot());
        }
        dialog.setContentView(binding.getRoot());
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.getBehavior().setSkipCollapsed(true);
        dialog.show();
        return dialog;
    }
}
