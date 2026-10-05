package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdProviderType;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdType;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.AdsConfig;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads.FullscreenType;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class RemoteConfigParseTest {

    /** Same shape as backend build_public_config(). */
    private static JSONObject backendConfig() throws Exception {
        return new JSONObject()
                .put("config_version", 12)
                .put("app", new JSONObject()
                        .put("app_version", "1.4.0")
                        .put("app_link", "https://play.google.com/store/apps/details?id=x")
                        .put("privacy_policy", "https://example.com/privacy"))
                .put("force_update", new JSONObject()
                        .put("enabled", true).put("minimum_version", "1.2.0").put("message", "Please update"))
                .put("screens", new JSONObject()
                        .put("welcome", false).put("theme_selection", true).put("language_selection", true)
                        .put("after_call_organic", true).put("after_call_marketing", false))
                .put("features", new JSONObject()
                        .put("splash_after_fullscreen_ad", true).put("app_open_on_resume", true)
                        .put("launcher_enabled", true))
                .put("marketing", new JSONObject()
                        .put("facebook_app_id", "1234").put("facebook_client_token", "token"))
                .put("ads", new JSONObject()
                        .put("screen_ads", new JSONObject()
                                .put("welcome", new JSONObject()
                                        .put("native_big", true).put("native_small", false)
                                        .put("banner", true).put("fullscreen", true)))
                        .put("admob", new JSONObject()
                                .put("banner", new JSONObject().put("enabled", true).put("id", "ca-app-pub-1/banner"))
                                .put("native", new JSONObject().put("enabled", false).put("id", "ca-app-pub-1/native"))
                                .put("interstitial", new JSONObject().put("enabled", true).put("id", " ")))
                        .put("tradplus", new JSONObject()
                                .put("interstitial", new JSONObject().put("enabled", true).put("id", "TP-INTER")))
                        .put("adx", new JSONObject()
                                .put("banner", new JSONArray()
                                        .put(new JSONObject().put("id", "/1/banner-a").put("weight", 70))
                                        .put(new JSONObject().put("id", "/1/banner-b").put("weight", 0))))
                        .put("custom", new JSONObject().put("enabled", true).put("url", "https://example.com/ads.json"))
                        .put("priority", new JSONObject()
                                .put("banner", new JSONObject()
                                        .put("main", new JSONArray(Arrays.asList("adx", "admob")))
                                        .put("failed", new JSONArray(Arrays.asList("admob", "bogus", "custom"))))
                                .put("native", new JSONObject()
                                        .put("main", new JSONArray()).put("failed", new JSONArray())))
                        .put("fullscreen_sequence", new JSONArray(Arrays.asList("interstitial", "bogus", "app_open", "custom"))));
    }

    @Test
    public void parsesEveryBackendSection() throws Exception {
        RemoteConfig config = RemoteConfig.fromJson(backendConfig());

        assertEquals(12, config.configVersion);
        assertEquals("1.4.0", config.appVersion);
        assertEquals("https://example.com/privacy", config.privacyPolicyUrl);
        assertTrue(config.forceUpdateEnabled);
        assertEquals("1.2.0", config.minimumVersion);
        assertEquals("Please update", config.forceUpdateMessage);
        assertFalse(config.isScreenEnabled(RemoteConfig.SCREEN_WELCOME));
        assertTrue(config.isScreenEnabled(RemoteConfig.SCREEN_LANGUAGE_SELECTION));
        assertFalse(config.isScreenEnabled(RemoteConfig.SCREEN_AFTER_CALL_MARKETING));
        assertTrue(config.splashAfterFullscreenAd);
        assertTrue(config.appOpenOnResume);
        assertTrue(config.launcherEnabled);
        assertEquals("1234", config.facebookAppId);
        assertEquals("token", config.facebookClientToken);
    }

    @Test
    public void adsConfigKeepsOnlyUsableUnits() throws Exception {
        AdsConfig ads = RemoteConfig.fromJson(backendConfig()).ads;

        assertEquals("ca-app-pub-1/banner", ads.unitId(AdProviderType.ADMOB, AdType.BANNER));
        assertNull("disabled unit", ads.unitId(AdProviderType.ADMOB, AdType.NATIVE));
        assertNull("blank unit", ads.unitId(AdProviderType.ADMOB, AdType.INTERSTITIAL));
        assertEquals("TP-INTER", ads.unitId(AdProviderType.TRADPLUS, AdType.INTERSTITIAL));
        assertEquals(1, ads.adxIds(AdType.BANNER).size());
        assertEquals("/1/banner-a", ads.adxIds(AdType.BANNER).get(0).id);
        assertTrue(ads.adxIds(AdType.NATIVE).isEmpty());
        assertTrue(ads.customEnabled);
        assertEquals(Arrays.asList(FullscreenType.INTERSTITIAL, FullscreenType.APP_OPEN, FullscreenType.CUSTOM),
                ads.fullscreenSequence);

        AdsConfig.ScreenAds welcome = ads.screen("welcome");
        assertTrue(welcome.nativeBig);
        assertFalse(welcome.nativeSmall);
        assertTrue(welcome.banner);
        assertTrue(welcome.fullscreen);
        AdsConfig.ScreenAds unknown = ads.screen("not_configured");
        assertFalse(unknown.nativeBig || unknown.nativeSmall || unknown.banner || unknown.fullscreen);
    }

    @Test
    public void priorityTriesTheSequenceEntryThenUntriedFailedNetworks() throws Exception {
        AdsConfig ads = RemoteConfig.fromJson(backendConfig()).ads;
        AdsConfig.Priority banner = ads.priority(AdType.BANNER);

        assertEquals(Arrays.asList(AdProviderType.ADX, AdProviderType.ADMOB, AdProviderType.CUSTOM), banner.order(0));
        assertEquals(Arrays.asList(AdProviderType.ADMOB, AdProviderType.CUSTOM), banner.order(1));
        assertEquals("loops", banner.order(0), banner.order(2));
        assertNull("empty lists use the default order", ads.priority(AdType.NATIVE));
        assertNull("missing type uses the default order", ads.priority(AdType.REWARD));
    }

    @Test
    public void prioritySequenceKeepsRepeats() throws Exception {
        JSONObject json = backendConfig();
        json.getJSONObject("ads").getJSONObject("priority").put("banner", new JSONObject()
                .put("main", new JSONArray(Arrays.asList("admob", "admob", "adx", "tradplus", "admob")))
                .put("failed", new JSONArray()));
        AdsConfig.Priority banner = RemoteConfig.fromJson(json).ads.priority(AdType.BANNER);

        assertEquals(Arrays.asList(AdProviderType.ADMOB, AdProviderType.ADMOB, AdProviderType.ADX,
                AdProviderType.TRADPLUS, AdProviderType.ADMOB), banner.sequence);
        assertEquals(Arrays.asList(AdProviderType.ADX), banner.order(2));
        assertEquals(Arrays.asList(AdProviderType.TRADPLUS), banner.order(3));
    }

    @Test
    public void marketingUsersGetMarketingValues() throws Exception {
        JSONObject json = backendConfig();
        JSONObject ads = json.getJSONObject("ads");
        ads.put("by_source", new JSONObject()
                .put("organic", new JSONObject()
                        .put("priority", new JSONObject().put("banner", new JSONObject()
                                .put("main", new JSONArray(Arrays.asList("admob"))).put("failed", new JSONArray())))
                        .put("adx", new JSONObject())
                        .put("fullscreen_sequence", new JSONArray(Arrays.asList("interstitial"))))
                .put("marketing", new JSONObject()
                        .put("priority", new JSONObject().put("banner", new JSONObject()
                                .put("main", new JSONArray(Arrays.asList("tradplus"))).put("failed", new JSONArray())))
                        .put("adx", new JSONObject().put("native", new JSONArray()
                                .put(new JSONObject().put("id", "/1/native-mkt").put("weight", 10))))
                        .put("fullscreen_sequence", new JSONArray(Arrays.asList("reward", "custom")))
                        .put("screen_ads", new JSONObject().put("welcome", new JSONObject()
                                .put("native_big", false).put("native_small", true)
                                .put("banner", false).put("fullscreen", false)))));

        AdsConfig config = RemoteConfig.fromJson(json).ads;
        AdsConfig organic = config.forSource(false);
        AdsConfig marketing = config.forSource(true);

        assertEquals(Arrays.asList(AdProviderType.ADMOB), organic.priority(AdType.BANNER).sequence);
        assertEquals(Arrays.asList(FullscreenType.INTERSTITIAL), organic.fullscreenSequence);
        assertTrue(organic.adxIds(AdType.NATIVE).isEmpty());

        assertEquals(Arrays.asList(AdProviderType.TRADPLUS), marketing.priority(AdType.BANNER).sequence);
        assertEquals(Arrays.asList(FullscreenType.REWARD, FullscreenType.CUSTOM), marketing.fullscreenSequence);
        assertEquals("/1/native-mkt", marketing.adxIds(AdType.NATIVE).get(0).id);

        assertEquals("shared units", "ca-app-pub-1/banner", marketing.unitId(AdProviderType.ADMOB, AdType.BANNER));

        AdsConfig.ScreenAds marketingWelcome = marketing.screen("welcome");
        assertFalse(marketingWelcome.nativeBig);
        assertTrue(marketingWelcome.nativeSmall);
        assertFalse(marketingWelcome.fullscreen);
        assertTrue("organic without its own screen_ads uses the top level", organic.screen("welcome").nativeBig);
        assertTrue(organic.screen("welcome").fullscreen);
    }

    @Test
    public void withoutBySourceEveryoneGetsTopLevelValues() throws Exception {
        AdsConfig config = RemoteConfig.fromJson(backendConfig()).ads;
        assertEquals(config.forSource(false).fullscreenSequence, config.forSource(true).fullscreenSequence);
        assertEquals(config.forSource(false).priority(AdType.BANNER).sequence,
                config.forSource(true).priority(AdType.BANNER).sequence);
        assertEquals("/1/banner-a", config.forSource(true).adxIds(AdType.BANNER).get(0).id);
    }

    @Test
    public void customAdsNeedAUrl() throws Exception {
        JSONObject json = backendConfig();
        json.getJSONObject("ads").put("custom", new JSONObject().put("enabled", true).put("url", ""));
        assertFalse(RemoteConfig.fromJson(json).ads.customEnabled);
    }

    @Test
    public void defaultsNeverBlockTheApp() {
        RemoteConfig config = RemoteConfig.fromJson(null);
        assertEquals(0, config.configVersion);
        assertFalse(config.forceUpdateEnabled);
        assertFalse(config.launcherEnabled);
        assertFalse(config.appOpenOnResume);
        assertTrue(config.isScreenEnabled(RemoteConfig.SCREEN_WELCOME));
        assertTrue(config.isScreenEnabled(RemoteConfig.SCREEN_THEME_SELECTION));
        assertFalse(config.isScreenEnabled(RemoteConfig.SCREEN_LANGUAGE_SELECTION));
        assertTrue(config.isScreenEnabled("screen_without_switch"));
        assertTrue(config.ads.fullscreenSequence.isEmpty());
        assertNull(config.ads.unitId(AdProviderType.ADMOB, AdType.BANNER));
    }

    @Test
    public void introFlowIsPerSourceAndAlwaysHasDefaultPhone() throws Exception {
        JSONObject json = backendConfig().put("onboarding", new JSONObject()
                .put("organic", new JSONArray(Arrays.asList("default_phone", "theme_selection", "welcome")))
                .put("marketing", new JSONArray(Arrays.asList("welcome", "bogus", "welcome"))));
        RemoteConfig config = RemoteConfig.fromJson(json);

        assertEquals(Arrays.asList(RemoteConfig.SCREEN_DEFAULT_PHONE, RemoteConfig.SCREEN_THEME_SELECTION,
                RemoteConfig.SCREEN_WELCOME), config.introFlow(false));
        assertEquals(Arrays.asList(RemoteConfig.SCREEN_WELCOME, RemoteConfig.SCREEN_DEFAULT_PHONE), config.introFlow(true));
    }

    @Test
    public void withoutIntroFlowTheScreenSwitchesDecide() throws Exception {
        RemoteConfig config = RemoteConfig.fromJson(backendConfig());
        List<String> expected = Arrays.asList(RemoteConfig.SCREEN_LANGUAGE_SELECTION,
                RemoteConfig.SCREEN_THEME_SELECTION, RemoteConfig.SCREEN_DEFAULT_PHONE);

        assertEquals(expected, config.introFlow(false));
        assertEquals(expected, config.introFlow(true));
        assertEquals(Arrays.asList(RemoteConfig.SCREEN_WELCOME, RemoteConfig.SCREEN_THEME_SELECTION,
                RemoteConfig.SCREEN_DEFAULT_PHONE), RemoteConfig.fromJson(null).introFlow(false));
    }

    @Test
    public void malformedJsonFallsBackToDefaults() throws Exception {
        JSONObject json = new JSONObject().put("config_version", 5).put("ads", "not-an-object");
        RemoteConfig config = RemoteConfig.fromJson(json);
        assertEquals(0, config.configVersion);
    }

    @Test
    public void mergeKeepsPreviousCopyOfMalformedSection() throws Exception {
        JSONObject previous = backendConfig();
        JSONObject fresh = new JSONObject()
                .put("config_version", 13)
                .put("features", new JSONObject().put("launcher_enabled", false))
                .put("ads", new JSONObject().put("admob", "broken"));

        RemoteConfig merged = RemoteConfig.fromJson(RemoteConfig.mergeValidSections(previous, fresh));

        assertEquals(13, merged.configVersion);
        assertFalse("valid section replaced", merged.launcherEnabled);
        assertEquals("malformed section kept", "ca-app-pub-1/banner",
                merged.ads.unitId(AdProviderType.ADMOB, AdType.BANNER));
        assertEquals("untouched section kept", "1.2.0", merged.minimumVersion);
    }
}
