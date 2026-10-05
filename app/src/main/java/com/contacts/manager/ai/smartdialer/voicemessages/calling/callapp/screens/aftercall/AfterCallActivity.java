package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.aftercall;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;
import android.provider.CallLog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.analytics.Analytics;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.ConfirmDialog;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.IntentKeys;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityAfterCallBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemAfterCallReminderBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemAfterCallTileBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.CallModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.contactdetails.ContactDetailsActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.editcontact.EditContactActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.main.MainActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.BlockService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.CallLogService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PhoneService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ReminderService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.telecom.CallManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.DateUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.IntentUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AfterCallActivity extends BaseActivity {

    private static final int TAB_ACTIONS = 0;
    private static final int TAB_MESSAGES = 1;
    private static final int TAB_REMINDERS = 2;
    private static final int TAB_MORE = 3;
    private static final int GRID_COLUMNS = 4;
    private static final long CALL_LOG_DELAY_MS = 900L;
    private static final long CALL_LOG_WINDOW_MS = 2 * 60 * 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ActivityAfterCallBinding binding;
    private View[] tabs;
    private View[] pages;
    private final boolean[] pageAnimated = new boolean[4];
    private int currentTab;
    private String actionsKey;

    private String number;
    private int callType;
    private long duration;
    private long callTime;
    private ContactModel contact;
    private boolean blocked;
    private boolean canBlock;

    private static final class Tile {
        final int icon;
        final CharSequence label;
        final String clickName;
        final boolean destructive;
        final View.OnClickListener listener;

        Tile(@DrawableRes int icon, CharSequence label, String clickName, boolean destructive,
             View.OnClickListener listener) {
            this.icon = icon;
            this.label = label;
            this.clickName = clickName;
            this.destructive = destructive;
            this.listener = listener;
        }
    }

    public static Intent intent(Context context, CallManager.EndedCall ended) {
        Intent intent = intent(context, ended.number, ended.callLogType, ended.durationSeconds,
                System.currentTimeMillis() - ended.durationSeconds * 1000L);
        if (ended.contact != null) {
            intent.putExtra(IntentKeys.EXTRA_CONTACT_ID, ended.contact.id)
                    .putExtra(IntentKeys.EXTRA_NAME, ended.contact.name)
                    .putExtra(IntentKeys.EXTRA_LOOKUP_KEY, ended.contact.lookupKey);
        }
        return intent;
    }

    public static Intent intent(Context context, String number, int callType, long durationSeconds, long callTime) {
        return new Intent(context, AfterCallActivity.class)
                .putExtra(IntentKeys.EXTRA_NUMBER, number)
                .putExtra(IntentKeys.EXTRA_CALL_TYPE, callType)
                .putExtra(IntentKeys.EXTRA_DURATION, durationSeconds)
                .putExtra(IntentKeys.EXTRA_CALL_TIME, callTime);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAfterCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);
        readIntent(getIntent());

        tabs = new View[]{binding.tabActions, binding.tabMessages, binding.tabReminders, binding.tabMore};
        pages = new View[]{binding.pageActions, binding.pageMessages, binding.pageReminders, binding.pageMore};
        for (int i = 0; i < tabs.length; i++) {
            int tab = i;
            tabs[i].setOnClickListener(v -> {
                HapticUtils.tap(v);
                selectTab(tab, true);
            });
        }
        binding.tabs.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (right - left != oldRight - oldLeft) {
                movePill(currentTab, false);
            }
        });

        binding.closeButton.setOnClickListener(v -> showFullscreen(AdScreens.AFTER_CALL_CLOSE, this::finish));
        AdsManager.showNativeBig(this, binding.nativeBigContainer, AdScreens.AFTER_CALL);
        binding.callBackButton.setOnClickListener(v -> {
            HapticUtils.confirm(v);
            if (isDialable()) {
                PhoneService.call(this, number);
                finish();
            }
        });
        setupPersonalMessage();

        buildMessages();
        buildReminderChips();
        buildMore();
        render();
        renderReminders();
        selectTab(TAB_ACTIONS, false);
        if (savedInstanceState == null) {
            animateIn();
            trackOpen();
        }
        load();
        refineFromCallLog();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readIntent(intent);
        actionsKey = null;
        buildMore();
        render();
        renderReminders();
        selectTab(TAB_ACTIONS, false);
        load();
        refineFromCallLog();
        trackOpen();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void readIntent(Intent intent) {
        number = intent.getStringExtra(IntentKeys.EXTRA_NUMBER);
        callType = intent.getIntExtra(IntentKeys.EXTRA_CALL_TYPE, 0);
        duration = intent.getLongExtra(IntentKeys.EXTRA_DURATION, 0);
        callTime = intent.getLongExtra(IntentKeys.EXTRA_CALL_TIME, System.currentTimeMillis() - duration * 1000L);
        long contactId = intent.getLongExtra(IntentKeys.EXTRA_CONTACT_ID, 0);
        if (contactId > 0) {
            contact = new ContactModel();
            contact.id = contactId;
            contact.name = intent.getStringExtra(IntentKeys.EXTRA_NAME);
            contact.lookupKey = intent.getStringExtra(IntentKeys.EXTRA_LOOKUP_KEY);
        } else {
            contact = null;
        }
        blocked = false;
        canBlock = false;
    }

    private void trackOpen() {
        StorageService.incrementAfterCallOpenCount();
        long total = StorageService.getAfterCallOpenCount();
        Bundle params = new Bundle();
        params.putString(Analytics.PARAM_SCREEN, screenName());
        params.putLong("open_count", total);
        params.putLong("opens_today", StorageService.getAfterCallOpenCountToday());
        params.putString("call_type", callTypeName(callType));
        params.putLong("call_duration_sec", duration);
        params.putLong("saved_contact", isSaved() ? 1 : 0);
        Analytics.logEvent("after_call_open", params);
        Analytics.setUserProperty("after_call_opens", String.valueOf(total));
    }

    private static String callTypeName(int type) {
        switch (type) {
            case CallLog.Calls.INCOMING_TYPE:
                return "incoming";
            case CallLog.Calls.OUTGOING_TYPE:
                return "outgoing";
            case CallLog.Calls.MISSED_TYPE:
                return "missed";
            case CallLog.Calls.VOICEMAIL_TYPE:
                return "voicemail";
            case CallLog.Calls.REJECTED_TYPE:
                return "rejected";
            case CallLog.Calls.BLOCKED_TYPE:
                return "blocked";
            default:
                return "unknown";
        }
    }

    private void load() {
        if (PhoneUtils.isPrivate(number)) {
            return;
        }
        Context app = getApplicationContext();
        String value = number;
        AppExecutors.io(() -> {
            ContactModel found = ContactsService.lookupNumber(app, value);
            boolean isBlocked = BlockService.isBlocked(app, value);
            boolean blockAllowed = BlockService.canBlock(app);
            AppExecutors.main(() -> {
                if (isDestroyed() || !TextUtils.equals(value, number)) {
                    return;
                }
                if (found != null) {
                    contact = found;
                }
                blocked = isBlocked;
                canBlock = blockAllowed;
                render();
            });
        });
    }

    private void refineFromCallLog() {
        if (!CallLogService.canRead(this)) {
            return;
        }
        Context app = getApplicationContext();
        String expected = number;
        long since = callTime - CALL_LOG_WINDOW_MS;
        handler.postDelayed(() -> AppExecutors.io(() -> {
            List<CallModel> calls = CallLogService.getRecentCalls(app, 1);
            CallModel latest = calls.isEmpty() ? null : calls.get(0);
            AppExecutors.main(() -> {
                if (isDestroyed() || latest == null || latest.date < since || !TextUtils.equals(expected, number)) {
                    return;
                }
                boolean unknownNumber = PhoneUtils.isPrivate(number);
                if (!unknownNumber && !PhoneUtils.sameNumber(latest.number, number)) {
                    return;
                }
                callTime = latest.date;
                callType = latest.type;
                duration = latest.duration;
                if (unknownNumber && !PhoneUtils.isPrivate(latest.number)) {
                    number = latest.number;
                    actionsKey = null;
                    renderReminders();
                    load();
                }
                render();
            });
        }), CALL_LOG_DELAY_MS);
    }

    private boolean isDialable() {
        return !PhoneUtils.isPrivate(number);
    }

    private boolean isSaved() {
        return contact != null && contact.id > 0;
    }

    private String displayName() {
        if (isSaved() && !TextUtils.isEmpty(contact.name)) {
            return contact.name;
        }
        return isDialable() ? PhoneUtils.format(this, number) : getString(R.string.private_number);
    }

    private void render() {
        boolean dialable = isDialable();
        boolean saved = isSaved();
        binding.nameText.setText(displayName());
        binding.numberText.setText(dialable ? PhoneUtils.format(this, number) : "");
        binding.numberText.setVisibility(saved && dialable ? View.VISIBLE : View.GONE);
        if (blocked) {
            binding.avatar.bindBlocked();
        } else {
            binding.avatar.bind(saved ? contact.name : null, saved ? contact.photoUri : null);
        }

        List<String> details = new ArrayList<>();
        if (callType > 0) {
            binding.typeIcon.setVisibility(View.VISIBLE);
            binding.typeIcon.setImageResource(CallLogService.typeIcon(callType));
            binding.typeIcon.setColorFilter(getColor(CallLogService.typeColor(callType)));
            details.add(getString(CallLogService.typeLabel(callType)));
        } else {
            binding.typeIcon.setVisibility(View.GONE);
            details.add(getString(R.string.call_ended));
        }
        details.add(DateUtils.clockTime(this, callTime));
        details.add(DateUtils.timer(duration));
        binding.detailText.setText(TextUtils.join("  •  ", details));
        binding.callBackButton.setEnabled(dialable);
        binding.callBackButton.setAlpha(dialable ? 1f : 0.4f);

        String key = dialable + "|" + saved + "|" + blocked + "|" + canBlock;
        if (!key.equals(actionsKey)) {
            actionsKey = key;
            buildActions();
        }
    }

    private void buildActions() {
        List<Tile> tiles = new ArrayList<>();
        if (isDialable()) {
            if (isSaved()) {
                tiles.add(new Tile(R.drawable.ic_person, getString(R.string.view_contact), "view_contact", false, v -> {
                    startActivity(ContactDetailsActivity.intent(this, contact.id, contact.lookupKey));
                    finish();
                }));
            } else if (ContactsService.canWrite(this)) {
                tiles.add(new Tile(R.drawable.ic_person_add, getString(R.string.save_contact), "save_contact", false, v -> {
                    startActivity(EditContactActivity.createIntent(this, number));
                    finish();
                }));
            }
        }
        tiles.add(new Tile(R.drawable.ic_history, getString(R.string.after_call_recent), "open_recent", false,
                v -> openMain(MainActivity.TAB_RECENT)));
        tiles.add(new Tile(R.drawable.ic_contacts, getString(R.string.after_call_contacts), "open_contacts", false,
                v -> openMain(MainActivity.TAB_CONTACTS)));
        if (isDialable() && canBlock) {
            tiles.add(new Tile(R.drawable.ic_block, getString(blocked ? R.string.unblock : R.string.block),
                    blocked ? "unblock" : "block", !blocked, v -> toggleBlock()));
        }
        fillGrid(binding.pageActions, tiles);
        if (pageAnimated[TAB_ACTIONS] && currentTab == TAB_ACTIONS) {
            pageAnimated[TAB_ACTIONS] = false;
            animatePage(TAB_ACTIONS, 120);
        }
    }

    private void buildMore() {
        List<Tile> tiles = new ArrayList<>();
        tiles.add(new Tile(R.drawable.ic_chat, getString(R.string.after_call_messages), "open_messages", false,
                v -> launch(IntentUtils.smsIntent(isDialable() ? number : "", null))));
        tiles.add(new Tile(R.drawable.ic_mail, getString(R.string.send_mail), "send_mail", false,
                v -> launch(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")))));
        tiles.add(new Tile(R.drawable.ic_calendar, getString(R.string.calendar), "calendar", false,
                v -> launch(new Intent(Intent.ACTION_INSERT)
                        .setData(CalendarContract.Events.CONTENT_URI)
                        .putExtra(CalendarContract.Events.TITLE,
                                getString(R.string.calendar_event_title, displayName())))));
        tiles.add(new Tile(R.drawable.ic_language, getString(R.string.web), "web_search", false, v -> {
            String url = isDialable()
                    ? "https://www.google.com/search?q=" + Uri.encode(number)
                    : "https://www.google.com";
            launch(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }));
        fillGrid(binding.pageMore, tiles);
    }

    private void fillGrid(LinearLayout page, List<Tile> tiles) {
        page.removeAllViews();
        LinearLayout row = null;
        for (int i = 0; i < tiles.size(); i++) {
            if (i % GRID_COLUMNS == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                page.addView(row, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            Tile tile = tiles.get(i);
            ItemAfterCallTileBinding item = ItemAfterCallTileBinding.inflate(getLayoutInflater(), row, false);
            item.tileIcon.setImageResource(tile.icon);
            item.tileLabel.setText(tile.label);
            item.tileRoot.setContentDescription(tile.label);
            Analytics.setClickName(item.tileRoot, tile.clickName);
            if (tile.destructive) {
                item.tileIconBg.setBackgroundResource(R.drawable.bg_circle_error_soft);
                item.tileIcon.setImageTintList(getColorStateList(R.color.error));
            }
            item.tileRoot.setOnClickListener(v -> {
                HapticUtils.tap(v);
                tile.listener.onClick(v);
            });
            row.addView(item.getRoot());
        }
        if (row != null) {
            for (int i = row.getChildCount(); i < GRID_COLUMNS; i++) {
                row.addView(new Space(this), new LinearLayout.LayoutParams(0, 1, 1f));
            }
        }
    }

    private Chip makeChip(CharSequence text, @DrawableRes int icon) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setEnsureMinTouchTargetSize(false);
        chip.setChipMinHeight(dp(38));
        chip.setChipCornerRadius(dp(12));
        chip.setChipStrokeWidth(0f);
        chip.setChipBackgroundColorResource(R.color.card);
        chip.setRippleColorResource(R.color.card_strong);
        chip.setTextColor(getColor(R.color.text_primary));
        chip.setTextSize(14);
        chip.setEllipsize(TextUtils.TruncateAt.END);
        if (icon != 0) {
            chip.setChipIconResource(icon);
            chip.setChipIconTintResource(R.color.primary);
            chip.setChipIconSize(dp(18));
            chip.setChipIconVisible(true);
        }
        return chip;
    }

    private void buildMessages() {
        ChipGroup group = binding.messageChips;
        group.removeAllViews();
        List<String> responses = StorageService.getQuickResponses(this);
        for (int i = 0; i < responses.size(); i++) {
            String response = responses.get(i);
            Chip chip = makeChip(response, 0);
            Analytics.setClickName(chip, "quick_reply_" + (i + 1));
            chip.setOnClickListener(v -> {
                HapticUtils.tap(v);
                sendMessage(response);
            });
            group.addView(chip);
        }
    }

    private void buildReminderChips() {
        ChipGroup group = binding.reminderChips;
        group.removeAllViews();
        addReminderChip(group, getString(R.string.reminder_in_minutes, 15), R.drawable.ic_schedule,
                "reminder_15_min", () -> saveReminder(System.currentTimeMillis() + 15 * 60_000L));
        addReminderChip(group, getString(R.string.reminder_in_minutes, 30), R.drawable.ic_schedule,
                "reminder_30_min", () -> saveReminder(System.currentTimeMillis() + 30 * 60_000L));
        addReminderChip(group, getString(R.string.reminder_in_hour), R.drawable.ic_schedule,
                "reminder_1_hour", () -> saveReminder(System.currentTimeMillis() + 60 * 60_000L));
        addReminderChip(group, getString(R.string.reminder_pick_time), R.drawable.ic_calendar,
                "reminder_pick_time", this::pickReminderTime);
    }

    private void addReminderChip(ChipGroup group, CharSequence text, @DrawableRes int icon, String clickName,
                                 Runnable action) {
        Chip chip = makeChip(text, icon);
        Analytics.setClickName(chip, clickName);
        chip.setOnClickListener(v -> {
            HapticUtils.tap(v);
            action.run();
        });
        group.addView(chip);
    }

    private void setupPersonalMessage() {
        binding.personalMessageField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                setSendVisible(s.toString().trim().length() > 0);
            }
        });
        binding.personalMessageField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendPersonalMessage();
                return true;
            }
            return false;
        });
        binding.personalMessageSend.setOnClickListener(v -> {
            HapticUtils.tap(v);
            sendPersonalMessage();
        });
    }

    private void setSendVisible(boolean visible) {
        View send = binding.personalMessageSend;
        boolean shown = send.getVisibility() == View.VISIBLE;
        if (visible == shown) {
            return;
        }
        send.animate().cancel();
        if (visible) {
            send.setVisibility(View.VISIBLE);
            send.setScaleX(0.4f);
            send.setScaleY(0.4f);
            send.setAlpha(0f);
            send.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(260)
                    .setInterpolator(new OvershootInterpolator(2.5f)).start();
        } else {
            send.animate().scaleX(0.4f).scaleY(0.4f).alpha(0f).setDuration(140)
                    .withEndAction(() -> send.setVisibility(View.INVISIBLE)).start();
        }
    }

    private void sendPersonalMessage() {
        String text = binding.personalMessageField.getText().toString().trim();
        if (!text.isEmpty()) {
            sendMessage(text);
        }
    }

    private void sendMessage(String text) {
        if (!isDialable()) {
            Toast.makeText(this, R.string.invalid_number, Toast.LENGTH_SHORT).show();
            return;
        }
        launch(IntentUtils.smsIntent(number, text));
    }

    private void launch(Intent intent) {
        hideKeyboard();
        if (IntentUtils.safeStart(this, intent)) {
            finish();
        }
    }

    private void openMain(int tab) {
        startActivity(new Intent(this, MainActivity.class)
                .putExtra(IntentKeys.EXTRA_TAB, tab)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    private void selectTab(int tab, boolean animate) {
        int previous = currentTab;
        currentTab = tab;
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setSelected(i == tab);
        }
        movePill(tab, animate);
        if (previous == TAB_MESSAGES && tab != TAB_MESSAGES) {
            hideKeyboard();
        }

        if (!animate || previous == tab) {
            for (int i = 0; i < pages.length; i++) {
                pages[i].animate().cancel();
                pages[i].setAlpha(1f);
                pages[i].setTranslationX(0f);
                pages[i].setVisibility(i == tab ? View.VISIBLE : View.INVISIBLE);
            }
            return;
        }
        boolean rtl = getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        float shift = dp(24) * (tab > previous ? 1 : -1) * (rtl ? -1 : 1);
        View out = pages[previous];
        View in = pages[tab];
        out.animate().cancel();
        out.animate().alpha(0f).translationX(-shift).setStartDelay(0).setDuration(120)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    out.setVisibility(View.INVISIBLE);
                    out.setAlpha(1f);
                    out.setTranslationX(0f);
                })
                .start();
        in.animate().cancel();
        in.setVisibility(View.VISIBLE);
        in.setAlpha(0f);
        in.setTranslationX(shift);
        in.animate().alpha(1f).translationX(0f).setStartDelay(50).setDuration(260)
                .setInterpolator(new DecelerateInterpolator(2f))
                .start();
        animatePage(tab, 60);
    }

    private void movePill(int tab, boolean animate) {
        View target = tabs[tab];
        int width = target.getWidth();
        if (width == 0) {
            return;
        }
        ViewGroup.LayoutParams params = binding.tabPill.getLayoutParams();
        if (params.width != width) {
            params.width = width;
            binding.tabPill.setLayoutParams(params);
        }
        float x = target.getLeft();
        if (animate) {
            binding.tabPill.animate().translationX(x).setDuration(300)
                    .setInterpolator(new PathInterpolator(0.2f, 0f, 0f, 1f)).start();
        } else {
            binding.tabPill.setTranslationX(x);
        }
    }

    private void animateIn() {
        View[] views = {binding.topBar, binding.headerCard, binding.tabBar};
        float offset = dp(16);
        for (int i = 0; i < views.length; i++) {
            View view = views[i];
            view.setAlpha(0f);
            view.setTranslationY(offset);
            view.animate().alpha(1f).translationY(0f).setStartDelay(50L * i).setDuration(340)
                    .setInterpolator(new DecelerateInterpolator(2f)).start();
        }
        binding.avatar.setScaleX(0.6f);
        binding.avatar.setScaleY(0.6f);
        binding.avatar.animate().scaleX(1f).scaleY(1f).setStartDelay(120).setDuration(420)
                .setInterpolator(new OvershootInterpolator(2f)).start();
        animatePage(TAB_ACTIONS, 180);
    }

    private void animatePage(int tab, long delay) {
        if (pageAnimated[tab]) {
            return;
        }
        pageAnimated[tab] = true;
        List<View> views = new ArrayList<>();
        if (tab == TAB_ACTIONS || tab == TAB_MORE) {
            LinearLayout page = (LinearLayout) pages[tab];
            for (int r = 0; r < page.getChildCount(); r++) {
                ViewGroup row = (ViewGroup) page.getChildAt(r);
                for (int c = 0; c < row.getChildCount(); c++) {
                    views.add(row.getChildAt(c));
                }
            }
        } else {
            ChipGroup group = tab == TAB_MESSAGES ? binding.messageChips : binding.reminderChips;
            for (int i = 0; i < group.getChildCount(); i++) {
                views.add(group.getChildAt(i));
            }
            views.add(tab == TAB_MESSAGES ? binding.personalMessage : binding.reminderEmpty);
        }
        for (int i = 0; i < views.size(); i++) {
            View view = views.get(i);
            view.setAlpha(0f);
            view.setScaleX(0.85f);
            view.setScaleY(0.85f);
            view.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(delay + 35L * i).setDuration(300)
                    .setInterpolator(new OvershootInterpolator(1.6f)).start();
        }
    }

    private void renderReminders() {
        LinearLayout rows = binding.reminderRows;
        rows.removeAllViews();
        List<ReminderService.Reminder> reminders = ReminderService.getUpcomingForNumber(this, number);
        for (ReminderService.Reminder reminder : reminders) {
            ItemAfterCallReminderBinding row = ItemAfterCallReminderBinding.inflate(getLayoutInflater(), rows, false);
            row.reminderTime.setText(reminderLabel(reminder.time));
            row.reminderDelete.setOnClickListener(v -> {
                HapticUtils.tap(v);
                ReminderService.remove(this, reminder.id);
                row.getRoot().animate().alpha(0f).translationX(dp(32)).setDuration(200)
                        .withEndAction(this::renderReminders).start();
            });
            rows.addView(row.getRoot());
        }
        binding.reminderEmpty.setVisibility(reminders.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String reminderLabel(long time) {
        String clock = DateUtils.clockTime(this, time);
        return DateUtils.isToday(time) ? clock : DateUtils.fullDate(time) + ", " + clock;
    }

    private void pickReminderTime() {
        Calendar suggested = Calendar.getInstance();
        suggested.add(Calendar.HOUR_OF_DAY, 1);
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(DateFormat.is24HourFormat(this) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                .setHour(suggested.get(Calendar.HOUR_OF_DAY))
                .setMinute(0)
                .setTitleText(R.string.reminder_pick_time)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            Analytics.logEvent("after_call_reminder_time_set");
            Calendar time = Calendar.getInstance();
            time.set(Calendar.HOUR_OF_DAY, picker.getHour());
            time.set(Calendar.MINUTE, picker.getMinute());
            time.set(Calendar.SECOND, 0);
            time.set(Calendar.MILLISECOND, 0);
            if (time.getTimeInMillis() <= System.currentTimeMillis()) {
                time.add(Calendar.DAY_OF_MONTH, 1);
            }
            saveReminder(time.getTimeInMillis());
        });
        picker.show(getSupportFragmentManager(), "reminder_time");
    }

    private void saveReminder(long time) {
        ReminderService.add(this, number, isSaved() ? contact.name : null, time);
        Toast.makeText(this, getString(R.string.reminder_set, reminderLabel(time)), Toast.LENGTH_SHORT).show();
        renderReminders();
        for (int i = 0; i < binding.reminderRows.getChildCount(); i++) {
            View row = binding.reminderRows.getChildAt(i);
            row.setAlpha(0f);
            row.setTranslationY(dp(12));
            row.animate().alpha(1f).translationY(0f).setStartDelay(40L * i).setDuration(280)
                    .setInterpolator(new DecelerateInterpolator(2f)).start();
        }
    }

    private void toggleBlock() {
        if (blocked) {
            runBlock(false);
            return;
        }
        ConfirmDialog.show(this, getString(R.string.block_number_title, PhoneUtils.format(this, number)),
                getString(R.string.block_number_body), R.string.block, true, R.drawable.ic_block,
                R.string.cancel, () -> runBlock(true));
    }

    private void runBlock(boolean block) {
        Context app = getApplicationContext();
        String value = number;
        AppExecutors.io(() -> {
            boolean ok = block ? BlockService.block(app, value) : BlockService.unblock(app, value);
            AppExecutors.main(() -> {
                if (isDestroyed()) {
                    return;
                }
                if (ok) {
                    blocked = block;
                    Toast.makeText(app, block ? R.string.number_blocked : R.string.number_unblocked,
                            Toast.LENGTH_SHORT).show();
                    render();
                } else {
                    Toast.makeText(app, block ? R.string.block_failed : R.string.unblock_failed,
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void hideKeyboard() {
        InputMethodManager input = getSystemService(InputMethodManager.class);
        if (input != null) {
            input.hideSoftInputFromWindow(binding.root.getWindowToken(), 0);
        }
        binding.personalMessageField.clearFocus();
    }

    private float dp(int value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
