package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.telecom;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.telecom.Call;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class CallAlerts {

    private static final long FLASH_INTERVAL_MS = 280L;
    private static final long ANNOUNCE_DELAY_MS = 1200L;
    private static final long ANNOUNCE_REPEAT_MS = 6000L;
    private static final int MAX_ANNOUNCEMENTS = 3;

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Set<Call> connected = new HashSet<>();
    private String torchId;
    private boolean torchResolved;
    private boolean torchOn;
    private boolean flashing;
    private Call announcing;
    private int announceCount;
    private TextToSpeech tts;
    private boolean ttsReady;
    private String pendingSpeech;

    private final Runnable flashTick = new Runnable() {
        @Override
        public void run() {
            setTorch(!torchOn);
            handler.postDelayed(this, FLASH_INTERVAL_MS);
        }
    };

    private final Runnable announceTick = new Runnable() {
        @Override
        public void run() {
            if (announcing == null || CallManager.getState(announcing) != Call.STATE_RINGING
                    || announceCount >= MAX_ANNOUNCEMENTS) {
                return;
            }
            announceCount++;
            speak(context.getString(R.string.announce_call, CallManager.getDisplayName(context, announcing)));
            handler.postDelayed(this, ANNOUNCE_REPEAT_MS);
        }
    };

    CallAlerts(Context context) {
        this.context = context.getApplicationContext();
    }

    void update() {
        List<Call> calls = CallManager.getCalls();
        Call ringing = null;
        for (Call call : calls) {
            int state = CallManager.getState(call);
            if (state == Call.STATE_RINGING) {
                ringing = call;
            } else if (state == Call.STATE_ACTIVE && !connected.contains(call)) {
                connected.add(call);
                if (StorageService.isVibrateOnAnswer()) {
                    vibrate();
                }
            }
        }
        connected.retainAll(calls);

        if (ringing != null && StorageService.isFlashOnCall()) {
            startFlash();
        } else {
            stopFlash();
        }

        if (ringing != null && StorageService.isCallAnnouncer() && ringerAudible()) {
            if (ringing != announcing) {
                announcing = ringing;
                announceCount = 0;
                handler.removeCallbacks(announceTick);
                handler.postDelayed(announceTick, ANNOUNCE_DELAY_MS);
            }
        } else if (announcing != null) {
            stopAnnouncing();
        }
    }

    void release() {
        stopFlash();
        stopAnnouncing();
        handler.removeCallbacksAndMessages(null);
        if (tts != null) {
            tts.shutdown();
            tts = null;
            ttsReady = false;
        }
    }

    private boolean ringerAudible() {
        AudioManager audio = context.getSystemService(AudioManager.class);
        return audio == null || audio.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
    }

    private void stopAnnouncing() {
        announcing = null;
        announceCount = 0;
        pendingSpeech = null;
        handler.removeCallbacks(announceTick);
        if (tts != null) {
            tts.stop();
        }
    }

    private void speak(String text) {
        if (tts == null) {
            pendingSpeech = text;
            tts = new TextToSpeech(context, status -> {
                ttsReady = status == TextToSpeech.SUCCESS;
                if (ttsReady && tts != null) {
                    int result = tts.setLanguage(Locale.getDefault());
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts.setLanguage(Locale.ENGLISH);
                    }
                    tts.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build());
                    if (pendingSpeech != null) {
                        tts.speak(pendingSpeech, TextToSpeech.QUEUE_FLUSH, null, "announce");
                        pendingSpeech = null;
                    }
                }
            });
            return;
        }
        if (ttsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "announce");
        } else {
            pendingSpeech = text;
        }
    }

    private void startFlash() {
        if (flashing || resolveTorch() == null) {
            return;
        }
        flashing = true;
        handler.post(flashTick);
    }

    private void stopFlash() {
        if (!flashing) {
            return;
        }
        flashing = false;
        handler.removeCallbacks(flashTick);
        setTorch(false);
    }

    private String resolveTorch() {
        if (torchResolved) {
            return torchId;
        }
        torchResolved = true;
        CameraManager cameras = context.getSystemService(CameraManager.class);
        if (cameras == null) {
            return null;
        }
        try {
            for (String id : cameras.getCameraIdList()) {
                Boolean flash = cameras.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facing = cameras.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING);
                if (Boolean.TRUE.equals(flash) && (facing == null || facing == CameraCharacteristics.LENS_FACING_BACK)) {
                    torchId = id;
                    break;
                }
            }
        } catch (Exception ignored) {
        }
        return torchId;
    }

    private void setTorch(boolean on) {
        CameraManager cameras = context.getSystemService(CameraManager.class);
        if (cameras == null || torchId == null) {
            return;
        }
        try {
            cameras.setTorchMode(torchId, on);
            torchOn = on;
        } catch (Exception e) {
            torchOn = false;
        }
    }

    @SuppressWarnings("deprecation")
    private void vibrate() {
        Vibrator vibrator = context.getSystemService(Vibrator.class);
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(160, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(160);
        }
    }
}
