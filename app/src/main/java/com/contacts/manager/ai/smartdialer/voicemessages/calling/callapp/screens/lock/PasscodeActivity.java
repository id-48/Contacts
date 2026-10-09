package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.lock;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdScreens;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.analytics.Analytics;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.common.BaseActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ActivityPasscodeBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.DialogSecurityQuestionBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.databinding.ItemDialKeyBinding;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.AppLockManager;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;
public class PasscodeActivity extends BaseActivity {

    public static final int MODE_SETUP = 0;
    public static final int MODE_VERIFY = 1;
    public static final int MODE_UNLOCK = 2;

    private static final String EXTRA_MODE = "passcode_mode";
    private static final int LENGTH = 4;

    private ActivityPasscodeBinding binding;
    private int mode;
    private boolean unlockFlow;
    private boolean confirming;
    private String firstEntry;
    private final StringBuilder entry = new StringBuilder();

    public static Intent setupIntent(Context context) {
        return new Intent(context, PasscodeActivity.class).putExtra(EXTRA_MODE, MODE_SETUP);
    }

    public static Intent verifyIntent(Context context) {
        return new Intent(context, PasscodeActivity.class).putExtra(EXTRA_MODE, MODE_VERIFY);
    }

    public static Intent unlockIntent(Context context) {
        return new Intent(context, PasscodeActivity.class).putExtra(EXTRA_MODE, MODE_UNLOCK)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        binding = ActivityPasscodeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);

