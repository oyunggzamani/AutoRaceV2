package com.autoracev2.waypoints;

import com.autoracev2.waypoints.api.AutoRaceV2Data;
import com.autoracev2.waypoints.api.WorldPosition;
import com.autoracev2.waypoints.persist.LoadReport;
import com.autoracev2.waypoints.persist.StorageLayout;
import com.autoracev2.waypoints.persist.TrackStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TrackStorageTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";

    @TempDir
    Path tempDir;

    @Test
    void saveLoadAndRestartSimulation() throws Exception {
        StorageLayout layout = new StorageLayout(tempDir.resolve("autoracev2"));
        TrackStorage first = new TrackStorage(layout);
        first.open();
        first.waypoints().add(pos(OVERWORLD, 1, 64, 1));
        first.waypoints().add(pos(OVERWORLD, 2, 64, 2));
        first.waypoints().add(pos(NETHER, 3, 40, 3));
        first.checkpoints().add(pos(OVERWORLD, 10, 64, 10));
        first.checkpoints().setStart(pos(OVERWORLD, 0, 64, 0));
        first.checkpoints().setFinish(pos(OVERWORLD, 50, 64, 0));
        first.saveAll();

        assertTrue(Files.exists(layout.waypointsFile()));
        assertTrue(Files.exists(layout.checkpointsFile()));
        assertFalse(Files.exists(layout.waypointsFile().resolveSibling("waypoints.json.tmp")));

        TrackStorage restarted = new TrackStorage(layout);
        List<LoadReport> reports = restarted.open();
        assertTrue(reports.stream().allMatch(report -> report.status() == LoadReport.Status.LOADED));

        assertEquals(3, restarted.waypoints().count());
        assertEquals(2.0, restarted.waypoints().byId(2).orElseThrow().position().x());
        assertEquals(NETHER, restarted.waypoints().byId(3).orElseThrow().position().dimension());
        assertEquals(1, restarted.checkpoints().count());
        assertEquals(0.0, restarted.checkpoints().start().orElseThrow().x());
        assertEquals(50.0, restarted.checkpoints().finish().orElseThrow().x());
        assertEquals(2, AutoRaceV2Data.waypoints().inDimension(OVERWORLD).size());
        assertEquals(1, AutoRaceV2Data.waypoints().inDimension(NETHER).size());
    }

    @Test
    void missingFilesAreCreated() {
        StorageLayout layout = new StorageLayout(tempDir.resolve("autoracev2"));
        TrackStorage storage = new TrackStorage(layout);
        List<LoadReport> reports = storage.open();
        assertTrue(reports.stream().allMatch(report -> report.status() == LoadReport.Status.CREATED));
        assertTrue(Files.exists(layout.waypointsFile()));
        assertTrue(Files.exists(layout.checkpointsFile()));
        assertEquals(0, storage.waypoints().count());
        assertEquals(0, storage.checkpoints().count());
    }

    @Test
    void emptyJsonUsesSafeFallback() throws Exception {
        StorageLayout layout = new StorageLayout(tempDir.resolve("autoracev2"));
        Files.createDirectories(layout.directory());
        Files.writeString(layout.waypointsFile(), "   ");
        Files.writeString(layout.checkpointsFile(), "");

        TrackStorage storage = new TrackStorage(layout);
        List<LoadReport> reports = assertDoesNotThrow(storage::open);
        assertTrue(reports.stream().anyMatch(report -> report.status() == LoadReport.Status.EMPTY));
        assertEquals(0, storage.waypoints().count());
        assertEquals(0, storage.checkpoints().count());
        assertTrue(Files.readString(layout.waypointsFile()).contains("\"waypoints\""));
        assertTrue(Files.readString(layout.checkpointsFile()).contains("\"checkpoints\""));
    }

    @Test
    void corruptJsonDoesNotCrashAndFallsBack() throws Exception {
        StorageLayout layout = new StorageLayout(tempDir.resolve("autoracev2"));
        Files.createDirectories(layout.directory());
        Files.writeString(layout.waypointsFile(), "{ this is not json");
        Files.writeString(layout.checkpointsFile(), "[1,2,3]");

        TrackStorage storage = new TrackStorage(layout);
        List<LoadReport> reports = assertDoesNotThrow(storage::open);
        assertTrue(reports.stream().allMatch(report -> report.status() == LoadReport.Status.CORRUPT));
        assertEquals(0, storage.waypoints().count());
        assertEquals(0, storage.checkpoints().count());
        assertTrue(storage.checkpoints().start().isEmpty());
        assertTrue(storage.checkpoints().finish().isEmpty());

        try (var files = Files.list(layout.directory())) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString().startsWith("waypoints.json.corrupt-")));
        }
        try (var files = Files.list(layout.directory())) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString().startsWith("checkpoints.json.corrupt-")));
        }
    }

    @Test
    void waypointAndCheckpointFilesStayIndependent() throws Exception {
        StorageLayout layout = new StorageLayout(tempDir.resolve("autoracev2"));
        TrackStorage storage = new TrackStorage(layout);
        storage.open();
        storage.waypoints().add(pos(OVERWORLD, 1, 64, 1));
        storage.checkpoints().add(pos(OVERWORLD, 9, 64, 9));
        storage.saveAll();

        storage.waypoints().clear();
        storage.saveWaypoints();

        TrackStorage reloaded = new TrackStorage(layout);
        reloaded.open();
        assertEquals(0, reloaded.waypoints().count());
        assertEquals(1, reloaded.checkpoints().count());
        assertEquals(9.0, reloaded.checkpoints().byId(1).orElseThrow().position().x());
    }

    @Test
    void dimensionFilterHidesForeignWorldPoints() throws Exception {
        StorageLayout layout = new StorageLayout(tempDir.resolve("autoracev2"));
        TrackStorage storage = new TrackStorage(layout);
        storage.open();
        storage.waypoints().add(pos(OVERWORLD, 1, 64, 1));
        storage.waypoints().add(pos(NETHER, 2, 40, 2));
        storage.checkpoints().add(pos(OVERWORLD, 3, 64, 3));
        storage.checkpoints().add(pos(NETHER, 4, 40, 4));
        storage.checkpoints().setStart(pos(OVERWORLD, 0, 64, 0));
        storage.checkpoints().setFinish(pos(NETHER, 8, 40, 8));
        storage.saveAll();

        TrackStorage reloaded = new TrackStorage(layout);
        reloaded.open();
        assertEquals(1, reloaded.waypoints().inDimension(OVERWORLD).size());
        assertEquals(1, reloaded.waypoints().inDimension(NETHER).size());
        assertEquals(0, reloaded.waypoints().inDimension("minecraft:the_end").size());
        assertTrue(reloaded.checkpoints().startInDimension(OVERWORLD).isPresent());
        assertTrue(reloaded.checkpoints().startInDimension(NETHER).isEmpty());
        assertTrue(reloaded.checkpoints().finishInDimension(NETHER).isPresent());
        assertTrue(reloaded.checkpoints().finishInDimension(OVERWORLD).isEmpty());
    }

    @Test
    void objectJsonWithoutArraysLoadsAsEmpty() throws Exception {
        StorageLayout layout = new StorageLayout(tempDir.resolve("autoracev2"));
        Files.createDirectories(layout.directory());
        Files.writeString(layout.waypointsFile(), "{}\n");
        Files.writeString(layout.checkpointsFile(), "{\"schemaVersion\":1}\n");

        TrackStorage storage = new TrackStorage(layout);
        List<LoadReport> reports = storage.open();
        assertTrue(reports.stream().allMatch(report -> report.status() == LoadReport.Status.LOADED));
        assertEquals(0, storage.waypoints().count());
        assertEquals(0, storage.checkpoints().count());
    }

    private static WorldPosition pos(String dimension, double x, double y, double z) {
        return new WorldPosition(dimension, x, y, z);
    }
}
