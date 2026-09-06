package com.autoracev2.waypoints;

import com.autoracev2.waypoints.api.Checkpoint;
import com.autoracev2.waypoints.api.WorldPosition;
import com.autoracev2.waypoints.store.CheckpointStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckpointStoreTest {
    private static final String OVERWORLD = "minecraft:overworld";

    @Test
    void addAndRemoveRenumbersCheckpoints() {
        CheckpointStore store = new CheckpointStore();
        store.add(pos(OVERWORLD, 1, 64, 0));
        store.add(pos(OVERWORLD, 2, 64, 0));
        store.add(pos(OVERWORLD, 3, 64, 0));

        assertTrue(store.remove(2).isPresent());
        List<Checkpoint> remaining = store.all();
        assertEquals(List.of("CP1", "CP2"), remaining.stream().map(Checkpoint::label).toList());
        assertEquals(3.0, remaining.get(1).position().x());
    }

    @Test
    void startAndFinishAreIndependentFromCheckpoints() {
        CheckpointStore store = new CheckpointStore();
        WorldPosition start = pos(OVERWORLD, 0, 64, 0);
        WorldPosition finish = pos(OVERWORLD, 100, 64, 0);
        store.setStart(start);
        store.setFinish(finish);
        store.add(pos(OVERWORLD, 25, 64, 0));

        store.clearCheckpoints();

        assertTrue(store.all().isEmpty());
        assertEquals(start, store.start().orElseThrow());
        assertEquals(finish, store.finish().orElseThrow());
    }

    @Test
    void startAndFinishCanBeReplaced() {
        CheckpointStore store = new CheckpointStore();
        store.setStart(pos(OVERWORLD, 1, 64, 1));
        store.setStart(pos(OVERWORLD, 8, 70, 8));
        store.setFinish(pos(OVERWORLD, 9, 70, 9));

        assertEquals(8.0, store.start().orElseThrow().x());
        assertEquals(9.0, store.finish().orElseThrow().z());
    }

    private static WorldPosition pos(String dimension, double x, double y, double z) {
        return new WorldPosition(dimension, x, y, z);
    }
}