        mode = getIntent().getIntExtra(EXTRA_MODE, MODE_VERIFY);
        unlockFlow = mode == MODE_UNLOCK;
        if (mode != MODE_SETUP && !AppLockManager.hasPasscode()) {
            if (unlockFlow) {
                AppLockManager.unlock();
                finish();
                return;
            }
            mode = MODE_SETUP;
        }
        binding.topBar.topTitle.setText(mode == MODE_SETUP ? R.string.app_lock : R.string.passcode_enter);
        binding.topBar.backButton.setVisibility(mode == MODE_UNLOCK ? View.INVISIBLE : View.VISIBLE);
        setupBack(binding.topBar.backButton);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (unlockFlow) {
                    finishAffinity();
                } else {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
        });
        if (mode == MODE_SETUP) {
            showNativeAds(binding.ads.nativeBigContainer, binding.ads.nativeSmallContainer, AdScreens.APP_LOCK);
        }
        binding.forgotButton.setOnClickListener(v -> showRecovery());

        buildDots();
        buildKeypad();
        renderStage();
    }

    private void renderStage() {
        if (mode == MODE_SETUP) {
            binding.passcodeTitle.setText(confirming ? R.string.passcode_confirm : R.string.passcode_set);
            binding.passcodeBody.setText(confirming ? R.string.passcode_confirm_body : R.string.passcode_set_body);
            binding.forgotButton.setVisibility(View.GONE);
        } else {
            binding.passcodeTitle.setText(R.string.passcode_enter);
            binding.passcodeBody.setText(R.string.passcode_enter_body);
            binding.forgotButton.setVisibility(StorageService.getSecurityAnswerHash() != null ? View.VISIBLE : View.GONE);
        }
        renderDots();
    }

    private void buildDots() {
        int size = getResources().getDimensionPixelSize(R.dimen.space_16);
        int margin = getResources().getDimensionPixelSize(R.dimen.space_12);
        for (int i = 0; i < LENGTH; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMarginStart(margin);
            params.setMarginEnd(margin);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.bg_pin_dot);
            binding.dots.addView(dot);
        }
    }

    private void renderDots() {
        for (int i = 0; i < binding.dots.getChildCount(); i++) {
            View dot = binding.dots.getChildAt(i);
            boolean filled = i < entry.length();
            if (dot.isActivated() != filled) {
                dot.setActivated(filled);
                if (filled) {
                    dot.setScaleX(0.6f);
                    dot.setScaleY(0.6f);
                    dot.animate().scaleX(1f).scaleY(1f).setDuration(160).start();
                }
            }
        }
    }

    private void buildKeypad() {
        String[][] rows = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {"", "0", "<"}};
        int gap = getResources().getDimensionPixelSize(R.dimen.space_12);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (String[] row : rows) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lineParams.topMargin = gap;
            line.setLayoutParams(lineParams);
            for (String key : row) {
                View keyView;
                if ("<".equals(key)) {
                    ImageView back = new ImageView(this);
                    int size = getResources().getDimensionPixelSize(R.dimen.dial_key_size);
                    back.setLayoutParams(new LinearLayout.LayoutParams(size, size));
                    back.setImageResource(R.drawable.ic_backspace);
                    back.setScaleType(ImageView.ScaleType.CENTER);
                    back.setImageTintList(getColorStateList(R.color.text_primary));
                    back.setBackgroundResource(R.drawable.bg_round_ripple);
                    back.setContentDescription(getString(R.string.delete));
                    back.setOnClickListener(v -> {
                        HapticUtils.tap(v);
                        if (entry.length() > 0) {
                            entry.deleteCharAt(entry.length() - 1);
                            binding.passcodeError.setText(null);
                            renderDots();
                        }
                    });
                    keyView = back;
                } else if (key.isEmpty()) {
                    keyView = new View(this);
                    int size = getResources().getDimensionPixelSize(R.dimen.dial_key_size);
                    keyView.setLayoutParams(new LinearLayout.LayoutParams(size, size));
                } else {
                    ItemDialKeyBinding item = ItemDialKeyBinding.inflate(inflater, line, false);
                    item.keyDigit.setText(key);
                    item.keyLetters.setVisibility(View.GONE);
                    item.getRoot().setContentDescription(key);
                    item.getRoot().setOnClickListener(v -> {
                        HapticUtils.tap(v);
                        onDigit(key.charAt(0));
                    });
                    keyView = item.getRoot();
                }
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) keyView.getLayoutParams();
                params.setMarginStart(gap);
                params.setMarginEnd(gap);
                line.addView(keyView, params);
            }
            binding.keypad.addView(line);
        }
    }

    private void onDigit(char digit) {
        if (entry.length() >= LENGTH) {
            return;
        }
        entry.append(digit);
        binding.passcodeError.setText(null);
        renderDots();
        if (entry.length() == LENGTH) {
            binding.dots.postDelayed(this::onComplete, 140);
        }
    }

    private void onComplete() {
        String value = entry.toString();
        if (mode == MODE_SETUP) {
            if (!confirming) {
                firstEntry = value;
                confirming = true;
                entry.setLength(0);
                renderStage();
                return;
            }
            if (!value.equals(firstEntry)) {
                confirming = false;
                firstEntry = null;
                fail(R.string.passcode_mismatch);
                renderStage();
                return;
            }
            AppLockManager.savePasscode(value);
            showSecurityQuestion();
            return;
        }
        if (AppLockManager.verifyPasscode(value)) {
            HapticUtils.confirm(binding.dots);
            succeed();
        } else {
            fail(R.string.passcode_wrong);
        }
    }

    private void fail(int message) {
        entry.setLength(0);
        renderDots();
        binding.passcodeError.setText(message);
        HapticUtils.longPress(binding.dots);
        binding.dots.animate().cancel();
        binding.dots.setTranslationX(0f);
        float shake = getResources().getDimension(R.dimen.space_12);
        binding.dots.animate().translationX(shake).setDuration(50).withEndAction(() ->
                binding.dots.animate().translationX(-shake).setDuration(70).withEndAction(() ->
                        binding.dots.animate().translationX(0f).setDuration(50).start()).start()).start();
    }

    private void succeed() {
        if (unlockFlow) {
            AppLockManager.unlock();
        }
        setResult(RESULT_OK);
        finish();
        if (unlockFlow) {
            overridePendingTransition(0, R.anim.fade_out);
        }
    }

    private void showSecurityQuestion() {
        DialogSecurityQuestionBinding form = DialogSecurityQuestionBinding.inflate(getLayoutInflater());
        String[] questions = getResources().getStringArray(R.array.security_questions);
        form.questionField.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown_text, questions));
        form.questionField.setText(questions[0], false);
        form.questionField.setOnClickListener(v -> form.questionField.showDropDown());
        form.cancelButton.setVisibility(View.GONE);
        form.buttonSpace.setVisibility(View.GONE);
        form.actionButton.setText(R.string.security_save_finish);
        Dialog dialog = createFormDialog(form, false);
        form.actionButton.setOnClickListener(v -> {
            String answer = form.answerField.getText().toString().trim();
            if (answer.isEmpty()) {
                showAnswerError(form, R.string.security_answer_required);
                return;
            }
            HapticUtils.confirm(v);
            AppLockManager.saveSecurityQuestion(form.questionField.getText().toString(), answer);
            dialog.dismiss();
            AppLockManager.unlock();
            setResult(RESULT_OK);
            finish();
        });
        Analytics.trackDialog(dialog, "security_question_dialog");
        dialog.show();
    }

    private void showRecovery() {
        String question = StorageService.getSecurityQuestion();
        if (question == null) {
            return;
        }
        DialogSecurityQuestionBinding form = DialogSecurityQuestionBinding.inflate(getLayoutInflater());
        form.dialogTitle.setText(R.string.passcode_forgot);
        form.questionGroup.setVisibility(View.GONE);
        form.securityBody.setText(question);
        form.actionButton.setText(R.string.security_verify);
        Dialog dialog = createFormDialog(form, true);
        form.cancelButton.setOnClickListener(v -> dialog.dismiss());
        form.actionButton.setOnClickListener(v -> {
            if (!AppLockManager.verifySecurityAnswer(form.answerField.getText().toString())) {
                showAnswerError(form, R.string.security_wrong_answer);
                return;
            }
            HapticUtils.confirm(v);
            dialog.dismiss();
            mode = MODE_SETUP;
            confirming = false;
            firstEntry = null;
            entry.setLength(0);
            binding.topBar.backButton.setVisibility(View.INVISIBLE);
            renderStage();
        });
        Analytics.trackDialog(dialog, "passcode_recovery_dialog");
        dialog.show();
    }

    private Dialog createFormDialog(DialogSecurityQuestionBinding form, boolean cancelable) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(form.getRoot());
        dialog.setCancelable(cancelable);
        int focusedLine = getColor(R.color.primary);
        int idleLine = getColor(R.color.divider);
        form.answerField.setOnFocusChangeListener((v, hasFocus) -> {
            if (form.answerError.getVisibility() != View.VISIBLE) {
                form.answerUnderline.setBackgroundColor(hasFocus ? focusedLine : idleLine);
            }
        });
        form.answerField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (form.answerError.getVisibility() == View.VISIBLE) {
                    form.answerError.setVisibility(View.GONE);
                    form.answerUnderline.setBackgroundColor(focusedLine);
                }
            }
        });
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int margin = getResources().getDimensionPixelSize(R.dimen.space_20);
            window.setLayout(getResources().getDisplayMetrics().widthPixels - margin * 2,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setDimAmount(0.45f);
            window.setWindowAnimations(R.style.Animation_App_Pop);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        return dialog;
    }

    private void showAnswerError(DialogSecurityQuestionBinding form, int message) {
        form.answerError.setText(message);
        form.answerError.setVisibility(View.VISIBLE);
        form.answerUnderline.setBackgroundColor(getColor(R.color.error));
        HapticUtils.longPress(form.answerField);
    }
}
