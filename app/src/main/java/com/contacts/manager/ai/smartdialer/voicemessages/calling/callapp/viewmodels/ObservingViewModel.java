package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.viewmodels;

import android.app.Application;
import android.database.ContentObserver;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;

public abstract class ObservingViewModel extends AndroidViewModel {

    private static final long RELOAD_DELAY_MS = 350;

    protected final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final Runnable reloadRunnable = this::reload;
    private final ContentObserver observer = new ContentObserver(AppExecutors.mainHandler()) {
        @Override
        public void onChange(boolean selfChange) {
            scheduleReload();
        }
    };
    private boolean observing;
    private boolean loaded;

    protected ObservingViewModel(@NonNull Application application) {
        super(application);
    }

    protected void observe(Uri... uris) {
        for (Uri uri : uris) {
            try {
                getApplication().getContentResolver().registerContentObserver(uri, true, observer);
                observing = true;
            } catch (SecurityException ignored) {
            }
        }
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void loadIfNeeded() {
        if (!loaded) {
            reload();
        }
    }

    public void reload() {
        loaded = true;
        AppExecutors.mainHandler().removeCallbacks(reloadRunnable);
        if (loading.getValue() == null || !loading.getValue()) {
            loading.setValue(true);
        }
        AppExecutors.io(() -> {
            load();
            AppExecutors.main(() -> loading.setValue(false));
        });
    }

    protected void scheduleReload() {
        AppExecutors.mainHandler().removeCallbacks(reloadRunnable);
        AppExecutors.mainHandler().postDelayed(reloadRunnable, RELOAD_DELAY_MS);
    }

    protected abstract void load();

    @Override
    protected void onCleared() {
        AppExecutors.mainHandler().removeCallbacks(reloadRunnable);
        if (observing) {
            getApplication().getContentResolver().unregisterContentObserver(observer);
        }
        super.onCleared();
    }
}
