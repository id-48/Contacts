package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.settings;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivitySettingsBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.IntentUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.widget.QuickActionsWidgetProvider;

public class SettingsActivity extends BaseActivity {

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);
        binding.topBar.topTitle.setText(R.string.settings);
        setupBack(binding.topBar.backButton);

        binding.languageRow.setOnClickListener(v -> startActivity(LanguageActivity.intent(this, false)));
        binding.themeRow.setOnClickListener(v -> startActivity(ThemeActivity.intent(this, false)));
        binding.blockedRow.setOnClickListener(v -> startActivity(new Intent(this, BlockedNumbersActivity.class)));
        binding.quickResponseRow.setOnClickListener(v -> startActivity(new Intent(this, QuickResponseActivity.class)));
        binding.widgetRow.setOnClickListener(v -> pinWidget());
        binding.shareRow.setOnClickListener(v -> IntentUtils.shareApp(this));
        binding.rateRow.setOnClickListener(v -> IntentUtils.rateApp(this));
        binding.privacyRow.setOnClickListener(v -> startActivity(new Intent(this, PrivacyPolicyActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        binding.languageRow.setValue(LanguageActivity.currentLabel(this));
        binding.themeRow.setValue(ThemeActivity.label(this, StorageService.getThemeMode()));
    }

    private void pinWidget() {
        AppWidgetManager manager = getSystemService(AppWidgetManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null
                && manager.isRequestPinAppWidgetSupported()) {
            manager.requestPinAppWidget(new ComponentName(this, QuickActionsWidgetProvider.class), null, null);
        } else {
            Toast.makeText(this, R.string.widget_manual_hint, Toast.LENGTH_LONG).show();
        }
    }
}
