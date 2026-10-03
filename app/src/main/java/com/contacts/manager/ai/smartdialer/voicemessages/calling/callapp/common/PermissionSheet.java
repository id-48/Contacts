package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemWelcomeFeatureBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.SheetPermissionsBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

public final class PermissionSheet {

    public static final class Item {
        final int iconRes;
        final int titleRes;
        final int bodyRes;

        public Item(@DrawableRes int iconRes, @StringRes int titleRes, @StringRes int bodyRes) {
            this.iconRes = iconRes;
            this.titleRes = titleRes;
            this.bodyRes = bodyRes;
        }
    }

    private PermissionSheet() {
    }

    public static BottomSheetDialog show(Activity activity, List<Item> items,
                                         Runnable onContinue, Runnable onDismiss) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        SheetPermissionsBinding binding = SheetPermissionsBinding.inflate(LayoutInflater.from(activity));
        int rowPadding = activity.getResources().getDimensionPixelSize(R.dimen.space_8);
        for (Item item : items) {
            ItemWelcomeFeatureBinding row = ItemWelcomeFeatureBinding.inflate(LayoutInflater.from(activity),
                    binding.permissionRows, false);
            row.featureIcon.setImageResource(item.iconRes);
            row.featureTitle.setText(item.titleRes);
            row.featureBody.setText(item.bodyRes);
            View root = row.getRoot();
            root.setPadding(root.getPaddingLeft(), rowPadding, root.getPaddingRight(), rowPadding);
            binding.permissionRows.addView(root);
        }
        boolean[] continued = {false};
        binding.continueButton.setOnClickListener(v -> {
            continued[0] = true;
            dialog.dismiss();
            onContinue.run();
        });
        dialog.setOnDismissListener(d -> {
            if (!continued[0] && onDismiss != null) {
                onDismiss.run();
            }
        });
        dialog.setCancelable(onDismiss != null);
        dialog.setContentView(binding.getRoot());
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.getBehavior().setSkipCollapsed(true);
        dialog.show();
        return dialog;
    }
}
