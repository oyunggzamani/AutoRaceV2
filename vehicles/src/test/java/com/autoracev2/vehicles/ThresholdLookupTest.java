package com.autoracev2.vehicles;

import com.autoracev2.vehicles.registry.VehicleTierRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThresholdLookupTest {
    private final VehicleTierRegistry registry = new VehicleTierRegistry();

    @Test
    void negativeValueHasNoTier() {
        assertTrue(registry.byThreshold(-1).isEmpty());
    }

    @Test
    void boundariesSelectHighestUnlockedTier() {
        assertEquals(1, registry.byThreshold(0).orElseThrow().id());
        assertEquals(1, registry.byThreshold(9).orElseThrow().id());
        assertEquals(2, registry.byThreshold(10).orElseThrow().id());
        assertEquals(2, registry.byThreshold(29).orElseThrow().id());
        assertEquals(3, registry.byThreshold(30).orElseThrow().id());
        assertEquals(3, registry.byThreshold(99).orElseThrow().id());
        assertEquals(4, registry.byThreshold(100).orElseThrow().id());
        assertEquals(4, registry.byThreshold(499).orElseThrow().id());
        assertEquals(5, registry.byThreshold(500).orElseThrow().id());
        assertEquals(5, registry.byThreshold(999).orElseThrow().id());
        assertEquals(6, registry.byThreshold(1000).orElseThrow().id());
        assertEquals(6, registry.byThreshold(2149).orElseThrow().id());
        assertEquals(7, registry.byThreshold(2150).orElseThrow().id());
        assertEquals(7, registry.byThreshold(1_000_000).orElseThrow().id());
    }
}
