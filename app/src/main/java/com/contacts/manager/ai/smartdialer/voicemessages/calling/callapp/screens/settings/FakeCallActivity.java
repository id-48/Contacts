package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.settings;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.StringRes;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.analytics.Analytics;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.AppDialogs;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.ContactPickerSheet;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.PermissionRequester;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.TileRows;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityFakeCallBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.DialogFakeCallBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemTileRowBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.FakeCallService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.NotificationService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PermissionManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.DateUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class FakeCallActivity extends BaseActivity {

    private static final long[] DELAYS = {10_000L, 30_000L, 60_000L, 300_000L, 600_000L, 1_800_000L};
    private static final int[] DELAY_LABELS = {R.string.seconds_10, R.string.seconds_30, R.string.minute_1,
            R.string.minutes_5, R.string.minutes_10, R.string.minutes_30};
    private static final int[] DURATIONS = {15, 30, 60, 120, 300};
    private static final int[] DURATION_LABELS = {R.string.duration_15s, R.string.duration_30s, R.string.duration_1m,
            R.string.duration_2m, R.string.duration_5m};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresher = new Runnable() {
        @Override
        public void run() {
            if (!showCompleted) {
                render();
            }
            handler.postDelayed(this, 1000);
        }
    };

    private ActivityFakeCallBinding binding;
    private PermissionRequester permissionRequester;
    private boolean showCompleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionRequester = new PermissionRequester(this, () -> {
        });
        binding = ActivityFakeCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);
        binding.topBar.topTitle.setText(R.string.fake_call_manager);
        setupBack(binding.topBar.backButton);
        setupBackAd(AdScreens.FAKE_CALL_BACK);
        showNativeBig(binding.ads.nativeBigContainer, AdScreens.FAKE_CALL);

        binding.tabs.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            showCompleted = checkedId == R.id.completedTab;
            HapticUtils.tick(group);
            render();
        });
        binding.topBar.topAction.setText(R.string.clear_all);
        binding.topBar.topAction.setOnClickListener(v -> AppDialogs.confirm(this, getString(R.string.clear_all), null,
                R.string.delete, true, () -> {
                    FakeCallService.clearCompleted();
                    render();
                }));
        binding.scheduleButton.setOnClickListener(v -> showScheduleDialog());
        binding.scroll.setOnScrollChangeListener((View.OnScrollChangeListener) (v, x, y, oldX, oldY) -> {
            if (y > oldY + 8) {
                binding.scheduleButton.shrink();
            } else if (y < oldY - 8) {
                binding.scheduleButton.extend();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
        handler.postDelayed(refresher, 1000);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refresher);
        super.onPause();
    }

    private void render() {
        List<FakeCallService.FakeCall> items = showCompleted ? FakeCallService.getCompleted()
                : FakeCallService.getUpcoming();
        binding.topBar.topAction.setVisibility(showCompleted && !items.isEmpty() ? View.VISIBLE : View.GONE);
        binding.listGroup.removeAllViews();
        binding.listGroup.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        if (items.isEmpty()) {
            binding.emptyState.setContent(R.drawable.ic_phone_in_talk,
                    showCompleted ? R.string.fake_call_empty_completed : R.string.fake_call_empty_upcoming,
                    showCompleted ? R.string.fake_call_empty_completed_body : R.string.fake_call_empty_upcoming_body);
            binding.emptyState.show(true);
            return;
        }
        binding.emptyState.show(false);
        long now = System.currentTimeMillis();
        for (FakeCallService.FakeCall call : items) {
            String title = TextUtils.isEmpty(call.name) ? PhoneUtils.format(this, call.number) : call.name;
            String subtitle;
            if (call.isUpcoming()) {
                long seconds = Math.max(0, (call.time - now) / 1000);
                subtitle = getString(R.string.fake_call_in, DateUtils.timer(seconds)) + "  •  "
                        + DateUtils.clockTime(this, call.time);
            } else {
                subtitle = getString(statusLabel(call.status)) + "  •  " + DateUtils.rowTime(this, call.completedAt);
            }
            ItemTileRowBinding row = TileRows.add(binding.listGroup, title, subtitle);
            TileRows.avatar(row, call.name, null);
            if (call.isUpcoming()) {
                TileRows.action(row, R.drawable.ic_close, getString(R.string.cancel), R.color.text_secondary,
                        v -> AppDialogs.confirm(this, getString(R.string.fake_call_cancel_title), null,
                                R.string.cancel, true, () -> {
                                    FakeCallService.cancel(this, call.id);
                                    render();
                                }));
            } else {
                row.tileSubtitle.setTextColor(getColor(call.status == FakeCallService.STATUS_ANSWERED
                        ? R.color.call_answer : call.status == FakeCallService.STATUS_MISSED
                        ? R.color.call_missed : R.color.text_secondary));
                TileRows.action(row, R.drawable.ic_delete, getString(R.string.delete), R.color.text_secondary, v -> {
                    FakeCallService.remove(call.id);
                    render();
                });
            }
        }
    }

    @StringRes
    private static int statusLabel(int status) {
        switch (status) {
            case FakeCallService.STATUS_ANSWERED:
                return R.string.fake_call_answered;
            case FakeCallService.STATUS_DECLINED:
                return R.string.fake_call_declined;
            default:
                return R.string.fake_call_missed;
        }
    }

    private void addChips(ChipGroup group, int[] labels, int checked) {
        for (int i = 0; i < labels.length; i++) {
            Chip chip = new Chip(this);
            chip.setId(View.generateViewId());
            chip.setText(labels[i]);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setChipBackgroundColorResource(R.color.selector_segment_bg);
            chip.setTextColor(getColorStateList(R.color.selector_segment_text));
            chip.setChipStrokeWidth(0f);
            chip.setTag(i);
            group.addView(chip);
            if (i == checked) {
                chip.setChecked(true);
            }
        }
    }

    private int checkedIndex(ChipGroup group, int fallback) {
        View chip = group.findViewById(group.getCheckedChipId());
        return chip != null && chip.getTag() instanceof Integer ? (Integer) chip.getTag() : fallback;
    }

    private void showScheduleDialog() {
        if (!NotificationService.canPost(this)) {
            permissionRequester.request(this, PermissionManager.Group.NOTIFICATIONS);
        }
        DialogFakeCallBinding form = DialogFakeCallBinding.inflate(getLayoutInflater());
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(form.getRoot());
        addChips(form.timeGroup, DELAY_LABELS, 0);
        addChips(form.durationGroup, DURATION_LABELS, 1);

        int focusedLine = getColor(R.color.primary);
        int idleLine = getColor(R.color.divider);
        form.nameField.setOnFocusChangeListener((v, hasFocus) -> {
            if (form.nameError.getVisibility() != View.VISIBLE) {
                form.nameUnderline.setBackgroundColor(hasFocus ? focusedLine : idleLine);
            }
        });
        form.numberField.setOnFocusChangeListener((v, hasFocus) ->
                form.numberUnderline.setBackgroundColor(hasFocus ? focusedLine : idleLine));
        TextWatcher clearError = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (form.nameError.getVisibility() == View.VISIBLE) {
                    form.nameError.setVisibility(View.GONE);
                    form.nameUnderline.setBackgroundColor(form.nameField.hasFocus() ? focusedLine : idleLine);
                }
            }
        };
        form.nameField.addTextChangedListener(clearError);
        form.numberField.addTextChangedListener(clearError);
        form.pickContact.setOnClickListener(v -> {
            if (!ContactsService.canRead(this)) {
                permissionRequester.request(this, PermissionManager.Group.CONTACTS);
                return;
            }
            ContactPickerSheet.show(this, getString(R.string.fake_call_pick_contact), false, true, null, contact -> {
                form.nameField.setText(contact.getDisplayName());
                form.numberField.setText(contact.primaryNumber);
            });
        });
        form.cancelButton.setOnClickListener(v -> dialog.dismiss());
        form.scheduleButton.setOnClickListener(v -> {
            String name = form.nameField.getText().toString().trim();
            String number = form.numberField.getText().toString().trim();
            if (name.isEmpty() && number.isEmpty()) {
                form.nameError.setVisibility(View.VISIBLE);
                form.nameUnderline.setBackgroundColor(getColor(R.color.error));
                HapticUtils.longPress(form.nameField);
                return;
            }
            int delay = checkedIndex(form.timeGroup, 0);
            int duration = checkedIndex(form.durationGroup, 1);
            FakeCallService.schedule(this, name, number, DELAYS[delay], DURATIONS[duration]);
            HapticUtils.confirm(v);
            Toast.makeText(this, getString(R.string.fake_call_scheduled,
                    getString(DELAY_LABELS[delay]).toLowerCase()), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            if (showCompleted) {
                binding.tabs.check(R.id.upcomingTab);
            }
            render();
        });

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int margin = getResources().getDimensionPixelSize(R.dimen.space_20);
            window.setLayout(getResources().getDisplayMetrics().widthPixels - margin * 2,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setDimAmount(0.45f);
            window.setWindowAnimations(R.style.Animation_App_Pop);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        Analytics.trackDialog(dialog, "fake_call_schedule_dialog");
        dialog.show();
    }
}
