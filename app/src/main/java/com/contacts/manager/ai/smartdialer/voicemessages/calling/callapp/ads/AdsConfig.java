package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Typed, immutable view of the "ads" section of the remote config. */
public final class AdsConfig {

    public static final class ScreenAds {
        static final ScreenAds NONE = new ScreenAds(false, false, false, false);

        public final boolean nativeBig;
        public final boolean nativeSmall;
        public final boolean banner;
        public final boolean fullscreen;

        ScreenAds(boolean nativeBig, boolean nativeSmall, boolean banner, boolean fullscreen) {
            this.nativeBig = nativeBig;
            this.nativeSmall = nativeSmall;
            this.banner = banner;
            this.fullscreen = fullscreen;
        }
    }

    public static final class WeightedId {
        public final String id;
        public final int weight;

        public WeightedId(String id, int weight) {
            this.id = id;
            this.weight = weight;
        }
    }

    public static final class Priority {
        public final List<AdProviderType> sequence;
        public final List<AdProviderType> failed;

        Priority(List<AdProviderType> sequence, List<AdProviderType> failed) {
            this.sequence = Collections.unmodifiableList(sequence);
            this.failed = Collections.unmodifiableList(failed);
        }

        /** The sequence entry at {@code position} (looping), then the "failed" networks not tried yet. */
        public List<AdProviderType> order(int position) {
            List<AdProviderType> order = new ArrayList<>();
            order.add(sequence.get(Math.floorMod(position, sequence.size())));
            for (AdProviderType provider : failed) {
                if (!order.contains(provider)) order.add(provider);
            }
            return order;
        }
    }

    private final Map<String, ScreenAds> screenAds;
    private final Map<AdType, String> admob;
    private final Map<AdType, String> tradplus;
    private final Map<AdType, List<WeightedId>> adx;
    private final Map<AdType, Priority> priority;
    public final boolean customEnabled;
    public final String customUrl;
    public final List<FullscreenType> fullscreenSequence;
    @Nullable
    private final AdsConfig marketing;

    private AdsConfig(Map<String, ScreenAds> screenAds, Map<AdType, String> admob, Map<AdType, String> tradplus,
                      Map<AdType, List<WeightedId>> adx, Map<AdType, Priority> priority,
                      boolean customEnabled, String customUrl, List<FullscreenType> fullscreenSequence,
                      @Nullable AdsConfig marketing) {
        this.screenAds = Collections.unmodifiableMap(screenAds);
        this.admob = Collections.unmodifiableMap(admob);
        this.tradplus = Collections.unmodifiableMap(tradplus);
        this.adx = Collections.unmodifiableMap(adx);
        this.priority = Collections.unmodifiableMap(priority);
        this.customEnabled = customEnabled && !customUrl.isEmpty();
        this.customUrl = customUrl;
        this.fullscreenSequence = Collections.unmodifiableList(fullscreenSequence);
        this.marketing = marketing;
    }

    public static AdsConfig empty() {
        return new AdsConfig(new HashMap<>(), new EnumMap<>(AdType.class), new EnumMap<>(AdType.class),
                new EnumMap<>(AdType.class), new EnumMap<>(AdType.class), false, "", new ArrayList<>(), null);
    }

    /** Priority, ADX IDs, fullscreen sequence and screen switches differ per user source; ad units and custom ads are shared. */
    public AdsConfig forSource(boolean isMarketing) {
        return isMarketing && marketing != null ? marketing : this;
    }

    public ScreenAds screen(String screenKey) {
        ScreenAds screen = screenAds.get(screenKey);
        return screen != null ? screen : ScreenAds.NONE;
    }

    /** Enabled AdMob/TradPlus unit ID, or null when the format is off or not configured. */
    @Nullable
    public String unitId(AdProviderType provider, AdType type) {
        if (provider == AdProviderType.ADMOB) return admob.get(type);
        if (provider == AdProviderType.TRADPLUS) return tradplus.get(type);
        return null;
    }

    public List<WeightedId> adxIds(AdType type) {
        List<WeightedId> ids = adx.get(type);
        return ids != null ? ids : Collections.emptyList();
    }

    /** The admin's network sequence and "failed" networks, or null when no sequence was set. */
    @Nullable
    public Priority priority(AdType type) {
        return priority.get(type);
    }

