package com.autoracev2.waypoints;

import com.autoracev2.waypoints.api.Waypoint;
import com.autoracev2.waypoints.api.WorldPosition;
import com.autoracev2.waypoints.store.WaypointStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaypointStoreTest {
    private static final String OVERWORLD = "minecraft:overworld";

    @Test
    void addsSequentialWaypointIds() {
        WaypointStore store = new WaypointStore();
        Waypoint wp1 = store.add(pos(OVERWORLD, 1, 64, 1));
        Waypoint wp2 = store.add(pos(OVERWORLD, 2, 64, 2));
        Waypoint wp3 = store.add(pos(OVERWORLD, 3, 64, 3));

        assertEquals("WP1", wp1.label());
        assertEquals("WP2", wp2.label());
        assertEquals("WP3", wp3.label());
        assertEquals(3, store.count());
        assertEquals(List.of(1, 2, 3), store.all().stream().map(Waypoint::id).toList());
    }

    @Test
    void removeRenumbersWithoutGaps() {
        WaypointStore store = new WaypointStore();
        store.add(pos(OVERWORLD, 10, 64, 0));
        store.add(pos(OVERWORLD, 20, 64, 0));
        store.add(pos(OVERWORLD, 30, 64, 0));
        store.add(pos(OVERWORLD, 40, 64, 0));

        assertTrue(store.remove(2).isPresent());

        List<Waypoint> remaining = store.all();
        assertEquals(3, remaining.size());
        assertEquals("WP1", remaining.get(0).label());
        assertEquals(10.0, remaining.get(0).position().x());
        assertEquals("WP2", remaining.get(1).label());
        assertEquals(30.0, remaining.get(1).position().x());
        assertEquals("WP3", remaining.get(2).label());
        assertEquals(40.0, remaining.get(2).position().x());
    }

    @Test
    void unknownRemoveIsIgnored() {
        WaypointStore store = new WaypointStore();
        store.add(pos(OVERWORLD, 1, 64, 1));
        assertTrue(store.remove(5).isEmpty());
        assertEquals(1, store.count());
    }

    private static WorldPosition pos(String dimension, double x, double y, double z) {
        return new WorldPosition(dimension, x, y, z);
    }
}
