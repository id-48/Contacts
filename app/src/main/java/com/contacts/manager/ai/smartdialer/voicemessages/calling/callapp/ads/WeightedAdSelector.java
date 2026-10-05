package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Random;

/** Picks one ADX ID with probability proportional to its weight (a weight of 0 is never picked). */
public final class WeightedAdSelector {

    private final Random random;

    public WeightedAdSelector() {
        this(new Random());
    }

    public WeightedAdSelector(Random random) {
        this.random = random;
    }

    @Nullable
    public String select(List<AdsConfig.WeightedId> ids) {
        long total = 0;
        for (AdsConfig.WeightedId id : ids) {
            if (id.weight > 0 && !id.id.isEmpty()) total += id.weight;
        }
        if (total <= 0) return null;
        long roll = (long) (random.nextDouble() * total);
        for (AdsConfig.WeightedId id : ids) {
            if (id.weight <= 0 || id.id.isEmpty()) continue;
            roll -= id.weight;
            if (roll < 0) return id.id;
        }
        return null;
    }
}
