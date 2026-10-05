package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.launcher.LauncherMode;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.main.MainActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.onboarding.DefaultPhoneActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.onboarding.ThemeIntroActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.onboarding.WelcomeActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.settings.LanguageActivity;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.PhoneService;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.services.StorageService;

import java.util.List;
import java.util.Set;

/**
 * Single source of truth for onboarding order: the admin's intro flow for the user source. Finished
 * steps are remembered, so a flow changed mid-onboarding never shows a screen twice or loops.
 */
public final class OnboardingFlow {

    public enum Step {
        LANGUAGE(RemoteConfig.SCREEN_LANGUAGE_SELECTION),
        WELCOME(RemoteConfig.SCREEN_WELCOME),
        THEME(RemoteConfig.SCREEN_THEME_SELECTION),
        DEFAULT_PHONE(RemoteConfig.SCREEN_DEFAULT_PHONE),
        HOME(null);

        @Nullable
        final String key;

        Step(@Nullable String key) {
            this.key = key;
        }

        @Nullable
        static Step fromKey(String key) {
            for (Step step : values()) {
                if (key.equals(step.key)) return step;
            }
            return null;
        }
    }

    private OnboardingFlow() {
    }

    /** First step of {@code flow} not in {@code done}, or HOME when every step is finished. */
    public static Step next(List<String> flow, Set<String> done) {
        for (String key : flow) {
            Step step = Step.fromKey(key);
            if (step != null && !done.contains(key)) return step;
        }
        return Step.HOME;
    }

    private static Step next() {
        return next(RemoteConfigManager.get().introFlow(UserSourceManager.isMarketing()),
                StorageService.getOnboardingStepsDone());
    }

    /** Where the app goes after the splash screen; the default phone screen again whenever a required default role is missing. */
    public static Intent startIntent(Context context) {
        if (!StorageService.isOnboardingDone()) return intentFor(context, next());
        if (!PhoneService.isDefaultDialerSatisfied(context) || !LauncherMode.isSatisfied(context)) {
            return new Intent(context, DefaultPhoneActivity.class);
        }
        return homeIntent(context);
    }

    public static Intent homeIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }

    public static void continueFrom(Activity activity, Step current) {
        if (current.key != null) StorageService.markOnboardingStepDone(current.key);
        Step next = StorageService.isOnboardingDone() ? Step.HOME : next();
        activity.startActivity(intentFor(activity, next));
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        activity.finish();
    }

    private static Intent intentFor(Context context, Step step) {
        switch (step) {
            case LANGUAGE:
                return LanguageActivity.intent(context, true);
            case WELCOME:
                return new Intent(context, WelcomeActivity.class);
            case THEME:
                return new Intent(context, ThemeIntroActivity.class);
            case DEFAULT_PHONE:
                return new Intent(context, DefaultPhoneActivity.class);
            case HOME:
            default:
                StorageService.setOnboardingDone(true);
                return homeIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
    }
}
