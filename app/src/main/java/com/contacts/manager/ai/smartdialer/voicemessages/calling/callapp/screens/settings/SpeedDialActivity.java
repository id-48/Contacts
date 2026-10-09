package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.AppBottomSheet;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.ContactPickerSheet;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.PermissionRequester;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.TileRows;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityListScreenBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemTileRowBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PermissionManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PhoneService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.SpeedDialService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

import java.util.ArrayList;
import java.util.List;

public class SpeedDialActivity extends BaseActivity {

    private ActivityListScreenBinding binding;
    private PermissionRequester permissionRequester;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionRequester = new PermissionRequester(this, this::render);
        binding = ActivityListScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);
        binding.topBar.topTitle.setText(R.string.speed_dial);
        setupBack(binding.topBar.backButton);
        setupBackAd(AdScreens.SPEED_DIAL_BACK);
        showNativeBig(binding.ads.nativeBigContainer, AdScreens.SPEED_DIAL);
        render();
    }

    private void render() {
        binding.listGroup.removeAllViews();
        for (int digit = SpeedDialService.FIRST_DIGIT; digit <= SpeedDialService.LAST_DIGIT; digit++) {
            int key = digit;
            SpeedDialService.Entry entry = SpeedDialService.get(key);
            String label = getString(R.string.speed_dial_key, String.valueOf(key));
            ItemTileRowBinding row;
            if (entry == null) {
                row = TileRows.add(binding.listGroup, getString(R.string.speed_dial_not_set), label);
                row.tileTitle.setTextColor(getColor(R.color.text_secondary));
                TileRows.badge(row, String.valueOf(key));
                row.tileBadge.setAlpha(0.55f);
                TileRows.action(row, R.drawable.ic_person_add, getString(R.string.speed_dial_assign), R.color.primary,
                        v -> assign(key));
                row.tileRow.setOnClickListener(v -> assign(key));
            } else {
                String name = TextUtils.isEmpty(entry.name) ? PhoneUtils.format(this, entry.number) : entry.name;
                row = TileRows.add(binding.listGroup, name,
                        label + "  •  " + PhoneUtils.format(this, entry.number));
                TileRows.badge(row, String.valueOf(key));
                row.tileBadge.setAlpha(1f);
                TileRows.action(row, R.drawable.ic_call, getString(R.string.call), R.color.primary,
                        v -> PhoneService.call(this, entry.number));
                row.tileRow.setOnClickListener(v -> showOptions(key, name, entry));
            }
        }
    }

    private void showOptions(int key, String name, SpeedDialService.Entry entry) {
        List<AppBottomSheet.Option> options = new ArrayList<>();
        options.add(new AppBottomSheet.Option(R.drawable.ic_call, getString(R.string.call),
                () -> PhoneService.call(this, entry.number)));
        options.add(new AppBottomSheet.Option(R.drawable.ic_edit, getString(R.string.speed_dial_change),
                () -> assign(key)));
        options.add(new AppBottomSheet.Option(R.drawable.ic_delete, getString(R.string.speed_dial_remove), () -> {
            SpeedDialService.clear(key);
            render();
        }));
        AppBottomSheet.showOptions(this, getString(R.string.speed_dial_key, String.valueOf(key)) + " · " + name, options);
    }

    private void assign(int key) {
        if (!ContactsService.canRead(this)) {
            permissionRequester.request(this, PermissionManager.Group.CONTACTS);
            return;
        }
        showFullscreen(AdScreens.SPEED_DIAL_ASSIGN, () -> ContactPickerSheet.show(this,
                getString(R.string.speed_dial_key, String.valueOf(key)), false, true, null, contact -> {
                    SpeedDialService.set(key, contact.getDisplayName(), contact.primaryNumber);
                    HapticUtils.confirm(binding.listGroup);
                    Toast.makeText(this, getString(R.string.speed_dial_saved, String.valueOf(key),
                            contact.getDisplayName()), Toast.LENGTH_SHORT).show();
                    render();
                }));
    }
}
