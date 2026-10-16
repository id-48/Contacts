package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
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
    public void treatsMissingSegmentsAsZero() {
        assertEquals(0, VersionUtils.compare("1.0", "1.0.0"));
        assertEquals(0, VersionUtils.compare("2", "2.0"));
        assertEquals(-1, VersionUtils.compare("1", "1.0.1"));
    }

    @Test
    public void ignoresNoise() {
        assertEquals(0, VersionUtils.compare("1.2.0-beta", "1.2"));
        assertEquals(0, VersionUtils.compare(" 1.2 ", "1.2"));
        assertEquals(0, VersionUtils.compare(null, ""));
        assertEquals(-1, VersionUtils.compare("", "0.1"));
    }

    @Test
    public void forceUpdateComparesVersionCodeExactly() throws JSONException {
        assertTrue(ForceUpdateHelper.isUpdateRequired(forceUpdate(true, "4"), 3));
        assertFalse(ForceUpdateHelper.isUpdateRequired(forceUpdate(true, "3"), 3));
        assertTrue(ForceUpdateHelper.isUpdateRequired(forceUpdate(true, "3"), 4));
    }

    @Test
    public void forceUpdateOffOrEmptyNeverBlocks() throws JSONException {
        assertFalse(ForceUpdateHelper.isUpdateRequired(forceUpdate(false, "9"), 1));
        assertFalse(ForceUpdateHelper.isUpdateRequired(forceUpdate(true, ""), 1));
        assertFalse(ForceUpdateHelper.isUpdateRequired(RemoteConfig.defaults(), 1));
        assertFalse(ForceUpdateHelper.isUpdateRequired(forceUpdate(true, "1.2.0"), 1));
    }

    @Test
    public void parseVersionCodeAcceptsWholeNumbersOnly() {
        assertEquals(Integer.valueOf(3), ForceUpdateHelper.parseVersionCode("3"));
        assertEquals(Integer.valueOf(12), ForceUpdateHelper.parseVersionCode(" 12 "));
        assertNull(ForceUpdateHelper.parseVersionCode("1.3"));
        assertNull(ForceUpdateHelper.parseVersionCode(""));
        assertNull(ForceUpdateHelper.parseVersionCode(null));
    }

    private static RemoteConfig forceUpdate(boolean enabled, String minimum) throws JSONException {
        return RemoteConfig.fromJson(new JSONObject()
                .put("force_update", new JSONObject()
                        .put("enabled", enabled)
                        .put("minimum_version", minimum)));
    }
}
