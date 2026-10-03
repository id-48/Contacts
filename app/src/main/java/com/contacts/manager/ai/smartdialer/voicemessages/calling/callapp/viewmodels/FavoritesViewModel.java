package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.viewmodels;

import android.app.Application;
import android.provider.ContactsContract;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;

import java.util.List;

public class FavoritesViewModel extends ObservingViewModel {

    private final MutableLiveData<List<ContactModel>> favorites = new MutableLiveData<>();

    public FavoritesViewModel(@NonNull Application application) {
        super(application);
        observe(ContactsContract.Contacts.CONTENT_URI);
    }

    public LiveData<List<ContactModel>> getFavorites() {
        return favorites;
    }

    @Override
    protected void load() {
        favorites.postValue(ContactsService.getFavorites(getApplication()));
    }
}
