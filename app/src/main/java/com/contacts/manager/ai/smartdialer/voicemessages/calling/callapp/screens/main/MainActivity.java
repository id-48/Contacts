package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.IntentKeys;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityMainBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.IncludeNavItemBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.contacts.ContactsFragment;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.dialer.DialerActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.editcontact.EditContactActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.favorites.FavoritesFragment;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.recent.RecentFragment;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.search.SearchActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.settings.SettingsActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;

public class MainActivity extends BaseActivity {

    public interface Tab {
        void scrollToTop();
    }

    public static final int TAB_RECENT = 0;
    public static final int TAB_CONTACTS = 1;
    public static final int TAB_FAVORITES = 2;

    private static final String STATE_TAB = "tab";
    private static final int[] TITLES = {R.string.tab_recent, R.string.tab_contacts, R.string.tab_favorites};
    private static final int[] ICONS = {R.drawable.ic_schedule, R.drawable.ic_account_circle, R.drawable.ic_star};
    private static final int[] ICONS_SELECTED = {R.drawable.ic_schedule_filled, R.drawable.ic_account_circle_filled,
            R.drawable.ic_star_filled};

    private ActivityMainBinding binding;
    private IncludeNavItemBinding[] navItems;
    private int currentTab = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);

        navItems = new IncludeNavItemBinding[]{binding.navRecent, binding.navContacts, binding.navFavorites};
        for (int i = 0; i < navItems.length; i++) {
            int tab = i;
            navItems[i].navLabel.setText(TITLES[i]);
            navItems[i].navIcon.setImageResource(ICONS[i]);
            navItems[i].navItem.setContentDescription(getString(TITLES[i]));
            navItems[i].navItem.setOnClickListener(v -> {
                HapticUtils.tick(v);
                selectTab(tab);
            });
        }

        binding.searchButton.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        binding.settingsButton.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        binding.fab.setOnClickListener(v -> {
            HapticUtils.tap(v);
            if (currentTab == TAB_CONTACTS) {
                startActivity(EditContactActivity.createIntent(this, null));
            } else {
                startActivity(new Intent(this, DialerActivity.class));
                overridePendingTransition(R.anim.sheet_enter, R.anim.hold);
            }
        });

        int tab = TAB_RECENT;
        if (savedInstanceState != null) {
            tab = savedInstanceState.getInt(STATE_TAB, TAB_RECENT);
        } else if (getIntent() != null) {
            tab = getIntent().getIntExtra(IntentKeys.EXTRA_TAB, TAB_RECENT);
        }
        selectTab(tab);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (currentTab != TAB_RECENT) {
                    selectTab(TAB_RECENT);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.hasExtra(IntentKeys.EXTRA_TAB)) {
            selectTab(intent.getIntExtra(IntentKeys.EXTRA_TAB, TAB_RECENT));
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TAB, currentTab);
    }

    public void selectTab(int tab) {
        if (tab < 0 || tab > 2) {
            tab = TAB_RECENT;
        }
        FragmentManager fm = getSupportFragmentManager();
        if (tab == currentTab) {
            Fragment fragment = fm.findFragmentByTag(tag(tab));
            if (fragment instanceof Tab) {
                ((Tab) fragment).scrollToTop();
            }
            return;
        }
        FragmentTransaction transaction = fm.beginTransaction().setReorderingAllowed(true);
        if (currentTab >= 0) {
            transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        }
        for (int i = 0; i < 3; i++) {
            Fragment existing = fm.findFragmentByTag(tag(i));
            if (i == tab) {
                if (existing == null) {
                    transaction.add(R.id.fragmentContainer, createFragment(i), tag(i));
                } else {
                    transaction.show(existing);
                }
            } else if (existing != null) {
                transaction.hide(existing);
            }
        }
        transaction.commitNow();
        boolean fabChanged = currentTab >= 0 && (tab == TAB_CONTACTS) != (currentTab == TAB_CONTACTS);
        currentTab = tab;
        binding.mainTitle.setText(TITLES[tab]);
        for (int i = 0; i < navItems.length; i++) {
            boolean selected = i == tab;
            navItems[i].navItem.setSelected(selected);
            navItems[i].navIcon.setImageResource(selected ? ICONS_SELECTED[i] : ICONS[i]);
        }
        updateFab(fabChanged);
    }

    private void updateFab(boolean animate) {
        boolean contacts = currentTab == TAB_CONTACTS;
        int icon = contacts ? R.drawable.ic_add : R.drawable.ic_dialpad_filled;
        binding.fab.setContentDescription(getString(contacts ? R.string.create_contact : R.string.open_dialpad));
        if (!animate) {
            binding.fabIcon.setImageResource(icon);
            return;
        }
        View fabIcon = binding.fabIcon;
        fabIcon.animate().cancel();
        fabIcon.animate().scaleX(0.4f).scaleY(0.4f).alpha(0f).rotation(contacts ? 90f : -90f).setDuration(110)
                .withEndAction(() -> {
                    binding.fabIcon.setImageResource(icon);
                    fabIcon.setRotation(contacts ? -90f : 90f);
                    fabIcon.animate().scaleX(1f).scaleY(1f).alpha(1f).rotation(0f).setDuration(160).start();
                }).start();
    }

    private Fragment createFragment(int tab) {
        switch (tab) {
            case TAB_CONTACTS:
                return new ContactsFragment();
            case TAB_FAVORITES:
                return new FavoritesFragment();
            default:
                return new RecentFragment();
        }
    }

    private static String tag(int tab) {
        return "tab_" + tab;
    }
}
