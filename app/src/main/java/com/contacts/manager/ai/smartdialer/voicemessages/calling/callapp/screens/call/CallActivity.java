package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.call;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.text.TextUtils;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.AppBottomSheet;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.QuickResponseDialog;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.IntentKeys;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityCallBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.IncludeCallControlBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.IncludeCallOptionBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.dialer.DialerActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.RemoteConfigManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.telecom.CallManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.DateUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.IntentUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

import java.util.ArrayList;
import java.util.List;

public class CallActivity extends BaseActivity implements CallManager.Listener {

    private static final long TICK_MS = 1000L;
    private static final long CLOSE_DELAY_MS = 1200L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateTimer();
            handler.postDelayed(this, TICK_MS);
        }
    };
    private final Runnable closer = this::finishAndRemoveTask;

    private ActivityCallBinding binding;
    private PowerManager.WakeLock proximityLock;
    private boolean keypadVisible;
    private boolean moreVisible;
    private boolean moreShown;
    private String lastLayoutState;
    private boolean ended;
    private String avatarKey;
    private final StringBuilder dtmf = new StringBuilder();

    public static Intent intent(Context context) {
        return new Intent(context, CallActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        binding = ActivityCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);
        AdsManager.showNativeSmall(this, binding.nativeSmallContainer, AdScreens.CALL);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null && powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            proximityLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    getPackageName() + ":proximity");
        }

        setupControl(binding.muteControl, R.drawable.ic_mic_off, R.string.mute, v -> {
            CallManager.setMuted(!CallManager.isMuted());
            render();
        });
        setupControl(binding.keypadControl, R.drawable.ic_dialpad, R.string.keypad, v -> setKeypadVisible(true));
        setupControl(binding.speakerControl, R.drawable.ic_volume_up, R.string.speaker, v -> onSpeaker());
        setupControl(binding.moreControl, R.drawable.ic_more_vert, R.string.more, v -> {
            moreVisible = !moreVisible;
            render();
        });
        setupOption(binding.holdControl, R.drawable.ic_pause, R.string.hold, v -> {
            CallManager.toggleHold(CallManager.getPrimaryCall());
        });
        setupOption(binding.addCallControl, R.drawable.ic_add_call, R.string.add_call, v -> {
            moreVisible = false;
            render();
            startActivity(DialerActivity.intent(this, null));
        });
        setupOption(binding.swapControl, R.drawable.ic_swap_calls, R.string.swap, v -> CallManager.swap());
        setupOption(binding.mergeControl, R.drawable.ic_merge, R.string.merge, v -> CallManager.merge());
        binding.hideKeypadButton.setOnClickListener(v -> {
            HapticUtils.tap(v);
            setKeypadVisible(false);
        });

        binding.secondarySwap.setOnClickListener(v -> CallManager.swap());
        binding.endButton.setOnClickListener(v -> {
            HapticUtils.confirm(v);
            CallManager.hangup(CallManager.getPrimaryCall());
        });
        binding.answerButton.setOnClickListener(v -> {
            HapticUtils.confirm(v);
            CallManager.answer(ringingCall());
        });
        binding.declineButton.setOnClickListener(v -> {
            HapticUtils.confirm(v);
            CallManager.reject(ringingCall());
        });
        binding.messageButton.setOnClickListener(v -> showQuickResponses());
        binding.dialPad.setListener(key -> {
            dtmf.append(key);
            binding.dtmfDigits.setText(dtmf);
            CallManager.playDtmf(key);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (keypadVisible) {
                    setKeypadVisible(false);
                } else if (moreVisible) {
                    moreVisible = false;
                    render();
                } else {
                    moveTaskToBack(true);
                }
            }
        });

        handleIntent(getIntent());
        if (savedInstanceState == null) {
            animateIn();
        }
    }

    private void animateIn() {
        View[] views = {binding.callTimer, binding.identityBlock, binding.activePanel, binding.bottomBar};
        float offset = 24 * getResources().getDisplayMetrics().density;
        for (int i = 0; i < views.length; i++) {
            View view = views[i];
            view.setAlpha(0f);
            view.setTranslationY(offset);
            view.animate().alpha(1f).translationY(0f)
                    .setStartDelay(60L * i)
                    .setDuration(360)
                    .setInterpolator(new DecelerateInterpolator(2f))
                    .start();
        }
    }

    private void setKeypadVisible(boolean visible) {
        if (keypadVisible == visible) {
            return;
        }
        keypadVisible = visible;
        moreVisible = false;
        beginLayoutTransition();
        render();
    }

    private void beginLayoutTransition() {
        Transition transition = new TransitionSet()
                .addTransition(new Fade().excludeTarget(binding.keypadPanel, true))
                .addTransition(new ChangeBounds())
                .addTransition(new Slide(Gravity.BOTTOM).addTarget(binding.keypadPanel))
                .setDuration(320)
                .setInterpolator(new PathInterpolator(0.2f, 0f, 0f, 1f))
                .excludeTarget(binding.moreCard, true);
        TransitionManager.beginDelayedTransition(binding.root, transition);
    }

    private void setMoreCardVisible(boolean visible) {
        if (moreShown == visible) {
            return;
        }
        moreShown = visible;
        View card = binding.moreCard;
        card.animate().cancel();
        float offset = 16 * getResources().getDisplayMetrics().density;
        if (!visible) {
            card.animate().alpha(0f).translationY(offset).scaleX(0.96f).scaleY(0.96f)
                    .setStartDelay(0)
                    .setDuration(160)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> card.setVisibility(View.GONE))
                    .start();
            return;
        }
        card.setVisibility(View.VISIBLE);
        card.setAlpha(0f);
        card.setTranslationY(offset);
        card.setScaleX(0.96f);
        card.setScaleY(0.96f);
        card.setPivotY(card.getHeight() > 0 ? card.getHeight() : offset * 6);
        card.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setStartDelay(0)
                .setDuration(280)
                .setInterpolator(new DecelerateInterpolator(2f))
                .start();
        int index = 0;
        for (int i = 0; i < binding.moreCard.getChildCount(); i++) {
            View option = binding.moreCard.getChildAt(i);
            if (option.getVisibility() != View.VISIBLE) {
                continue;
            }
            option.animate().cancel();
            option.setScaleX(0.7f);
            option.setScaleY(0.7f);
            option.setAlpha(0f);
            option.animate().scaleX(1f).scaleY(1f).alpha(1f)
                    .setStartDelay(60L + 40L * index++)
                    .setDuration(300)
                    .setInterpolator(new OvershootInterpolator(2f))
                    .start();
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && IntentKeys.ACTION_ANSWER.equals(intent.getAction())) {
            CallManager.answer(ringingCall());
            intent.setAction(null);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        CallManager.addListener(this);
        render();
        handler.post(ticker);
    }

    @Override
    protected void onStop() {
        CallManager.removeListener(this);
        handler.removeCallbacks(ticker);
        releaseProximity();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        releaseProximity();
        super.onDestroy();
    }

    @Override
    public void onCallsChanged() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            render();
        } else {
            handler.post(this::render);
        }
    }

    private Call ringingCall() {
        for (Call call : CallManager.getCalls()) {
            if (CallManager.getState(call) == Call.STATE_RINGING) {
                return call;
            }
        }
        return null;
    }

    private void setupControl(IncludeCallControlBinding control, @DrawableRes int icon, @StringRes int label,
                              View.OnClickListener listener) {
        control.controlButton.setImageResource(icon);
        control.controlLabel.setText(label);
        control.controlButton.setContentDescription(getString(label));
        control.controlButton.setOnClickListener(v -> {
            HapticUtils.tap(v);
            listener.onClick(v);
        });
    }

    private void setupOption(IncludeCallOptionBinding option, @DrawableRes int icon, @StringRes int label,
                             View.OnClickListener listener) {
        option.optionButton.setImageResource(icon);
        option.optionLabel.setText(label);
        option.optionButton.setContentDescription(getString(label));
        option.optionButton.setOnClickListener(v -> {
            HapticUtils.tap(v);
            listener.onClick(v);
        });
    }

    private static void setOptionEnabled(IncludeCallOptionBinding option, boolean enabled) {
        option.optionButton.setEnabled(enabled);
        option.optionLabel.setAlpha(enabled ? 1f : 0.4f);
    }

    private void render() {
        if (binding == null) {
            return;
        }
        Call primary = CallManager.getPrimaryCall();
        if (primary == null) {
            showEnded();
            return;
        }
        if (ended) {
            ended = false;
            handler.removeCallbacks(closer);
        }
        int state = CallManager.getState(primary);
        boolean ringing = state == Call.STATE_RINGING;
        boolean active = state == Call.STATE_ACTIVE;
        boolean holding = state == Call.STATE_HOLDING;

        if (ringing) {
            keypadVisible = false;
            moreVisible = false;
        }
        boolean keypadMode = keypadVisible && active;
        Call secondary = CallManager.getSecondaryCall();
        boolean showSecondary = secondary != null && !ringing;
        String layoutState = ringing + "|" + keypadMode + "|" + active + "|" + showSecondary;
        if (lastLayoutState != null && !layoutState.equals(lastLayoutState)) {
            beginLayoutTransition();
        }
        lastLayoutState = layoutState;

        bindIdentity(primary);
        binding.callStatus.setText(statusText(primary, state));
        binding.callStatus.setTextColor(getColor(ringing ? R.color.primary : R.color.text_secondary));
        binding.callStatus.setVisibility(active ? View.GONE : View.VISIBLE);

        binding.incomingPanel.setVisibility(ringing ? View.VISIBLE : View.GONE);
        binding.endButton.setVisibility(ringing ? View.GONE : View.VISIBLE);
        binding.activePanel.setVisibility(ringing ? View.INVISIBLE : keypadMode ? View.GONE : View.VISIBLE);
        binding.hideKeypadButton.setVisibility(keypadMode ? View.VISIBLE : View.GONE);

        boolean muted = CallManager.isMuted();
        binding.muteControl.controlButton.setActivated(muted);
        binding.muteControl.controlButton.setImageResource(muted ? R.drawable.ic_mic_off : R.drawable.ic_mic);
        int route = CallManager.getAudioRoute();
        boolean speaker = route == CallAudioState.ROUTE_SPEAKER;
        boolean bluetooth = route == CallAudioState.ROUTE_BLUETOOTH;
        binding.speakerControl.controlButton.setActivated(speaker || bluetooth);
        binding.speakerControl.controlButton.setImageResource(bluetooth ? R.drawable.ic_bluetooth : R.drawable.ic_volume_up);
        binding.speakerControl.controlLabel.setText(bluetooth ? R.string.bluetooth : R.string.speaker);
        binding.keypadControl.controlButton.setActivated(keypadVisible);
        binding.keypadControl.controlButton.setEnabled(active);
        binding.moreControl.controlButton.setActivated(moreVisible);

        boolean canSwap = secondary != null && CallManager.getState(secondary) == Call.STATE_HOLDING;
        boolean canMerge = CallManager.canMerge();
        binding.holdControl.optionButton.setActivated(holding);
        setOptionEnabled(binding.holdControl, CallManager.canHold(primary) && (active || holding));
        binding.holdControl.optionLabel.setText(holding ? R.string.resume : R.string.hold);
        binding.holdControl.optionButton.setImageResource(holding ? R.drawable.ic_play : R.drawable.ic_pause);
        setOptionEnabled(binding.addCallControl, active || holding);
        binding.swapControl.optionRoot.setVisibility(canSwap ? View.VISIBLE : View.GONE);
        binding.mergeControl.optionRoot.setVisibility(canMerge ? View.VISIBLE : View.GONE);

        binding.keypadPanel.setVisibility(keypadMode ? View.VISIBLE : View.GONE);
        setMoreCardVisible(moreVisible && !ringing && !keypadMode);
        binding.avatar.setVisibility(keypadMode ? View.GONE : View.VISIBLE);
        if (keypadMode) {
            binding.callNumber.setVisibility(View.GONE);
        }

        if (showSecondary) {
            binding.secondaryBar.setVisibility(View.VISIBLE);
            binding.secondaryText.setText(getString(R.string.on_hold_with, CallManager.getDisplayName(this, secondary)));
            binding.secondarySwap.setVisibility(canSwap ? View.VISIBLE : View.GONE);
        } else {
            binding.secondaryBar.setVisibility(View.GONE);
        }

        updateTimer();
        updateProximity(active && route == CallAudioState.ROUTE_EARPIECE && !keypadVisible);
    }

    private void bindIdentity(Call call) {
        String number = CallManager.getNumber(call);
        ContactModel contact = CallManager.getContact(call);
        String name = CallManager.getDisplayName(this, call);
        binding.callName.setText(name);
        boolean showNumber = contact != null && !CallManager.isConference(call) && !TextUtils.isEmpty(number);
        binding.callNumber.setText(showNumber ? PhoneUtils.format(this, number) : "");
        binding.callNumber.setVisibility(showNumber ? View.VISIBLE : View.GONE);
        String photo = contact == null ? null : contact.photoUri;
        String key = (contact == null ? "" : contact.name) + "|" + photo;
        if (!key.equals(avatarKey)) {
            avatarKey = key;
            binding.avatar.bind(contact == null ? null : contact.name, photo);
        }
    }

    private String statusText(Call call, int state) {
        switch (state) {
            case Call.STATE_RINGING:
                return getString(R.string.incoming_call);
            case Call.STATE_DIALING:
            case Call.STATE_CONNECTING:
            case Call.STATE_NEW:
            case Call.STATE_SELECT_PHONE_ACCOUNT:
            case Call.STATE_PULLING_CALL:
                return getString(R.string.calling);
            case Call.STATE_HOLDING:
                return getString(R.string.on_hold);
            case Call.STATE_DISCONNECTING:
            case Call.STATE_DISCONNECTED:
                return getString(R.string.call_ended);
            default:
                return CallManager.isConference(call) ? getString(R.string.conference) : "";
        }
    }

    private void updateTimer() {
        if (binding == null || ended) {
            return;
        }
        Call primary = CallManager.getPrimaryCall();
        long connect = CallManager.getConnectTime(primary);
        int state = primary == null ? Call.STATE_DISCONNECTED : CallManager.getState(primary);
        if (connect > 0 && (state == Call.STATE_ACTIVE || state == Call.STATE_HOLDING)) {
            long seconds = Math.max(0, (System.currentTimeMillis() - connect) / 1000);
            binding.callTimer.setText(DateUtils.timer(seconds));
        } else {
            binding.callTimer.setText("");
        }
    }

    private void showEnded() {
        if (ended) {
            return;
        }
        ended = true;
        releaseProximity();
        if (lastLayoutState != null) {
            beginLayoutTransition();
        }
        lastLayoutState = "ended";
        binding.callStatus.setVisibility(View.VISIBLE);
        binding.callStatus.setText(R.string.call_ended);
        binding.callStatus.setTextColor(getColor(R.color.call_missed));
        binding.incomingPanel.setVisibility(View.GONE);
        binding.keypadPanel.setVisibility(View.GONE);
        binding.hideKeypadButton.setVisibility(View.GONE);
        setMoreCardVisible(false);
        binding.secondaryBar.setVisibility(View.GONE);
        binding.avatar.setVisibility(View.VISIBLE);
        binding.activePanel.setVisibility(View.INVISIBLE);
        binding.endButton.setVisibility(View.VISIBLE);
        binding.endButton.setEnabled(false);
        binding.endButton.setAlpha(0.4f);
        handler.removeCallbacks(closer);
        boolean afterCall = StorageService.isAfterCallEnabled() && RemoteConfigManager.isAfterCallScreenEnabled();
        handler.postDelayed(closer, afterCall ? 300L : CLOSE_DELAY_MS);
    }

    private void onSpeaker() {
        int route = CallManager.getAudioRoute();
        if (CallManager.isBluetoothAvailable()) {
            List<AppBottomSheet.Option> options = new ArrayList<>();
            options.add(new AppBottomSheet.Option(R.drawable.ic_phone_in_talk, getString(R.string.audio_phone),
                    () -> CallManager.setAudioRoute(CallAudioState.ROUTE_WIRED_OR_EARPIECE))
                    .checked(route == CallAudioState.ROUTE_EARPIECE || route == CallAudioState.ROUTE_WIRED_HEADSET));
            options.add(new AppBottomSheet.Option(R.drawable.ic_volume_up, getString(R.string.speaker),
                    () -> CallManager.setAudioRoute(CallAudioState.ROUTE_SPEAKER))
                    .checked(route == CallAudioState.ROUTE_SPEAKER));
            options.add(new AppBottomSheet.Option(R.drawable.ic_bluetooth, getString(R.string.bluetooth),
                    () -> CallManager.setAudioRoute(CallAudioState.ROUTE_BLUETOOTH))
                    .checked(route == CallAudioState.ROUTE_BLUETOOTH));
            AppBottomSheet.showOptions(this, getString(R.string.audio_output), options);
            return;
        }
        CallManager.setAudioRoute(route == CallAudioState.ROUTE_SPEAKER
                ? CallAudioState.ROUTE_WIRED_OR_EARPIECE : CallAudioState.ROUTE_SPEAKER);
    }

    private void showQuickResponses() {
        Call call = ringingCall();
        if (call == null) {
            return;
        }
        boolean canReject = call.getDetails().can(Call.Details.CAPABILITY_RESPOND_VIA_TEXT);
        List<AppBottomSheet.Option> options = new ArrayList<>();
        for (String response : StorageService.getQuickResponses(this)) {
            options.add(new AppBottomSheet.Option(R.drawable.ic_chat, response, () -> respond(call, response, canReject)));
        }
        options.add(new AppBottomSheet.Option(R.drawable.ic_edit, getString(R.string.custom_message),
                () -> QuickResponseDialog.show(this, R.string.reply_with_message, null, R.string.send,
                        R.drawable.ic_chat, null, value -> respond(call, value, canReject))));
        AppBottomSheet.showOptions(this, getString(R.string.reply_with_message), options);
    }

    private void respond(Call call, String message, boolean viaTelecom) {
        if (viaTelecom) {
            CallManager.rejectWithMessage(call, message);
            return;
        }
        String number = CallManager.getNumber(call);
        CallManager.reject(call);
        if (!TextUtils.isEmpty(number)) {
            IntentUtils.safeStart(this, IntentUtils.smsIntent(number, message).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    private void updateProximity(boolean enable) {
        if (proximityLock == null) {
            return;
        }
        if (enable && !proximityLock.isHeld()) {
            proximityLock.acquire(60 * 60 * 1000L);
        } else if (!enable) {
            releaseProximity();
        }
    }

    private void releaseProximity() {
        if (proximityLock != null && proximityLock.isHeld()) {
            proximityLock.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY);
        }
    }
}
