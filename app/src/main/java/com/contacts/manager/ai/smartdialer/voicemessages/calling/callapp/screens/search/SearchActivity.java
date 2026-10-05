package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.AppConstants;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivitySearchBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.CallModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.callhistory.CallHistoryActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.contactdetails.ContactDetailsActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.editcontact.EditContactActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.CallLogService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PhoneService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.DateUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.viewmodels.SearchViewModel;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends BaseActivity implements SearchAdapter.Listener {

    private static final int MIN_DIAL_DIGITS = 3;

    private ActivitySearchBinding binding;
    private SearchViewModel viewModel;
    private SearchAdapter adapter;
    private String lastQuery;
    private final Runnable searchRunnable = this::submitQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);

        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        adapter = new SearchAdapter(this);
        binding.resultList.setLayoutManager(new LinearLayoutManager(this));
        binding.resultList.setAdapter(adapter);
        binding.resultList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    hideKeyboard();
                    binding.searchField.clearFocus();
                }
            }
        });

        setupBack(binding.backButton);
        setupBackAd(AdScreens.SEARCH_BACK);
        AdsManager.showNativeSmall(this, binding.nativeSmallContainer, AdScreens.SEARCH);
        binding.searchBar.setOnClickListener(v -> showKeyboard());
        binding.clearButton.setOnClickListener(v -> {
            binding.searchField.setText("");
            showKeyboard();
        });
        binding.searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                toggleClear(s.length() > 0);
                AppExecutors.mainHandler().removeCallbacks(searchRunnable);
                AppExecutors.mainHandler().postDelayed(searchRunnable, AppConstants.SEARCH_DEBOUNCE_MS);
            }
        });
        binding.searchField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                AppExecutors.mainHandler().removeCallbacks(searchRunnable);
                submitQuery();
                hideKeyboard();
                return true;
            }
            return false;
        });

        viewModel.getResult().observe(this, this::render);
        viewModel.loadIfNeeded();
        binding.searchField.requestFocus();
        if (savedInstanceState == null) {
            animateIn();
        }
    }

    @Override
    protected void onDestroy() {
        AppExecutors.mainHandler().removeCallbacks(searchRunnable);
        super.onDestroy();
    }

    private void animateIn() {
        float offset = getResources().getDimension(R.dimen.space_24);
        binding.searchBar.setAlpha(0f);
        binding.searchBar.setTranslationX(offset);
        binding.searchBar.animate().alpha(1f).translationX(0f).setDuration(260)
                .setInterpolator(new DecelerateInterpolator(1.6f)).start();
        binding.resultList.setAlpha(0f);
        binding.resultList.setTranslationY(offset);
        binding.resultList.animate().alpha(1f).translationY(0f).setStartDelay(80).setDuration(300)
                .setInterpolator(new DecelerateInterpolator(1.6f)).start();
    }

    private void toggleClear(boolean show) {
        View clear = binding.clearButton;
        if (show == (clear.getVisibility() == View.VISIBLE)) {
            return;
        }
        clear.animate().cancel();
        if (show) {
            clear.setVisibility(View.VISIBLE);
            clear.setAlpha(0f);
            clear.setScaleX(0.6f);
            clear.setScaleY(0.6f);
            clear.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(160).start();
        } else {
            clear.animate().alpha(0f).scaleX(0.6f).scaleY(0.6f).setDuration(120)
                    .withEndAction(() -> clear.setVisibility(View.GONE)).start();
        }
    }

    private void submitQuery() {
        if (binding != null) {
            viewModel.setQuery(binding.searchField.getText().toString());
        }
    }

    private void showKeyboard() {
        binding.searchField.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(binding.searchField, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(binding.searchField.getWindowToken(), 0);
        }
    }

    private void render(SearchViewModel.Result result) {
        String query = result.query.trim();
        List<SearchAdapter.Item> items = new ArrayList<>();
        if (query.isEmpty()) {
            if (!result.recents.isEmpty()) {
                List<SearchAdapter.Item> people = new ArrayList<>();
                for (CallModel call : result.recents) {
                    people.add(recentItem(call, query));
                }
                items.add(SearchAdapter.Item.header(getString(R.string.section_recent), 0));
                items.add(SearchAdapter.Item.carousel(people));
            }
            items.add(SearchAdapter.Item.hint());
        } else {
            if (looksLikeNumber(query)) {
                String formatted = PhoneUtils.format(this, query);
                items.add(SearchAdapter.Item.action(SearchAdapter.ACTION_CALL,
                        getString(R.string.call_name, formatted), query));
                items.add(SearchAdapter.Item.action(SearchAdapter.ACTION_CREATE,
                        getString(R.string.create_contact), query));
            }
            if (!result.recents.isEmpty()) {
                items.add(SearchAdapter.Item.header(getString(R.string.section_recent), result.recents.size()));
                for (CallModel call : result.recents) {
                    items.add(recentItem(call, query));
                }
            }
            if (!result.contacts.isEmpty()) {
                items.add(SearchAdapter.Item.header(getString(R.string.section_contacts), result.contacts.size()));
                for (ContactModel contact : result.contacts) {
                    items.add(SearchAdapter.Item.row("c:" + contact.id, contact.name,
                            PhoneUtils.format(this, contact.primaryNumber), contact.name, contact.photoUri,
                            contact.primaryNumber, contact).withQuery(query));
                }
            }
        }

        boolean queryChanged = !TextUtils.equals(query, lastQuery);
        lastQuery = query;
        adapter.submitList(items, () -> {
            if (queryChanged && binding != null) {
                binding.resultList.scrollToPosition(0);
            }
        });

        boolean noAccess = !ContactsService.canRead(this) && !CallLogService.canRead(this);
        if (noAccess) {
            binding.emptyState.setContent(R.drawable.ic_search, R.string.search_no_access_title, R.string.search_no_access_body);
            binding.emptyState.show(true);
        } else if (items.isEmpty()) {
            binding.emptyState.setContent(R.drawable.ic_search, R.string.search_empty_title, R.string.search_empty_body);
            binding.emptyState.show(true);
        } else {
            binding.emptyState.show(false);
        }
    }

    private SearchAdapter.Item recentItem(CallModel call, String query) {
        ContactModel contact = null;
        if (call.isSavedContact()) {
            contact = new ContactModel();
            contact.id = call.contactId;
            contact.lookupKey = call.lookupKey;
            contact.name = call.name;
        }
        String formatted = PhoneUtils.format(this, call.number);
        String meta = getString(R.string.recent_meta,
                getString(CallLogService.typeLabelLong(call.type)), DateUtils.agoLabel(this, call.date));
        boolean showNumber = call.isSavedContact() && query.matches(".*\\d.*");
        SearchAdapter.Item item = SearchAdapter.Item.row("r:" + call.id,
                call.isSavedContact() ? call.name : formatted, showNumber ? formatted : meta,
                call.isSavedContact() ? call.name : null, call.photoUri, call.number, contact);
        if (!showNumber) {
            item.withType(CallLogService.typeArrowIcon(call.type), CallLogService.typeColor(call.type), call.isMissed());
        }
        return item.withQuery(query);
    }

    private static boolean looksLikeNumber(String query) {
        int digits = 0;
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (Character.isDigit(c)) {
                digits++;
            } else if ("+-() *#.".indexOf(c) < 0) {
                return false;
            }
        }
        return digits >= MIN_DIAL_DIGITS;
    }

    @Override
    public void onOpen(SearchAdapter.Item item) {
        if (item.contact != null && item.contact.id > 0) {
            startActivity(ContactDetailsActivity.intent(this, item.contact.id, item.contact.lookupKey));
        } else {
            startActivity(CallHistoryActivity.intent(this, item.number));
        }
    }

    @Override
    public void onCall(SearchAdapter.Item item) {
        PhoneService.call(this, item.number);
    }

    @Override
    public void onQuickAction(SearchAdapter.Item item) {
        if (item.action == SearchAdapter.ACTION_CALL) {
            PhoneService.call(this, item.number);
        } else {
            startActivity(EditContactActivity.createIntent(this, item.number));
        }
    }
}
