package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.PermissionRequester;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityPermissionsBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemPermissionBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PermissionManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PhoneService;

public class PermissionsActivity extends BaseActivity {

    private ActivityPermissionsBinding binding;
    private PermissionRequester requester;

    private final ActivityResultLauncher<Intent> roleLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> render());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPermissionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);
        binding.topBar.topTitle.setText(R.string.permissions);
        setupBack(binding.topBar.backButton);
        requester = new PermissionRequester(this, this::render);
        binding.systemSettingsButton.setOnClickListener(v -> PermissionManager.openAppSettings(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        binding.roleGroup.removeAllViews();
        ItemPermissionBinding role = ItemPermissionBinding.inflate(getLayoutInflater(), binding.roleGroup, true);
        role.permissionIcon.setImageResource(R.drawable.ic_phone_in_talk);
        role.permissionTitle.setText(R.string.default_phone_app);
        role.permissionBody.setText(R.string.default_phone_permission_body);
        bindState(role, PhoneService.isDefaultDialer(this), v -> requestDefault());

        binding.permissionGroup.removeAllViews();
        for (PermissionManager.Group group : PermissionManager.Group.values()) {
            if (!PermissionManager.isSupported(group)) {
                continue;
            }
            ItemPermissionBinding item = ItemPermissionBinding.inflate(getLayoutInflater(), binding.permissionGroup, true);
            item.permissionIcon.setImageResource(group.iconRes);
            item.permissionTitle.setText(group.titleRes);
            item.permissionBody.setText(group.bodyRes);
            bindState(item, PermissionManager.isGranted(this, group), v -> requester.request(this, group));
        }
    }

    private void bindState(ItemPermissionBinding item, boolean granted, View.OnClickListener onAllow) {
        item.permissionGranted.setVisibility(granted ? View.VISIBLE : View.GONE);
        item.permissionButton.setVisibility(granted ? View.GONE : View.VISIBLE);
        item.permissionButton.setText(R.string.allow);
        item.permissionButton.setContentDescription(getString(R.string.allow_named, item.permissionTitle.getText()));
        item.permissionButton.setOnClickListener(onAllow);
    }

    private void requestDefault() {
        Intent intent = PhoneService.createDefaultDialerIntent(this);
        if (intent == null) {
            Toast.makeText(this, R.string.default_phone_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            roleLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.default_phone_unavailable, Toast.LENGTH_SHORT).show();
        }
    }
}
