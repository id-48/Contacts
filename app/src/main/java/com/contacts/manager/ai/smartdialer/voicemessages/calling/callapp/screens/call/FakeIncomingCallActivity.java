package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.call;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityFakeIncomingCallBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.IncludeCallControlBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.settings.CallStyleActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.FakeCallService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.telecom.FakeCallReceiver;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.DateUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

import java.io.File;

public class FakeIncomingCallActivity extends BaseActivity {

    private static final String EXTRA_ANSWER = "extra_fake_answer";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable missed = () -> finishWith(FakeCallService.STATUS_MISSED);
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            long seconds = (System.currentTimeMillis() - answeredAt) / 1000;
            binding.callTimer.setText(DateUtils.timer(seconds));
            if (call != null && seconds >= call.duration) {
                finishWith(FakeCallService.STATUS_ANSWERED);
                return;
            }
            handler.postDelayed(this, 1000);
        }
    };

    private ActivityFakeIncomingCallBinding binding;
    private FakeCallService.FakeCall call;
    private Ringtone ringtone;
    private Vibrator vibrator;
    private boolean ringing;
    private boolean done;
    private long answeredAt;

    public static Intent intent(Context context, long id, boolean answer) {
        return new Intent(context, FakeIncomingCallActivity.class)
                .putExtra(FakeCallService.EXTRA_FAKE_CALL_ID, id)
                .putExtra(EXTRA_ANSWER, answer);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FakeCallReceiver.cancelNotification(this);
        call = FakeCallService.find(getIntent().getLongExtra(FakeCallService.EXTRA_FAKE_CALL_ID, -1));
        if (call == null || !call.isUpcoming()) {
            finish();
            return;
        }
        binding = ActivityFakeIncomingCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);

        File wallpaper = CallStyleActivity.wallpaperFile(this);
        boolean hasWallpaper = wallpaper != null;
        if (hasWallpaper) {
            binding.wallpaper.setVisibility(View.VISIBLE);
            binding.scrim.setVisibility(View.VISIBLE);
            CallStyleActivity.loadWallpaper(binding.wallpaper, wallpaper);
            int white = getColor(R.color.white);
            for (TextView text : new TextView[]{binding.callName, binding.callNumber, binding.callTimer}) {
                text.setTextColor(white);
            }
            binding.callStatus.setTextColor(getColor(R.color.white));
            binding.callStatus.setAlpha(0.85f);
        }
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        binding.incomingControl.configure(StorageService.getCallStyle(), StorageService.isAnswerOnLeft(),
                hasWallpaper || night);
        binding.incomingControl.setListener(new IncomingCallControlView.Listener() {
            @Override
            public void onAnswer() {
                answer();
            }

            @Override
            public void onDecline() {
                finishWith(FakeCallService.STATUS_DECLINED);
            }
        });

        bindIdentity();
        setupControl(binding.muteControl, R.drawable.ic_mic_off, R.string.mute, hasWallpaper);
        setupControl(binding.keypadControl, R.drawable.ic_dialpad, R.string.keypad, hasWallpaper);
        setupControl(binding.speakerControl, R.drawable.ic_volume_up, R.string.speaker, hasWallpaper);
        binding.endButton.setOnClickListener(v -> {
            HapticUtils.confirm(v);
            finishWith(FakeCallService.STATUS_ANSWERED);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (ringing) {
                    finishWith(FakeCallService.STATUS_DECLINED);
                } else {
                    moveTaskToBack(true);
                }
            }
        });

        if (getIntent().getBooleanExtra(EXTRA_ANSWER, false)) {
            answer();
        } else {
            startRinging();
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        if (intent.getBooleanExtra(EXTRA_ANSWER, false) && ringing) {
            answer();
        }
    }

    private void bindIdentity() {
        String number = call.number;
        boolean hasName = !TextUtils.isEmpty(call.name);
        binding.callName.setText(hasName ? call.name : PhoneUtils.format(this, number));
        boolean showNumber = hasName && !TextUtils.isEmpty(number);
        binding.callNumber.setText(showNumber ? PhoneUtils.format(this, number) : "");
        binding.callNumber.setVisibility(showNumber ? View.VISIBLE : View.GONE);
        binding.avatar.bind(hasName ? call.name : null, null);
        if (TextUtils.isEmpty(number) || !ContactsService.canRead(this)) {
            return;
        }
        Context app = getApplicationContext();
        AppExecutors.io(() -> {
            ContactModel contact = ContactsService.lookupNumber(app, number);
            if (contact == null || TextUtils.isEmpty(contact.photoUri)) {
                return;
            }
            AppExecutors.main(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    binding.avatar.bind(hasName ? call.name : contact.name, contact.photoUri);
                }
            });
        });
    }

    private void setupControl(IncludeCallControlBinding control, @DrawableRes int icon, @StringRes int label,
                              boolean onWallpaper) {
        control.controlButton.setImageResource(icon);
        control.controlLabel.setText(label);
        control.controlButton.setContentDescription(getString(label));
        if (onWallpaper) {
            control.controlLabel.setTextColor(getColor(R.color.white));
        }
        control.controlButton.setOnClickListener(v -> {
            HapticUtils.tap(v);
            v.setActivated(!v.isActivated());
        });
    }

    private void startRinging() {
        ringing = true;
        handler.postDelayed(missed, FakeCallService.RING_TIMEOUT_MS);
        AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        int mode = audio == null ? AudioManager.RINGER_MODE_NORMAL : audio.getRingerMode();
        if (mode == AudioManager.RINGER_MODE_NORMAL) {
            Uri uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE);
            if (uri == null) {
                uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
            ringtone = RingtoneManager.getRingtone(this, uri);
            if (ringtone != null) {
                ringtone.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone.setLooping(true);
                }
                try {
                    ringtone.play();
                } catch (Exception ignored) {
                }
            }
        }
        if (mode != AudioManager.RINGER_MODE_SILENT) {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 900, 1100};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            }
        }
    }

    private void stopRinging() {
        ringing = false;
        handler.removeCallbacks(missed);
        if (ringtone != null) {
            ringtone.stop();
            ringtone = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
    }

    private void answer() {
        if (done || answeredAt > 0) {
            return;
        }
        stopRinging();
        answeredAt = System.currentTimeMillis();
        binding.incomingControl.setVisibility(View.GONE);
        binding.callStatus.setVisibility(View.GONE);
        binding.activePanel.setVisibility(View.VISIBLE);
        binding.endButton.setVisibility(View.VISIBLE);
        binding.activePanel.setAlpha(0f);
        binding.activePanel.animate().alpha(1f).setDuration(300).start();
        handler.post(ticker);
    }

    private void finishWith(int status) {
        if (done) {
            return;
        }
        done = true;
        stopRinging();
        handler.removeCallbacksAndMessages(null);
        FakeCallService.complete(call.id, answeredAt > 0 ? FakeCallService.STATUS_ANSWERED : status);
        if (binding != null) {
            binding.callStatus.setVisibility(View.VISIBLE);
            binding.callStatus.setText(R.string.call_ended);
            binding.callStatus.setTextColor(getColor(R.color.call_missed));
            binding.incomingControl.setVisibility(View.GONE);
            binding.endButton.setEnabled(false);
            binding.endButton.setAlpha(0.4f);
        }
        handler.postDelayed(this::finishAndRemoveTask, 700);
    }

    @Override
    protected void onDestroy() {
        stopRinging();
        handler.removeCallbacksAndMessages(null);
        if (call != null && !done && isFinishing()) {
            FakeCallService.complete(call.id, answeredAt > 0 ? FakeCallService.STATUS_ANSWERED
                    : FakeCallService.STATUS_MISSED);
        }
        super.onDestroy();
    }
}
