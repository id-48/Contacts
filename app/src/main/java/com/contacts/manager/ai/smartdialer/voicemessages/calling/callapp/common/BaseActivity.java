package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
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
}
