package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

public class VersionUtilsTest {

    @Test
    public void comparesNumericallyNotLexically() {
        assertEquals(1, VersionUtils.compare("1.10", "1.9"));
        assertEquals(-1, VersionUtils.compare("1.9", "1.10"));
        assertEquals(-1, VersionUtils.compare("1.2.3", "1.2.4"));
        assertEquals(1, VersionUtils.compare("2.0", "1.99.99"));
    }

    @Test
    public void missingPartsCountAsZero() {
        assertEquals(0, VersionUtils.compare("1.0", "1.0.0"));
        assertEquals(0, VersionUtils.compare("2", "2.0"));
        assertEquals(-1, VersionUtils.compare("1", "1.0.1"));
    }

    @Test
    public void suffixesAndBlanksAreTolerated() {
        assertEquals(0, VersionUtils.compare("1.2.0-beta", "1.2"));
        assertEquals(0, VersionUtils.compare(" 1.2 ", "1.2"));
        assertEquals(0, VersionUtils.compare(null, ""));
        assertEquals(-1, VersionUtils.compare("", "0.1"));
    }

    private static RemoteConfig forceUpdate(boolean enabled, String minimum) throws Exception {
        return RemoteConfig.fromJson(new JSONObject()
                .put("config_version", 3)
                .put("force_update", new JSONObject().put("enabled", enabled).put("minimum_version", minimum)));
    }

    @Test
    public void forceUpdateOnlyBelowMinimumVersion() throws Exception {
        assertTrue(ForceUpdateHelper.isUpdateRequired(forceUpdate(true, "1.5.0"), "1.4.9"));
        assertFalse(ForceUpdateHelper.isUpdateRequired(forceUpdate(true, "1.5.0"), "1.5"));
        assertFalse(ForceUpdateHelper.isUpdateRequired(forceUpdate(true, "1.5.0"), "1.10"));
    }

    @Test
    public void forceUpdateNeedsSwitchAndMinimum() throws Exception {
        assertFalse(ForceUpdateHelper.isUpdateRequired(forceUpdate(false, "9.0"), "1.0"));
        assertFalse(ForceUpdateHelper.isUpdateRequired(forceUpdate(true, ""), "1.0"));
        assertFalse(ForceUpdateHelper.isUpdateRequired(RemoteConfig.defaults(), "1.0"));
    }
}
