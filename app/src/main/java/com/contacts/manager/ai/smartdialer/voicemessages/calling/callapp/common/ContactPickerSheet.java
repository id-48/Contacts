package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.analytics.Analytics;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.SheetPickFavoriteBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ContactPickerSheet {

    public interface Listener {
        void onPicked(ContactModel contact);
    }

    public interface Filter {
        boolean accept(ContactModel contact);
    }

    private ContactPickerSheet() {
    }

    public static void show(Activity activity, CharSequence title, boolean includeHidden, boolean requireNumber,
                            @Nullable Filter filter, Listener listener) {
        Context app = activity.getApplicationContext();
        AppExecutors.io(() -> {
            List<ContactModel> candidates = new ArrayList<>();
            for (ContactModel contact : ContactsService.getContacts(app, includeHidden)) {
                if (requireNumber && !PhoneUtils.isValidNumber(contact.primaryNumber)) {
                    continue;
                }
                if (filter == null || filter.accept(contact)) {
                    candidates.add(contact);
                }
            }
            AppExecutors.main(() -> {
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    present(activity, title, candidates, listener);
                }
            });
        });
    }

    private static void present(Activity context, CharSequence title, List<ContactModel> candidates, Listener listener) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        SheetPickFavoriteBinding sheet = SheetPickFavoriteBinding.inflate(LayoutInflater.from(context));
        sheet.pickTitle.setText(title);
        PersonRowAdapter adapter = new PersonRowAdapter(item -> {
            dialog.dismiss();
            listener.onPicked(item.contact);
        });
        List<PersonRowAdapter.Item> rows = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<String> numbers = new ArrayList<>();
        for (ContactModel contact : candidates) {
            String formatted = PhoneUtils.isValidNumber(contact.primaryNumber)
                    ? PhoneUtils.format(context, contact.primaryNumber) : null;
            boolean named = contact.name != null && contact.name.matches(".*\\p{L}.*");
            String rowTitle = named || formatted == null ? contact.getDisplayName() : formatted;
            rows.add(PersonRowAdapter.Item.number("c:" + contact.id, rowTitle, named ? formatted : null,
                    contact.photoUri, contact.primaryNumber, contact));
            names.add(fold(contact.getDisplayName()));
            numbers.add(digitsOf(contact.primaryNumber));
        }
        sheet.pickList.setLayoutManager(new LinearLayoutManager(context));
        sheet.pickList.setAdapter(adapter);
        adapter.submitList(rows);
        sheet.pickEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);

        int rowHeight = context.getResources().getDimensionPixelSize(R.dimen.row_min_height);
        int maxHeight = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.55f);
        ViewGroup.LayoutParams containerParams = sheet.pickListContainer.getLayoutParams();
        containerParams.height = Math.min(Math.max(rows.size(), 3) * rowHeight, maxHeight);
        sheet.pickListContainer.setLayoutParams(containerParams);
        ViewGroup.LayoutParams listParams = sheet.pickList.getLayoutParams();
        listParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        sheet.pickList.setLayoutParams(listParams);

        sheet.pickSearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                sheet.pickSearchClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                String folded = fold(query);
                String digits = digitsOf(query);
                boolean numeric = !digits.isEmpty() && query.matches("[+\\d\\s()\\-]+");
                List<PersonRowAdapter.Item> filtered = new ArrayList<>();
                for (int i = 0; i < rows.size(); i++) {
                    if (folded.isEmpty() || names.get(i).contains(folded)
                            || (numeric && numbers.get(i).contains(digits))) {
                        filtered.add(rows.get(i));
                    }
                }
                adapter.submitList(filtered, () -> sheet.pickList.scrollToPosition(0));
                sheet.pickEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
        sheet.pickSearchClear.setOnClickListener(v -> sheet.pickSearchField.setText(null));
        sheet.pickList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    InputMethodManager imm = (InputMethodManager) recyclerView.getContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(recyclerView.getWindowToken(), 0);
                    }
                }
            }
        });
        dialog.setContentView(sheet.getRoot());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.getBehavior().setSkipCollapsed(true);
        Analytics.trackDialog(dialog, "contact_picker_sheet");
        dialog.show();
    }

    private static String fold(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.getDefault()).trim();
    }

    private static String digitsOf(@Nullable String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}