    /** @throws JSONException when a sub-section has the wrong shape, so the caller can keep the cached copy. */
    public static AdsConfig fromJson(JSONObject json) throws JSONException {
        JSONObject custom = object(json, "custom");
        String customUrl = custom.optString("url", "").trim();
        boolean customEnabled = custom.optBoolean("enabled");
        Map<AdType, String> admob = units(object(json, "admob"));
        Map<AdType, String> tradplus = units(object(json, "tradplus"));

        // Backends without by_source send one set of values at the top level for everyone.
        JSONObject bySource = object(json, "by_source");
        JSONObject organicJson = bySource.has("organic") ? bySource.getJSONObject("organic") : json;
        JSONObject marketingJson = bySource.has("marketing") ? bySource.getJSONObject("marketing") : organicJson;

        AdsConfig marketing = new AdsConfig(screens(marketingJson, json), admob, tradplus, adx(object(marketingJson, "adx")),
                priority(object(marketingJson, "priority")), customEnabled, customUrl, sequence(marketingJson), null);
        return new AdsConfig(screens(organicJson, json), admob, tradplus, adx(object(organicJson, "adx")),
                priority(object(organicJson, "priority")), customEnabled, customUrl, sequence(organicJson), marketing);
    }

    private static Map<String, ScreenAds> screens(JSONObject sourceJson, JSONObject root) throws JSONException {
        JSONObject screensJson = object(sourceJson.has("screen_ads") ? sourceJson : root, "screen_ads");
        Map<String, ScreenAds> screens = new HashMap<>();
        Iterator<String> keys = screensJson.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject s = screensJson.getJSONObject(key);
            screens.put(key, new ScreenAds(s.optBoolean("native_big"), s.optBoolean("native_small"),
                    s.optBoolean("banner"), s.optBoolean("fullscreen")));
        }
        return screens;
    }

    private static List<FullscreenType> sequence(JSONObject json) throws JSONException {
        List<FullscreenType> sequence = new ArrayList<>();
        JSONArray sequenceJson = json.has("fullscreen_sequence") ? json.getJSONArray("fullscreen_sequence") : new JSONArray();
        for (int i = 0; i < sequenceJson.length(); i++) {
            FullscreenType type = FullscreenType.fromKey(sequenceJson.optString(i));
            if (type != null) sequence.add(type);
        }
        return sequence;
    }

    private static Map<AdType, Priority> priority(JSONObject json) throws JSONException {
        Map<AdType, Priority> priority = new EnumMap<>(AdType.class);
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            AdType type = AdType.fromKey(key);
            JSONObject item = json.getJSONObject(key);
            List<AdProviderType> sequence = providers(item.optJSONArray("main"), true);
            if (type != null && !sequence.isEmpty()) {
                priority.put(type, new Priority(sequence, providers(item.optJSONArray("failed"), false)));
            }
        }
        return priority;
    }

    private static List<AdProviderType> providers(@Nullable JSONArray keys, boolean allowRepeats) {
        List<AdProviderType> providers = new ArrayList<>();
        if (keys == null) return providers;
        for (int i = 0; i < keys.length(); i++) {
            AdProviderType provider = AdProviderType.fromKey(keys.optString(i));
            if (provider != null && (allowRepeats || !providers.contains(provider))) providers.add(provider);
        }
        return providers;
    }

    private static Map<AdType, String> units(JSONObject json) throws JSONException {
        Map<AdType, String> units = new EnumMap<>(AdType.class);
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            AdType type = AdType.fromKey(key);
            JSONObject unit = json.getJSONObject(key);
            String id = unit.optString("id", "").trim();
            if (type != null && unit.optBoolean("enabled") && !id.isEmpty()) {
                units.put(type, id);
            }
        }
        return units;
    }

    private static Map<AdType, List<WeightedId>> adx(JSONObject json) throws JSONException {
        Map<AdType, List<WeightedId>> adx = new EnumMap<>(AdType.class);
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            AdType type = AdType.fromKey(key);
            JSONArray rows = json.getJSONArray(key);
            List<WeightedId> ids = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                String id = row.optString("id", "").trim();
                int weight = row.optInt("weight", 0);
                if (!id.isEmpty() && weight > 0) ids.add(new WeightedId(id, weight));
            }
            if (type != null && !ids.isEmpty()) adx.put(type, Collections.unmodifiableList(ids));
        }
        return adx;
    }

    static JSONObject object(JSONObject root, String key) throws JSONException {
        if (!root.has(key) || root.isNull(key)) return new JSONObject();
        return root.getJSONObject(key);
    }
}
