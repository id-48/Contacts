package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class WeightedAdSelectorTest {

    private static AdsConfig.WeightedId id(String id, int weight) {
        return new AdsConfig.WeightedId(id, weight);
    }

    /** Random whose nextDouble() always returns the given value. */
    private static Random fixed(double value) {
        return new Random() {
            @Override
            public double nextDouble() {
                return value;
            }
        };
    }

    @Test
    public void emptyOrZeroWeightsSelectNothing() {
        WeightedAdSelector selector = new WeightedAdSelector(new Random(1));
        assertNull(selector.select(Collections.emptyList()));
        assertNull(selector.select(Arrays.asList(id("a", 0), id("b", 0))));
        assertNull(selector.select(Collections.singletonList(id("", 50))));
    }

    @Test
    public void singleUsableIdIsAlwaysSelected() {
        WeightedAdSelector selector = new WeightedAdSelector(new Random(7));
        List<AdsConfig.WeightedId> ids = Arrays.asList(id("zero", 0), id("only", 10));
        for (int i = 0; i < 100; i++) {
            assertEquals("only", selector.select(ids));
        }
    }

    @Test
    public void rollBoundariesFollowWeights() {
        List<AdsConfig.WeightedId> ids = Arrays.asList(id("a", 70), id("zero", 0), id("b", 30));
        assertEquals("a", new WeightedAdSelector(fixed(0.0)).select(ids));
        assertEquals("a", new WeightedAdSelector(fixed(0.699)).select(ids));
        assertEquals("b", new WeightedAdSelector(fixed(0.70)).select(ids));
        assertEquals("b", new WeightedAdSelector(fixed(0.9999)).select(ids));
    }

    @Test
    public void distributionMatchesWeights() {
        WeightedAdSelector selector = new WeightedAdSelector(new Random(42));
        List<AdsConfig.WeightedId> ids = Arrays.asList(id("a", 70), id("b", 30), id("never", 0));
        Map<String, Integer> counts = new HashMap<>();
        int runs = 20_000;
        for (int i = 0; i < runs; i++) {
            counts.merge(selector.select(ids), 1, Integer::sum);
        }
        double shareA = counts.getOrDefault("a", 0) / (double) runs;
        assertTrue("share of a was " + shareA, Math.abs(shareA - 0.70) < 0.02);
        assertEquals(0, (int) counts.getOrDefault("never", 0));
    }
}
