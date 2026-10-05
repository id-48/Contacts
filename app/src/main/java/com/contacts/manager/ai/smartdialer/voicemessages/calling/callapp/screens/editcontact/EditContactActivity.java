package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.editcontact;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.animation.AnimatorInflater;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.util.Patterns;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.CycleInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.AppBottomSheet;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.ConfirmDialog;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.constants.IntentKeys;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityEditContactBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemEditValueBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemTypeOptionBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.PopupTypeMenuBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.ContactModel;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.models.LabeledValue;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.contactdetails.ContactDetailsActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.ContactsService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.AppExecutors;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.DateUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.PhoneUtils;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.viewmodels.EditContactViewModel;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class EditContactActivity extends BaseActivity {

    private static final int[] PHONE_TYPES = {Phone.TYPE_MOBILE, Phone.TYPE_HOME, Phone.TYPE_WORK, Phone.TYPE_MAIN,
            Phone.TYPE_OTHER};
    private static final int[] EMAIL_TYPES = {Email.TYPE_HOME, Email.TYPE_WORK, Email.TYPE_MOBILE, Email.TYPE_OTHER};

    private ActivityEditContactBinding binding;
    private EditContactViewModel viewModel;
    private ContactModel model;
    private final List<Row> phoneRows = new ArrayList<>();
    private final List<Row> emailRows = new ArrayList<>();
    private ContactsService.AccountOption selectedAccount;
    private List<ContactsService.AccountOption> accounts = new ArrayList<>();
    private String birthday;
    private boolean bound;
    private Uri captureUri;

    private static class Row {
        final ItemEditValueBinding binding;
        final LabeledValue value;
        final boolean phone;

        Row(ItemEditValueBinding binding, LabeledValue value, boolean phone) {
            this.binding = binding;
            this.value = value;
            this.phone = phone;
        }
    }

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    startCrop(uri);
                }
            });

    private final ActivityResultLauncher<Uri> takePicture =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (Boolean.TRUE.equals(success) && captureUri != null) {
                    startCrop(captureUri);
                }
            });

    private final ActivityResultLauncher<Intent> cropLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String path = result.getData().getStringExtra(IntentKeys.EXTRA_IMAGE_URI);
                    if (path != null) {
                        applyCroppedPhoto(new File(path));
                    }
                }
            });

    public static Intent createIntent(Context context, String number) {
        return new Intent(context, EditContactActivity.class).putExtra(IntentKeys.EXTRA_NUMBER, number);
    }

    public static Intent editIntent(Context context, long contactId) {
        return new Intent(context, EditContactActivity.class).putExtra(IntentKeys.EXTRA_CONTACT_ID, contactId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditContactBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);

        long contactId = getIntent().getLongExtra(IntentKeys.EXTRA_CONTACT_ID, -1);
        String number = getIntent().getStringExtra(IntentKeys.EXTRA_NUMBER);
        if (savedInstanceState != null && savedInstanceState.getString("capture") != null) {
            captureUri = Uri.parse(savedInstanceState.getString("capture"));
        }
        binding.editTitle.setText(contactId > 0 ? R.string.edit_contact : R.string.create_contact);
        binding.accountSelector.setVisibility(contactId > 0 ? View.GONE : View.VISIBLE);
        binding.scrollView.setVisibility(View.INVISIBLE);
        binding.progress.setVisibility(View.VISIBLE);

        if (!ContactsService.canWrite(this)) {
            Toast.makeText(this, R.string.perm_contacts_body, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(EditContactViewModel.class);
        viewModel.init(contactId, number);
        viewModel.getContact().observe(this, this::bind);
        viewModel.getAccounts().observe(this, list -> {
            accounts = list;
            if (selectedAccount == null && !list.isEmpty()) {
                selectedAccount = list.get(0);
            }
            updateAccountLabel();
        });
        viewModel.getSaving().observe(this, saving -> {
            boolean busy = Boolean.TRUE.equals(saving);
            binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
            binding.doneButton.setEnabled(!busy && hasContent());
        });
        viewModel.getSaved().observe(this, this::onSaved);

        binding.closeButton.setOnClickListener(v -> attemptClose());
        binding.doneButton.setOnClickListener(v -> save());
        binding.photoContainer.setStateListAnimator(AnimatorInflater.loadStateListAnimator(this, R.animator.press_scale));
        binding.photoContainer.setOnClickListener(v -> {
            HapticUtils.tap(v);
            showPhotoOptions();
        });
        binding.accountSelector.setOnClickListener(v -> showAccounts());
        binding.nameExpandButton.setOnClickListener(v -> toggleNameExtra());
        binding.addPhone.addLabel.setText(R.string.add_number);
        binding.addPhone.addRoot.setOnClickListener(v -> addRow(true, new LabeledValue("", Phone.TYPE_MOBILE, null), true));
        binding.addEmail.addLabel.setText(R.string.add_email);
        binding.addEmail.addRoot.setOnClickListener(v -> addRow(false, new LabeledValue("", Email.TYPE_HOME, null), true));
        binding.addAddress.addLabel.setText(R.string.add_address);
        binding.addAddress.addRoot.setOnClickListener(v -> {
            showAddress(true);
            animateRowIn(binding.addressRow);
            focus(binding.addressField);
            reveal(binding.addressRow);
        });
        binding.removeAddress.setOnClickListener(v -> {
            binding.addressField.setText("");
            showAddress(false);
        });
        binding.addBirthday.addLabel.setText(R.string.add_birthday);
        binding.addBirthday.addRoot.setOnClickListener(v -> pickBirthday());
        binding.birthdayRow.setOnClickListener(v -> pickBirthday());
        binding.removeBirthday.setOnClickListener(v -> setBirthday(null));

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateDone();
            }
        };
        for (EditText field : new EditText[]{binding.firstNameField, binding.middleNameField, binding.lastNameField,
                binding.prefixField, binding.suffixField, binding.companyField}) {
            field.addTextChangedListener(watcher);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                attemptClose();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bound) {
            collect();
        }
        if (captureUri != null) {
            outState.putString("capture", captureUri.toString());
        }
    }

    private void bind(ContactModel contact) {
        if (contact == null || bound) {
            return;
        }
        bound = true;
        model = contact;
        binding.firstNameField.setText(contact.firstName);
        binding.middleNameField.setText(contact.middleName);
        binding.lastNameField.setText(contact.lastName);
        binding.prefixField.setText(contact.prefix);
        binding.suffixField.setText(contact.suffix);
        if (!TextUtils.isEmpty(contact.middleName) || !TextUtils.isEmpty(contact.lastName)
                || !TextUtils.isEmpty(contact.prefix) || !TextUtils.isEmpty(contact.suffix)) {
            setNameExtraVisible(true);
        }
        binding.companyField.setText(contact.company);
        binding.jobTitleField.setText(contact.jobTitle);
        for (LabeledValue phone : contact.phones) {
            addRow(true, phone, false);
        }
        if (contact.phones.isEmpty()) {
            addRow(true, new LabeledValue("", Phone.TYPE_MOBILE, null), false);
        }
        for (LabeledValue email : contact.emails) {
            addRow(false, email, false);
        }
        binding.addressField.setText(contact.address);
        showAddress(!TextUtils.isEmpty(contact.address));
        setBirthday(contact.birthday);
        binding.nicknameField.setText(contact.nickname);
        binding.notesField.setText(contact.note);
        bindPhoto();

        binding.progress.setVisibility(View.GONE);
        binding.scrollView.setVisibility(View.VISIBLE);
        animateFormIn();
        binding.doneButton.setEnabled(hasContent());
        viewModel.setInitialSignature(signature());
        if (contact.id <= 0 && contact.phones.size() <= 1 && TextUtils.isEmpty(contact.firstName)) {
            focus(binding.firstNameField);
        }
    }

    private void animateFormIn() {
        float offset = dp(18);
        int order = 0;
        for (int i = 0; i < binding.formContent.getChildCount(); i++) {
            View child = binding.formContent.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) {
                continue;
            }
            child.setAlpha(0f);
            child.setTranslationY(offset);
            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(Math.min(order++, 8) * 35L)
                    .setDuration(320)
                    .setInterpolator(new DecelerateInterpolator(2f))
                    .start();
        }
    }

    private void bindPhoto() {
        if (model.photoBytes != null) {
            File file = new File(getCacheDir(), "photos/pending.jpg");
            if (file.exists()) {
                binding.photoView.bindFile(file);
            }
        } else if (!model.removePhoto && model.hasPhoto()) {
            binding.photoView.bind(model.name, model.fullPhotoUri != null ? model.fullPhotoUri : model.photoUri);
        } else {
            binding.photoView.bind(null, null);
        }
    }

    private void addRow(boolean phone, LabeledValue value, boolean requestFocus) {
        List<Row> rows = phone ? phoneRows : emailRows;
        android.widget.LinearLayout container = phone ? binding.phoneRows : binding.emailRows;
        ItemEditValueBinding row = ItemEditValueBinding.inflate(LayoutInflater.from(this), container, false);
        Row entry = new Row(row, value, phone);
        row.valueField.setSaveEnabled(false);
        row.valueField.setText(value.value);
        row.valueField.setHint(phone ? R.string.phone : R.string.email);
        row.valueField.setInputType(phone ? InputType.TYPE_CLASS_PHONE
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            row.valueField.setAutofillHints(phone ? View.AUTOFILL_HINT_PHONE : View.AUTOFILL_HINT_EMAIL_ADDRESS);
        }
        row.valueIcon.setImageResource(phone ? R.drawable.ic_call : R.drawable.ic_mail);
        if (phone) {
            row.valueField.addTextChangedListener(new PhoneNumberFormattingTextWatcher(PhoneUtils.countryIso(this)));
        }
        row.getRoot().setOnClickListener(v -> focus(row.valueField));
        row.valueField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                row.valueField.setError(null);
                updateDone();
            }
        });
        updateTypeLabel(entry);
        row.valueType.setOnClickListener(v -> showTypeMenu(entry));
        row.valueRemove.setOnClickListener(v -> {
            HapticUtils.tap(v);
            v.setEnabled(false);
            rows.remove(entry);
            updateDone();
            View root = row.getRoot();
            root.animate()
                    .alpha(0f)
                    .translationX(dp(32))
                    .setDuration(180)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> {
                        container.removeView(root);
                        refreshRowDecorations(rows);
                    })
                    .start();
        });
        rows.add(entry);
        container.addView(row.getRoot());
        refreshRowDecorations(rows);
        if (requestFocus) {
            HapticUtils.tap(container);
            animateRowIn(row.getRoot());
            focus(row.valueField);
            // The container's layout transition starts from zero height, which drops focus on the first frame.
            row.valueField.postDelayed(() -> {
                if (row.valueField.isAttachedToWindow() && !row.valueField.hasFocus()) {
                    focus(row.valueField);
                }
            }, 80);
            reveal(row.getRoot());
        }
    }

    private void animateRowIn(View view) {
        view.setAlpha(0f);
        view.setTranslationY(-dp(8));
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(240)
                .setInterpolator(new DecelerateInterpolator(2f))
                .start();
    }

    private void reveal(View view) {
        binding.scrollView.postDelayed(() -> {
            if (!view.isAttachedToWindow()) {
                return;
            }
            Rect rect = new Rect();
            view.getDrawingRect(rect);
            binding.scrollView.offsetDescendantRectToMyCoords(view, rect);
            int target = rect.bottom + (int) dp(96) - binding.scrollView.getHeight();
            if (target > binding.scrollView.getScrollY()) {
                binding.scrollView.smoothScrollTo(0, target);
            }
        }, 280);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private void refreshRowDecorations(List<Row> rows) {
        for (int i = 0; i < rows.size(); i++) {
            ItemEditValueBinding row = rows.get(i).binding;
            row.valueIcon.setVisibility(i == 0 ? View.VISIBLE : View.INVISIBLE);
            row.valueDivider.setVisibility(i == 0 ? View.GONE : View.VISIBLE);
        }
    }

    private void updateTypeLabel(Row row) {
        LabeledValue value = row.value;
        String label = row.phone ? ContactsService.phoneTypeLabel(this, value)
                : ContactsService.emailTypeLabel(this, value);
        row.binding.valueType.setText(label);
        row.binding.valueType.setContentDescription(getString(R.string.label_type, label));
    }

    private void showTypeMenu(Row row) {
        HapticUtils.tap(row.binding.valueType);
        PopupTypeMenuBinding menu = PopupTypeMenuBinding.inflate(LayoutInflater.from(this));
        PopupWindow popup = new PopupWindow(menu.getRoot(), ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        menu.typeMenuCard.setClipToOutline(true);
        int[] types = row.phone ? PHONE_TYPES : EMAIL_TYPES;
        boolean custom = row.value.type == Phone.TYPE_CUSTOM;
        for (int type : types) {
            LabeledValue probe = new LabeledValue("", type, null);
            ItemTypeOptionBinding option = ItemTypeOptionBinding.inflate(getLayoutInflater(), menu.typeMenuCard, false);
            option.typeOptionLabel.setText(row.phone ? ContactsService.phoneTypeLabel(this, probe)
                    : ContactsService.emailTypeLabel(this, probe));
            boolean selected = !custom && row.value.type == type;
            option.typeOption.setSelected(selected);
            option.typeOptionCheck.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
            option.typeOption.setOnClickListener(v -> {
                HapticUtils.tap(v);
                row.value.type = type;
                row.value.label = null;
                updateTypeLabel(row);
                popup.dismiss();
            });
            menu.typeMenuCard.addView(option.getRoot());
        }
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setAnimationStyle(R.style.Animation_App_PopupMenu);
        popup.setElevation(0f);

        View content = menu.getRoot();
        content.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(binding.root);
        boolean keyboardVisible = insets != null && insets.isVisible(WindowInsetsCompat.Type.ime());
        if (keyboardVisible) {
            hideKeyboard();
        }
        binding.root.postDelayed(() -> showTypeMenuBelow(row, popup, content), keyboardVisible ? 260 : 0);
    }

    private void showTypeMenuBelow(Row row, PopupWindow popup, View content) {
        View anchor = row.binding.valueType;
        if (isFinishing() || !anchor.isAttachedToWindow()) {
            return;
        }
        int gap = (int) dp(4);
        int margin = getResources().getDimensionPixelSize(R.dimen.space_16);
        int[] location = new int[2];
        anchor.getLocationInWindow(location);
        Rect visible = new Rect();
        binding.root.getWindowVisibleDisplayFrame(visible);
        int overflow = location[1] + anchor.getHeight() + gap + content.getMeasuredHeight() + margin - visible.bottom;
        int delay = 0;
        if (overflow > 0) {
            binding.scrollView.smoothScrollBy(0, overflow);
            delay = 220;
        }
        binding.root.postDelayed(() -> {
            if (isFinishing() || !anchor.isAttachedToWindow()) {
                return;
            }
            anchor.getLocationInWindow(location);
            int x = location[0] + anchor.getWidth() - content.getMeasuredWidth();
            int y = location[1] + anchor.getHeight() + gap;
            popup.showAtLocation(binding.root, Gravity.NO_GRAVITY, Math.max(margin, x), y);
        }, delay);
    }

    private void toggleNameExtra() {
        boolean visible = binding.nameExtra.getVisibility() != View.VISIBLE;
        HapticUtils.tap(binding.nameExpandButton);
        TransitionManager.beginDelayedTransition(binding.formContent, new AutoTransition()
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator(1.5f)));
        setNameExtraVisible(visible);
        if (visible && binding.firstNameField.hasFocus()) {
            binding.middleNameField.requestFocus();
        } else if (!visible && binding.nameExtra.findFocus() != null) {
            binding.firstNameField.requestFocus();
        }
    }

    private void setNameExtraVisible(boolean visible) {
        binding.nameExtra.setVisibility(visible ? View.VISIBLE : View.GONE);
        binding.nameExpandButton.animate().rotation(visible ? 180f : 0f).setDuration(200).start();
        binding.nameExpandButton.setContentDescription(getString(visible ? R.string.fewer_name_fields
                : R.string.more_name_fields));
    }

    private void showAddress(boolean visible) {
        binding.addressRow.setVisibility(visible ? View.VISIBLE : View.GONE);
        binding.addAddress.addRoot.setVisibility(visible ? View.GONE : View.VISIBLE);
    }

    private void setBirthday(String value) {
        birthday = TextUtils.isEmpty(value) ? null : value;
        binding.birthdayRow.setVisibility(birthday == null ? View.GONE : View.VISIBLE);
        binding.addBirthday.addRoot.setVisibility(birthday == null ? View.VISIBLE : View.GONE);
        binding.birthdayValue.setText(birthday == null ? null : DateUtils.birthdayLabel(birthday));
    }

    private void pickBirthday() {
        long selection = MaterialDatePicker.todayInUtcMilliseconds();
        if (birthday != null && !birthday.startsWith("--")) {
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = format.parse(birthday);
                if (date != null) {
                    selection = date.getTime();
                }
            } catch (Exception ignored) {
            }
        }
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.birthday)
                .setSelection(selection)
                .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
                .build();
        picker.addOnPositiveButtonClickListener(value -> {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(value);
            setBirthday(String.format(Locale.US, "%04d-%02d-%02d", calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH)));
        });
        picker.show(getSupportFragmentManager(), "birthday");
    }

    private void showAccounts() {
        if (accounts.size() <= 1) {
            return;
        }
        List<AppBottomSheet.Option> options = new ArrayList<>();
        for (ContactsService.AccountOption account : accounts) {
            int icon = account.type == null ? R.drawable.ic_sim_card : R.drawable.ic_account_circle;
            options.add(new AppBottomSheet.Option(icon, account.label, () -> {
                selectedAccount = account;
                updateAccountLabel();
            }).checked(account == selectedAccount));
        }
        AppBottomSheet.showOptions(this, getString(R.string.save_to_title), options);
    }

    private void updateAccountLabel() {
        if (selectedAccount == null) {
            binding.accountSelector.setText(getString(R.string.save_to, getString(R.string.device_account)));
        } else {
            binding.accountSelector.setText(getString(R.string.save_to, selectedAccount.label));
        }
        binding.accountSelector.setClickable(accounts.size() > 1);
        binding.accountSelector.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0,
                accounts.size() > 1 ? R.drawable.ic_expand_more : 0, 0);
    }

    private void showPhotoOptions() {
        List<AppBottomSheet.Option> options = new ArrayList<>();
        options.add(new AppBottomSheet.Option(R.drawable.ic_photo_camera, getString(R.string.take_photo), this::launchCamera));
        options.add(new AppBottomSheet.Option(R.drawable.ic_image, getString(R.string.choose_photo), () ->
                pickMedia.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build())));
        boolean hasPhoto = model != null && (model.photoBytes != null || (!model.removePhoto && model.hasPhoto()));
        if (hasPhoto) {
            options.add(new AppBottomSheet.Option(R.drawable.ic_delete, getString(R.string.remove_photo), () -> {
                model.photoBytes = null;
                model.removePhoto = true;
                bindPhoto();
            }));
        }
        AppBottomSheet.showOptions(this, getString(R.string.set_photo), options);
    }

    private void launchCamera() {
        try {
            File dir = new File(getCacheDir(), "photos");
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException();
            }
            File file = new File(dir, "capture.jpg");
            captureUri = FileProvider.getUriForFile(this, getPackageName() + ".files", file);
            takePicture.launch(captureUri);
        } catch (Exception e) {
            Toast.makeText(this, R.string.camera_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void startCrop(Uri uri) {
        cropLauncher.launch(PhotoCropActivity.intent(this, uri));
    }

    private void applyCroppedPhoto(File file) {
        AppExecutors.io(() -> {
            byte[] bytes = null;
            File pending = new File(getCacheDir(), "photos/pending.jpg");
            try (FileInputStream input = new FileInputStream(file)) {
                bytes = new byte[(int) file.length()];
                int read = 0;
                while (read < bytes.length) {
                    int count = input.read(bytes, read, bytes.length - read);
                    if (count < 0) {
                        break;
                    }
                    read += count;
                }
                if (!file.equals(pending)) {
                    if (pending.exists()) {
                        pending.delete();
                    }
                    file.renameTo(pending);
                }
            } catch (IOException ignored) {
                bytes = null;
            }
            byte[] result = bytes;
            AppExecutors.main(() -> {
                if (isDestroyed() || model == null) {
                    return;
                }
                if (result == null) {
                    Toast.makeText(this, R.string.photo_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                model.photoBytes = result;
                model.removePhoto = false;
                bindPhoto();
            });
        });
    }

    private void focus(EditText field) {
        field.requestFocus();
        field.post(() -> {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(field, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private boolean hasContent() {
        if (!bound) {
            return false;
        }
        if (!isBlank(binding.firstNameField) || !isBlank(binding.middleNameField) || !isBlank(binding.lastNameField)
                || !isBlank(binding.companyField)) {
            return true;
        }
        for (Row row : phoneRows) {
            if (!isBlank(row.binding.valueField)) {
                return true;
            }
        }
        return false;
    }

    private void updateDone() {
        boolean enabled = hasContent() && !Boolean.TRUE.equals(viewModel.getSaving().getValue());
        if (enabled && !binding.doneButton.isEnabled()) {
            binding.doneButton.setScaleX(0.88f);
            binding.doneButton.setScaleY(0.88f);
            binding.doneButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(280)
                    .setInterpolator(new OvershootInterpolator(3f))
                    .start();
        }
        binding.doneButton.setEnabled(enabled);
    }

    private void rejectField(EditText field, int message) {
        HapticUtils.longPress(field);
        field.setError(getString(message));
        focus(field);
        reveal(field);
        field.animate().cancel();
        field.setTranslationX(0f);
        field.animate().translationX(dp(8)).setInterpolator(new CycleInterpolator(3)).setDuration(360).start();
    }

    private void hideKeyboard() {
        View focused = getCurrentFocus();
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (focused != null && imm != null) {
            imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }

    private static boolean isBlank(EditText field) {
        return field.getText() == null || field.getText().toString().trim().isEmpty();
    }

    private static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void collect() {
        model.firstName = text(binding.firstNameField);
        model.middleName = text(binding.middleNameField);
        model.lastName = text(binding.lastNameField);
        model.prefix = text(binding.prefixField);
        model.suffix = text(binding.suffixField);
        model.company = text(binding.companyField);
        model.jobTitle = text(binding.jobTitleField);
        model.phones.clear();
        for (Row row : phoneRows) {
            row.value.value = text(row.binding.valueField);
            model.phones.add(row.value);
        }
        model.emails.clear();
        for (Row row : emailRows) {
            row.value.value = text(row.binding.valueField);
            model.emails.add(row.value);
        }
        model.address = binding.addressRow.getVisibility() == View.VISIBLE ? text(binding.addressField) : null;
        model.birthday = birthday;
        model.nickname = text(binding.nicknameField);
        model.note = text(binding.notesField);
        if (model.rawContactId <= 0 && selectedAccount != null) {
            model.accountType = selectedAccount.type;
            model.accountName = selectedAccount.name;
        }
    }

    private String signature() {
        StringBuilder builder = new StringBuilder();
        for (EditText field : new EditText[]{binding.firstNameField, binding.middleNameField, binding.lastNameField,
                binding.prefixField, binding.suffixField, binding.companyField, binding.jobTitleField,
                binding.addressField, binding.nicknameField, binding.notesField}) {
            builder.append(text(field)).append('\u0001');
        }
        for (Row row : phoneRows) {
            builder.append(text(row.binding.valueField)).append(row.value.type).append('\u0002');
        }
        for (Row row : emailRows) {
            builder.append(text(row.binding.valueField)).append(row.value.type).append('\u0003');
        }
        builder.append(birthday).append(model != null && (model.photoBytes != null || model.removePhoto));
        return builder.toString();
    }

    private void attemptClose() {
        if (!bound || signature().equals(viewModel.getInitialSignature())) {
            closeWithAd();
            return;
        }
        hideKeyboard();
        ConfirmDialog.show(this, getString(R.string.discard_changes_title), getString(R.string.discard_changes_body),
                R.string.discard, true, R.drawable.ic_edit, R.string.keep_editing, this::closeWithAd);
    }

    private void closeWithAd() {
        showFullscreen(AdScreens.EDIT_CONTACT_CLOSE, this::finish);
    }

    private void save() {
        if (!bound || !hasContent()) {
            return;
        }
        for (Row row : emailRows) {
            String email = text(row.binding.valueField);
            if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                rejectField(row.binding.valueField, R.string.invalid_email);
                return;
            }
        }
        HapticUtils.confirm(binding.doneButton);
        hideKeyboard();
        collect();
        viewModel.save(model);
    }

    private void onSaved(Long id) {
        if (id == null) {
            return;
        }
        if (id <= 0) {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_LONG).show();
            return;
        }
        new File(getCacheDir(), "photos/pending.jpg").delete();
        Toast.makeText(this, R.string.contact_saved, Toast.LENGTH_SHORT).show();
        showFullscreen(AdScreens.EDIT_CONTACT_DONE, () -> {
            if (getIntent().getLongExtra(IntentKeys.EXTRA_CONTACT_ID, -1) <= 0) {
                startActivity(ContactDetailsActivity.intent(this, id, null));
            }
            finish();
        });
    }
}
