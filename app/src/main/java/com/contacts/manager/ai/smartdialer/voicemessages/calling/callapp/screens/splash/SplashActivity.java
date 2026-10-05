package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.splash;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.core.splashscreen.SplashScreen;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivitySplashBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.ForceUpdateHelper;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.OnboardingFlow;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.RemoteConfigManager;

public class SplashActivity extends BaseActivity {

    private static final long PROGRESS_DURATION = 2500L;
    private static final long CONFIG_WAIT_MS = 4000L;

    private ActivitySplashBinding binding;
    private ValueAnimator progressAnimator;
    private boolean progressDone;
    private boolean configReady;
    private boolean routed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);

        binding.brandGroup.setAlpha(0f);
        binding.brandGroup.setTranslationY(getResources().getDimension(R.dimen.space_16));
        binding.brandGroup.animate().alpha(1f).translationY(0f).setDuration(400).start();

        showProgress(0);
        startProgress();
        RemoteConfigManager.whenReady(CONFIG_WAIT_MS, config -> {
            configReady = true;
            continueIfReady();
        });
    }

    private void startProgress() {
        progressAnimator = ValueAnimator.ofInt(0, 100);
        progressAnimator.setDuration(PROGRESS_DURATION);
        progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        progressAnimator.addUpdateListener(animation -> showProgress((int) animation.getAnimatedValue()));
        progressAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancelled) openNext();
            }
        });
        progressAnimator.start();
    }

    private void showProgress(int value) {
        binding.splashProgress.setProgressCompat(value, false);
        binding.splashPercent.setText(getString(R.string.splash_progress, value));
    }

    private void openNext() {
        progressDone = true;
        continueIfReady();
    }

    private void continueIfReady() {
        if (!progressDone || !configReady || routed || isFinishing()) return;
        routed = true;
        if (ForceUpdateHelper.blockIfRequired(this)) return;
        AdsManager.showSplashFullscreen(this, this::navigate);
    }

    private void navigate() {
        if (isFinishing() || isDestroyed()) return;
        startActivity(OnboardingFlow.startIntent(this));
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (progressAnimator != null) progressAnimator.cancel();
        super.onDestroy();
    }
}
