package com.autoracev2.waypoints;

import com.autoracev2.waypoints.api.Waypoint;
import com.autoracev2.waypoints.api.WorldPosition;
import com.autoracev2.waypoints.persist.StorageLayout;
import com.autoracev2.waypoints.persist.TrackStorage;
import com.autoracev2.waypoints.store.ClearConfirmGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end coverage for the required editor scenarios, without launching Minecraft.
 */
class ScenarioSuiteTest {
    private static final String OVERWORLD = "minecraft:overworld";

    @TempDir
    Path tempDir;

    @Test
    void requiredEditorScenarios() throws Exception {
        TrackStorage storage = new TrackStorage(new StorageLayout(tempDir.resolve("autoracev2")));
        storage.open();

        storage.waypoints().add(pos(1));
        storage.waypoints().add(pos(2));
        storage.waypoints().add(pos(3));
        assertEquals(List.of("WP1", "WP2", "WP3"),
                storage.waypoints().all().stream().map(Waypoint::label).toList());

        storage.waypoints().remove(2);
        assertEquals("WP2", storage.waypoints().byId(2).orElseThrow().label());
        assertEquals(3.0, storage.waypoints().byId(2).orElseThrow().position().x());

        storage.checkpoints().add(pos(11));
        storage.checkpoints().add(pos(12));
        storage.checkpoints().remove(1);
        assertEquals(1, storage.checkpoints().count());
        assertEquals(12.0, storage.checkpoints().byId(1).orElseThrow().position().x());

        storage.checkpoints().setStart(pos(0));
        storage.checkpoints().setFinish(pos(99));
        assertEquals(0.0, storage.checkpoints().start().orElseThrow().x());
        assertEquals(99.0, storage.checkpoints().finish().orElseThrow().x());

        storage.saveAll();
        TrackStorage loaded = new TrackStorage(new StorageLayout(tempDir.resolve("autoracev2")));
        loaded.open();
        assertEquals(2, loaded.waypoints().count());
        assertEquals(1, loaded.checkpoints().count());
        assertTrue(loaded.checkpoints().start().isPresent());
        assertTrue(loaded.checkpoints().finish().isPresent());

        ClearConfirmGuard guard = new ClearConfirmGuard(30_000L);
        assertEquals(ClearConfirmGuard.Outcome.NOT_PENDING, guard.confirm("op"));
        guard.request("op");
        assertEquals(ClearConfirmGuard.Outcome.CONFIRMED, guard.confirm("op"));
        loaded.waypoints().clear();
        loaded.checkpoints().clearCheckpoints();
        loaded.saveAll();
        assertEquals(0, loaded.waypoints().count());
        assertEquals(0, loaded.checkpoints().count());
        assertTrue(loaded.checkpoints().start().isPresent());
    }

    private static WorldPosition pos(double x) {
        return new WorldPosition(OVERWORLD, x, 64, 0);
    }
}
