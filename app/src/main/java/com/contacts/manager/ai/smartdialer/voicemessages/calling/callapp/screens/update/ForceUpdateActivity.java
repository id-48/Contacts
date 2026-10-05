package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.update;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityForceUpdateBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.ForceUpdateHelper;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.RemoteConfig;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.RemoteConfigManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.splash.SplashActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.IntentUtils;

/** Blocks normal usage until the user installs a version >= the admin's minimum version. */
public class ForceUpdateActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityForceUpdateBinding binding = ActivityForceUpdateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);

        RemoteConfig config = RemoteConfigManager.get();
        String message = config.forceUpdateMessage.trim();
        binding.updateMessage.setText(message.isEmpty()
                ? getString(R.string.update_required_message, getString(R.string.app_name)) : message);
        binding.updateButton.setOnClickListener(v -> openStore(config.appLink));
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        RemoteConfigManager.refresh();
        if (!ForceUpdateHelper.isUpdateRequired()) {
            startActivity(new Intent(this, SplashActivity.class));
            finish();
        }
    }

    private void openStore(String appLink) {
        if (!appLink.isEmpty()) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(appLink)));
                return;
            } catch (ActivityNotFoundException ignored) {
            }
        }
        IntentUtils.rateApp(this);
    }
}
