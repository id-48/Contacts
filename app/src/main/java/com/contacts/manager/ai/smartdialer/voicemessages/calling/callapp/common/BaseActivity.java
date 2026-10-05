package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Lifecycle;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.analytics.Analytics;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.analytics.ClickTracker;

public abstract class BaseActivity extends AppCompatActivity {

    private final ClickTracker clickTracker = new ClickTracker(this::screenName);
    @Nullable
    private OnBackPressedCallback backAdCallback;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Analytics.logScreen(screenName(), getClass().getSimpleName());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        clickTracker.onTouch(getWindow().getDecorView(), event);
        return super.dispatchTouchEvent(event);
    }

    protected String screenName() {
        return Analytics.screenName(getClass());
    }

    protected void onScreenChanged() {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            Analytics.logScreen(screenName(), getClass().getSimpleName());
        }
    }

    protected void applyInsets(View root) {
        applyInsets(root, true, true);
    }

    protected void applyInsets(View root, boolean top, boolean bottom) {
        int left = root.getPaddingLeft();
        int topPadding = root.getPaddingTop();
        int right = root.getPaddingRight();
        int bottomPadding = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(left + insets.left,
                    topPadding + (top ? insets.top : 0),
                    right + insets.right,
                    bottomPadding + (bottom ? Math.max(insets.bottom, ime.bottom) : 0));
            return WindowInsetsCompat.CONSUMED;
        });
    }

    protected void setupBack(View backButton) {
        if (backButton != null) {
            backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }
    }

    protected void setupBackAd(String screenKey) {
        if (backAdCallback != null) {
            backAdCallback.remove();
        }
        backAdCallback = new OnBackPressedCallback(true) {
            private boolean showing;

            @Override
            public void handleOnBackPressed() {
                if (showing) {
                    return;
                }
                showing = true;
                AdsManager.showFullscreen(BaseActivity.this, screenKey, () -> {
                    showing = false;
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                });
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backAdCallback);
    }

    protected void showFullscreen(String screenKey, Runnable onDone) {
        AdsManager.showFullscreen(this, screenKey, onDone);
    }
}
