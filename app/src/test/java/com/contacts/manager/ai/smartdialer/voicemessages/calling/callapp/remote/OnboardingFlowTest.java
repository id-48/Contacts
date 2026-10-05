package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote;

import static org.junit.Assert.assertEquals;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote.OnboardingFlow.Step;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OnboardingFlowTest {

    private static final List<String> REVERSED = Arrays.asList(
            RemoteConfig.SCREEN_DEFAULT_PHONE, RemoteConfig.SCREEN_THEME_SELECTION, RemoteConfig.SCREEN_WELCOME);

    @Test
    public void followsTheAdminOrder() {
        Set<String> done = new HashSet<>();
        assertEquals(Step.DEFAULT_PHONE, OnboardingFlow.next(REVERSED, done));
        done.add(RemoteConfig.SCREEN_DEFAULT_PHONE);
        assertEquals(Step.THEME, OnboardingFlow.next(REVERSED, done));
        done.add(RemoteConfig.SCREEN_THEME_SELECTION);
        assertEquals(Step.WELCOME, OnboardingFlow.next(REVERSED, done));
        done.add(RemoteConfig.SCREEN_WELCOME);
        assertEquals(Step.HOME, OnboardingFlow.next(REVERSED, done));
    }

    @Test
    public void screensNotInTheFlowAreNeverShown() {
        List<String> flow = Arrays.asList(RemoteConfig.SCREEN_LANGUAGE_SELECTION, RemoteConfig.SCREEN_DEFAULT_PHONE);
        assertEquals(Step.LANGUAGE, OnboardingFlow.next(flow, Collections.emptySet()));
        assertEquals(Step.DEFAULT_PHONE,
                OnboardingFlow.next(flow, new HashSet<>(Collections.singletonList(RemoteConfig.SCREEN_LANGUAGE_SELECTION))));
    }

    @Test
    public void finishedStepsAreSkippedWhenTheFlowChanges() {
        Set<String> done = new HashSet<>(Arrays.asList(RemoteConfig.SCREEN_WELCOME, RemoteConfig.SCREEN_THEME_SELECTION));
        assertEquals(Step.DEFAULT_PHONE, OnboardingFlow.next(REVERSED, done));
    }
}
