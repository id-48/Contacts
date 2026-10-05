package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.launcher;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityLauncherBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemLauncherAppBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.OnboardingFlow;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.RemoteConfigManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.aftercall.AfterCallActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.call.CallActivity;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Grid of launchable apps, used as the phone's home screen while Launcher Mode is on. */
public class LauncherActivity extends BaseActivity {

    private static final int COLUMNS = 4;
    private static final long CONFIG_MAX_AGE_MS = 60_000L;

    private ActivityLauncherBinding binding;
    private final AppAdapter adapter = new AppAdapter();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private static final class AppEntry {
        final String label;
        final ComponentName component;
        final Drawable icon;
        final boolean system;

        AppEntry(String label, ComponentName component, Drawable icon, boolean system) {
            this.label = label;
            this.component = component;
            this.icon = icon;
            this.system = system;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLauncherBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);

        binding.topBar.backButton.setVisibility(View.GONE);
        binding.topBar.topTitle.setText(R.string.launcher_title);
        binding.topBar.topAction.setVisibility(View.VISIBLE);
        binding.topBar.topAction.setText(R.string.launcher_open_contacts);
        binding.topBar.topAction.setOnClickListener(v -> openContacts());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });

        binding.appGrid.setLayoutManager(new GridLayoutManager(this, COLUMNS));
        binding.appGrid.setAdapter(adapter);
        AdsManager.showNativeSmall(this, binding.nativeSmallContainer, AdScreens.LAUNCHER);
        if (LauncherMode.shouldReturnToApp()) returnToApp();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (LauncherMode.shouldReturnToApp()) returnToApp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        RemoteConfigManager.refreshIfOlderThan(CONFIG_MAX_AGE_MS);
        if (!LauncherMode.isEnabled()) {
            leaveHome();
            return;
        }
        loadApps();
    }

    private void leaveHome() {
        LauncherMode.sync(this);
        finish();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        main.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void loadApps() {
        if (adapter.getItemCount() == 0) binding.appProgress.setVisibility(View.VISIBLE);
        PackageManager pm = getPackageManager();
        executor.execute(() -> {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> infos = pm.queryIntentActivities(query, 0);
            List<AppEntry> apps = new ArrayList<>();
            for (ResolveInfo info : infos) {
                if (info.activityInfo == null) continue;
                ApplicationInfo appInfo = info.activityInfo.applicationInfo;
                boolean system = appInfo != null && (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                apps.add(new AppEntry(String.valueOf(info.loadLabel(pm)),
                        new ComponentName(info.activityInfo.packageName, info.activityInfo.name),
                        info.loadIcon(pm), system));
            }
            Collator collator = Collator.getInstance();
            apps.sort((a, b) -> collator.compare(a.label, b.label));
            main.post(() -> {
                if (isDestroyed()) return;
                binding.appProgress.setVisibility(View.GONE);
                binding.appEmpty.setVisibility(apps.isEmpty() ? View.VISIBLE : View.GONE);
                adapter.submit(apps);
            });
        });
    }

    private void returnToApp() {
        ActivityManager manager = getSystemService(ActivityManager.class);
        if (manager != null) {
            for (ActivityManager.AppTask task : manager.getAppTasks()) {
                try {
                    ComponentName base = task.getTaskInfo().baseIntent.getComponent();
                    if (base == null || isOwnSideTask(base.getClassName())) continue;
                    task.moveToFront();
                    return;
                } catch (RuntimeException ignored) {
                }
            }
        }
        openContacts();
    }

    private static boolean isOwnSideTask(String className) {
        return className.equals(LauncherActivity.class.getName())
                || className.equals(CallActivity.class.getName())
                || className.equals(AfterCallActivity.class.getName());
    }

    private void openContacts() {
        startActivity(OnboardingFlow.startIntent(this).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private void openApp(AppEntry app) {
        Intent intent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.no_app_found, Toast.LENGTH_SHORT).show();
        }
    }

    private void showActions(View anchor, AppEntry app) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, R.string.launcher_app_info);
        if (!app.system) menu.getMenu().add(0, 2, 1, R.string.launcher_uninstall);
        menu.setOnMenuItemClickListener(item -> {
            Uri uri = Uri.fromParts("package", app.component.getPackageName(), null);
            Intent intent = item.getItemId() == 1
                    ? new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
                    : new Intent(Intent.ACTION_DELETE, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException | SecurityException e) {
                Toast.makeText(this, R.string.no_app_found, Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        menu.show();
    }

    private class AppAdapter extends RecyclerView.Adapter<AppAdapter.Holder> {

        private final List<AppEntry> items = new ArrayList<>();

        void submit(List<AppEntry> apps) {
            items.clear();
            items.addAll(apps);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemLauncherAppBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            AppEntry app = items.get(position);
            holder.binding.appIcon.setImageDrawable(app.icon);
            holder.binding.appLabel.setText(app.label);
            holder.itemView.setContentDescription(app.label);
            holder.itemView.setOnClickListener(v -> showFullscreen(AdScreens.LAUNCHER_APP, () -> openApp(app)));
            holder.itemView.setOnLongClickListener(v -> {
                showActions(v, app);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final ItemLauncherAppBinding binding;

            Holder(ItemLauncherAppBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
