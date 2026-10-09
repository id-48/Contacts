package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.viewmodels;

import android.app.Application;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.AppConstants;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.CallModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.CallLogService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SearchViewModel extends ObservingViewModel {

    public static class Result {
        public final String query;
        public final List<CallModel> recents;
        public final List<ContactModel> contacts;

        Result(String query, List<CallModel> recents, List<ContactModel> contacts) {
            this.query = query;
            this.recents = recents;
            this.contacts = contacts;
        }
    }

    private static final int RECENT_SOURCE_LIMIT = 300;
    private static final int EMPTY_RECENT_LIMIT = 8;
    private static final int MATCH_RECENT_LIMIT = 5;

    private final MutableLiveData<Result> result = new MutableLiveData<>();
    private volatile List<ContactModel> entries = Collections.emptyList();
    private volatile List<CallModel> recentCalls = Collections.emptyList();
    private volatile String query = "";
    private volatile int generation;

    public SearchViewModel(@NonNull Application application) {
        super(application);
        observe(ContactsContract.Contacts.CONTENT_URI, CallLog.Calls.CONTENT_URI);
    }

    public LiveData<Result> getResult() {
        return result;
    }

    protected boolean isDialpad() {
        return false;
    }

    public void setQuery(String value) {
        query = value == null ? "" : value.trim();
        int current = ++generation;
        AppExecutors.io(() -> filter(current));
    }

    public String getQuery() {
        return query;
    }

    @Override
    protected void load() {
        List<ContactModel> loaded = new ArrayList<>(ContactsService.getNumberIndex(getApplication()).values());
        Set<String> hidden = StorageService.getVaultKeys();
        if (!hidden.isEmpty()) {
            loaded.removeIf(contact -> contact.lookupKey != null && hidden.contains(contact.lookupKey));
        }
        java.text.Collator collator = java.text.Collator.getInstance(Locale.getDefault());
        collator.setStrength(java.text.Collator.PRIMARY);
        Collections.sort(loaded, (a, b) -> collator.compare(a.getDisplayName(), b.getDisplayName()));
        entries = loaded;
        recentCalls = CallLogService.getRecentCalls(getApplication(), RECENT_SOURCE_LIMIT);
        filter(++generation);
    }

    private void filter(int current) {
        String q = query;
        List<CallModel> recents = new ArrayList<>();
        List<ContactModel> contacts = new ArrayList<>();
        Set<String> seenNumbers = new HashSet<>();
        if (q.isEmpty()) {
            for (CallModel call : recentCalls) {
                String key = PhoneUtils.key(call.number);
                if (PhoneUtils.isPrivate(call.number) || !seenNumbers.add(key.isEmpty() ? call.number : key)) {
                    continue;
                }
                recents.add(call);
                if (recents.size() >= EMPTY_RECENT_LIMIT) {
                    break;
                }
            }
        } else {
            String digits = digitsOf(q);
            String folded = fold(q);
            boolean dialpad = isDialpad();
            for (ContactModel contact : entries) {
                if (matches(contact.name, contact.primaryNumber, folded, digits, dialpad)) {
                    contacts.add(contact);
                    seenNumbers.add(PhoneUtils.key(contact.primaryNumber));
                    if (contacts.size() >= AppConstants.DIALER_SUGGESTION_LIMIT) {
                        break;
                    }
                }
            }
            Set<String> seenRecent = new HashSet<>();
            for (CallModel call : recentCalls) {
                if (PhoneUtils.isPrivate(call.number)) {
                    continue;
                }
                String key = PhoneUtils.key(call.number);
                if (dialpad && seenNumbers.contains(key)) {
                    continue;
                }
                if (!seenRecent.add(key.isEmpty() ? call.number : key)) {
                    continue;
                }
                if (matches(call.name, call.number, folded, digits, dialpad)) {
                    recents.add(call);
                    if (recents.size() >= MATCH_RECENT_LIMIT) {
                        break;
                    }
                }
            }
        }
        if (current == generation) {
            result.postValue(new Result(q, recents, contacts));
        }
    }

    private static boolean matches(String name, String number, String folded, String digits, boolean dialpad) {
        if (!digits.isEmpty() && digitsOf(number).contains(digits)) {
            return true;
        }
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        if (dialpad) {
            return PhoneUtils.matchesT9(name, digits);
        }
        String foldedName = fold(name);
        if (foldedName.startsWith(folded)) {
            return true;
        }
        for (String word : foldedName.split("\\s+")) {
            if (word.startsWith(folded)) {
                return true;
            }
        }
        return folded.length() > 2 && foldedName.contains(folded);
    }

    private static String digitsOf(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private static String fold(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.getDefault()).trim();
    }
}
