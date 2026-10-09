package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.dialer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import android.transition.ChangeBounds;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.AppBottomSheet;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.DialPadView;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.PersonRowAdapter;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.IntentKeys;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityDialerBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.CallModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.callhistory.CallHistoryActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.editcontact.EditContactActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.CallLogService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PhoneService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.SpeedDialService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.WhatsAppService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.IntentUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.viewmodels.DialerViewModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.viewmodels.SearchViewModel;

import java.util.ArrayList;
import java.util.List;

public class DialerActivity extends BaseActivity implements DialPadView.Listener, PersonRowAdapter.Listener {

    private static final String STATE_NUMBER = "number";

    private ActivityDialerBinding binding;
    private DialerViewModel viewModel;
    private PersonRowAdapter adapter;
    private final StringBuilder number = new StringBuilder();
    private ToneGenerator toneGenerator;
    private boolean exactMatch;
    private boolean dialPadHidden;

    public static Intent intent(Context context, String number) {
        return new Intent(context, DialerActivity.class).putExtra(IntentKeys.EXTRA_NUMBER, number);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDialerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);

        viewModel = new ViewModelProvider(this).get(DialerViewModel.class);
        adapter = new PersonRowAdapter(this);
        binding.suggestionList.setLayoutManager(new LinearLayoutManager(this));
        binding.suggestionList.setAdapter(adapter);
        binding.suggestionList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    setDialPadVisible(false);
                }
            }
        });
        binding.dialpadFab.setOnClickListener(v -> {
            HapticUtils.tap(v);
            setDialPadVisible(true);
        });

        setupBack(binding.backButton);
        setupBackAd(AdScreens.DIALER_BACK);
        binding.dialPad.setListener(this);
        binding.callButton.setOnClickListener(v -> placeCall());
        binding.backspaceButton.setOnClickListener(v -> {
            HapticUtils.tap(v);
            if (number.length() > 0) {
                number.deleteCharAt(number.length() - 1);
                onNumberChanged();
            }
        });
        binding.backspaceButton.setOnLongClickListener(v -> {
            HapticUtils.longPress(v);
            number.setLength(0);
            onNumberChanged();
            return true;
        });
        binding.addContactLink.setOnClickListener(v ->
                startActivity(EditContactActivity.createIntent(this, number.toString())));
        binding.dialNumber.setOnLongClickListener(v -> {
            showMenu();
            return true;
        });

        if (savedInstanceState != null) {
            number.append(savedInstanceState.getString(STATE_NUMBER, ""));
        } else {
            readIntent(getIntent());
        }

        viewModel.getResult().observe(this, this::renderResult);
        onNumberChanged();
        viewModel.loadIfNeeded();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        readIntent(intent);
        onNumberChanged();
    }

    private void readIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        String value = intent.getStringExtra(IntentKeys.EXTRA_NUMBER);
        Uri data = intent.getData();
        if (data != null && "tel".equals(data.getScheme())) {
            value = data.getSchemeSpecificPart();
        }
        if (!TextUtils.isEmpty(value)) {
            number.setLength(0);
            for (char c : value.toCharArray()) {
                if (Character.isDigit(c) || c == '+' || c == '*' || c == '#' || c == ',' || c == ';') {
                    number.append(c);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean tones = StorageService.isDialpadSoundEnabled();
        if (tones && toneGenerator == null) {
            try {
                toneGenerator = new ToneGenerator(AudioManager.STREAM_DTMF, 70);
            } catch (RuntimeException ignored) {
                toneGenerator = null;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_NUMBER, number.toString());
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.hold, R.anim.sheet_exit);
    }

    @Override
    public void onKey(char key) {
        number.append(key);
        playTone(key);
        onNumberChanged();
    }

    @Override
    public boolean onKeyLongPress(char key) {
        if (key == '0') {
            number.append('+');
            onNumberChanged();
            return true;
        }
        if (number.length() == 0 && key >= '1' && key <= '9') {
            SpeedDialService.Entry entry = SpeedDialService.get(key - '0');
            if (entry != null) {
                PhoneService.call(this, entry.number);
            } else {
                Toast.makeText(this, getString(R.string.speed_dial_not_set_hint, String.valueOf(key)),
                        Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        char c = (char) event.getUnicodeChar();
        if ((c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+') {
            onKey(c);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DEL && number.length() > 0) {
            number.deleteCharAt(number.length() - 1);
            onNumberChanged();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_CALL) {
            placeCall();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void playTone(char key) {
        if (toneGenerator == null) {
            return;
        }
        int tone;
        if (key >= '0' && key <= '9') {
            tone = ToneGenerator.TONE_DTMF_0 + (key - '0');
        } else if (key == '*') {
            tone = ToneGenerator.TONE_DTMF_S;
        } else if (key == '#') {
            tone = ToneGenerator.TONE_DTMF_P;
        } else {
            return;
        }
        toneGenerator.startTone(tone, 120);
    }

    private void onNumberChanged() {
        String value = number.toString();
        binding.dialNumber.setText(formatForDisplay(value));
        binding.dialNumber.setContentDescription(value.isEmpty() ? getString(R.string.enter_number) : value);
        binding.backspaceButton.setVisibility(value.isEmpty() ? View.INVISIBLE : View.VISIBLE);
        viewModel.setQuery(value);
        if (value.isEmpty()) {
            binding.addContactLink.setVisibility(View.INVISIBLE);
        }
    }

    private void setDialPadVisible(boolean visible) {
        if (dialPadHidden != visible) {
            return;
        }
        dialPadHidden = !visible;
        TransitionSet transition = new TransitionSet()
                .addTransition(new Slide(Gravity.BOTTOM).addTarget(binding.dialPanel))
                .addTransition(new ChangeBounds().addTarget(binding.suggestionList))
                .setDuration(280)
                .setInterpolator(new PathInterpolator(0.4f, 0f, 0.2f, 1f));
        TransitionManager.beginDelayedTransition(binding.content, transition);
        binding.dialPanel.setVisibility(visible ? View.VISIBLE : View.GONE);

        View fab = binding.dialpadFab;
        fab.animate().cancel();
        if (visible) {
            fab.animate().scaleX(0.6f).scaleY(0.6f).alpha(0f)
                    .setStartDelay(0)
                    .setDuration(150)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> fab.setVisibility(View.GONE))
                    .start();
        } else {
            fab.setVisibility(View.VISIBLE);
            fab.setScaleX(0.6f);
            fab.setScaleY(0.6f);
            fab.setAlpha(0f);
            fab.animate().scaleX(1f).scaleY(1f).alpha(1f)
                    .setStartDelay(140)
                    .setDuration(280)
                    .setInterpolator(new OvershootInterpolator(2f))
                    .start();
        }
    }

    private String formatForDisplay(String value) {
        if (value.contains("*") || value.contains("#") || value.contains(",") || value.length() < 5) {
            return value;
        }
        return PhoneUtils.format(this, value);
    }

    private void renderResult(SearchViewModel.Result result) {
        List<PersonRowAdapter.Item> items = new ArrayList<>();
        exactMatch = false;
        if (!result.contacts.isEmpty()) {
            items.add(PersonRowAdapter.Item.header(getString(R.string.section_contacts)));
            for (ContactModel contact : result.contacts) {
                if (PhoneUtils.sameNumber(contact.primaryNumber, result.query)) {
                    exactMatch = true;
                }
                items.add(PersonRowAdapter.Item.contact(contact, PhoneUtils.format(this, contact.primaryNumber))
                        .withAction());
            }
        }
        if (!result.recents.isEmpty()) {
            items.add(PersonRowAdapter.Item.header(getString(R.string.section_recent)));
            for (CallModel call : result.recents) {
                if (call.isSavedContact() && PhoneUtils.sameNumber(call.number, result.query)) {
                    exactMatch = true;
                }
                ContactModel contact = null;
                if (call.isSavedContact()) {
                    contact = new ContactModel();
                    contact.id = call.contactId;
                    contact.name = call.name;
                }
                String formatted = PhoneUtils.format(this, call.number);
                items.add(PersonRowAdapter.Item.number("r:" + call.id,
                        call.isSavedContact() ? call.name : formatted,
                        call.isSavedContact() ? formatted : getString(CallLogService.typeLabel(call.type)),
                        call.photoUri, call.number, contact).withAction());
            }
        }
        adapter.submitList(items);
        boolean showAdd = !result.query.isEmpty() && result.query.equals(number.toString())
                && !exactMatch && PhoneUtils.isValidNumber(result.query) && ContactsService.canWrite(this);
        binding.addContactLink.setVisibility(showAdd ? View.VISIBLE : View.INVISIBLE);
    }

    private void placeCall() {
        HapticUtils.confirm(binding.callButton);
        if (number.length() == 0) {
            SearchViewModel.Result result = viewModel.getResult().getValue();
            if (result != null && !result.recents.isEmpty()) {
                for (CallModel call : result.recents) {
                    if (call.type == android.provider.CallLog.Calls.OUTGOING_TYPE) {
                        number.append(call.number);
                        onNumberChanged();
                        return;
                    }
                }
            }
            return;
        }
        PhoneService.call(this, number.toString());
    }

    private void showMenu() {
        String value = number.toString();
        List<AppBottomSheet.Option> options = new ArrayList<>();
        if (PhoneUtils.isValidNumber(value)) {
            if (ContactsService.canWrite(this)) {
                options.add(new AppBottomSheet.Option(R.drawable.ic_person_add, getString(R.string.create_contact),
                        () -> startActivity(EditContactActivity.createIntent(this, value))));
            }
            options.add(new AppBottomSheet.Option(R.drawable.ic_chat, getString(R.string.send_message),
                    () -> IntentUtils.openSms(this, value)));
            options.add(new AppBottomSheet.Option(R.drawable.ic_history, getString(R.string.call_history),
                    () -> startActivity(CallHistoryActivity.intent(this, value))));
            if (WhatsAppService.isInstalled(this)) {
                options.add(new AppBottomSheet.Option(R.drawable.ic_videocam, getString(R.string.video_call),
                        () -> WhatsAppService.videoCall(this, value)));
            }
        }
        String clip = clipboardNumber();
        if (clip != null) {
            options.add(new AppBottomSheet.Option(R.drawable.ic_content_copy, getString(R.string.paste), () -> {
                number.setLength(0);
                number.append(clip);
                onNumberChanged();
                setDialPadVisible(true);
            }));
        }
        if (value.length() > 0) {
            options.add(new AppBottomSheet.Option(R.drawable.ic_close, getString(R.string.clear), () -> {
                number.setLength(0);
                onNumberChanged();
            }));
        }
        if (options.isEmpty()) {
            return;
        }
        AppBottomSheet.showOptions(this, null, options);
    }

    private String clipboardNumber() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            return null;
        }
        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0 || clip.getItemAt(0).getText() == null) {
            return null;
        }
        String text = clip.getItemAt(0).getText().toString();
        StringBuilder digits = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isDigit(c) || (c == '+' && digits.length() == 0) || c == '*' || c == '#') {
                digits.append(c);
            }
        }
        return PhoneUtils.isValidNumber(digits.toString()) ? digits.toString() : null;
    }

    @Override
    public void onClick(PersonRowAdapter.Item item) {
        PhoneService.call(this, item.number);
    }

    @Override
    public void onAction(PersonRowAdapter.Item item) {
        PhoneService.call(this, item.number);
    }
}
