package com.autoracev2.waypoints.persist;

import com.autoracev2.waypoints.api.AutoRaceV2Data;
import com.autoracev2.waypoints.store.CheckpointStore;
import com.autoracev2.waypoints.store.WaypointStore;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns waypoint and checkpoint stores and their JSON files.
 * Future modules should read through {@link AutoRaceV2Data}, not this class.
 */
public final class TrackStorage {
    private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final StorageLayout layout;
    private final WaypointStore waypoints = new WaypointStore();
    private final CheckpointStore checkpoints = new CheckpointStore();
    private final List<LoadReport> lastLoadReports = new ArrayList<>();

    public TrackStorage(StorageLayout layout) {
        this.layout = layout;
    }

    public WaypointStore waypoints() {
        return waypoints;
    }

    public CheckpointStore checkpoints() {
        return checkpoints;
    }

    public StorageLayout layout() {
        return layout;
    }

    public List<LoadReport> lastLoadReports() {
        return List.copyOf(lastLoadReports);
    }

    public void bindApi() {
        AutoRaceV2Data.bind(waypoints, checkpoints);
    }

    public List<LoadReport> open() {
        lastLoadReports.clear();
        try {
            Files.createDirectories(layout.directory());
        } catch (IOException exception) {
            lastLoadReports.add(LoadReport.of(LoadReport.Status.IO_ERROR, layout.directory().toString(),
                    "could not create data directory: " + exception.getMessage()));
            bindApi();
            return lastLoadReports();
        }
        lastLoadReports.add(loadWaypoints());
        lastLoadReports.add(loadCheckpoints());
        bindApi();
        return lastLoadReports();
    }

    public void saveAll() throws IOException {
        saveWaypoints();
        saveCheckpoints();
    }

    public void saveWaypoints() throws IOException {
        WaypointDocument document = WaypointDocument.fromStore(waypoints.all());
        AtomicJsonFiles.write(layout.waypointsFile(), TrackJsonCodec.writeWaypoints(document));
    }

    public void saveCheckpoints() throws IOException {
        CheckpointDocument document = CheckpointDocument.fromStore(
                checkpoints.all(),
                checkpoints.start().orElse(null),
                checkpoints.finish().orElse(null)
        );
        AtomicJsonFiles.write(layout.checkpointsFile(), TrackJsonCodec.writeCheckpoints(document));
    }

    public void saveQuietly() {
        try {
            saveAll();
        } catch (IOException ignored) {
            // Caller logs this at the Forge layer.
        }
    }

    private LoadReport loadWaypoints() {
        return loadFile(layout.waypointsFile(), StorageLayout.WAYPOINTS_FILE, true);
    }

    private LoadReport loadCheckpoints() {
        return loadFile(layout.checkpointsFile(), StorageLayout.CHECKPOINTS_FILE, false);
    }

    private LoadReport loadFile(java.nio.file.Path file, String name, boolean waypointSide) {
        try {
            if (!Files.exists(file)) {
                writeEmpty(waypointSide);
                return LoadReport.of(LoadReport.Status.CREATED, name, "missing file created");
            }
            String raw = AtomicJsonFiles.readIfPresent(file);
            if (raw == null || raw.isBlank()) {
                writeEmpty(waypointSide);
                return LoadReport.of(LoadReport.Status.EMPTY, name, "empty file replaced with valid JSON");
            }
            if (waypointSide) {
                ParsedDocument<WaypointDocument> parsed = TrackJsonCodec.readWaypoints(raw);
                return applyWaypointParse(parsed, name);
            }
            ParsedDocument<CheckpointDocument> parsed = TrackJsonCodec.readCheckpoints(raw);
            return applyCheckpointParse(parsed, name);
        } catch (IOException exception) {
            applyEmpty(waypointSide);
            return LoadReport.of(LoadReport.Status.IO_ERROR, name, exception.getMessage());
        }
    }

    private LoadReport applyWaypointParse(ParsedDocument<WaypointDocument> parsed, String name) {
        if (parsed.kind() == ParsedDocument.Kind.CORRUPT) {
            AtomicJsonFiles.backupCorrupt(layout.waypointsFile(), corruptSuffix());
            waypoints.replaceAll(List.of());
            try {
                saveWaypoints();
            } catch (IOException ignored) {
            }
            return new LoadReport(LoadReport.Status.CORRUPT, name, parsed.warnings());
        }
        if (parsed.kind() == ParsedDocument.Kind.EMPTY) {
            waypoints.replaceAll(List.of());
            try {
                saveWaypoints();
            } catch (IOException ignored) {
            }
            return new LoadReport(LoadReport.Status.EMPTY, name, parsed.warnings());
        }
        waypoints.replaceAll(parsed.value().orderedPositions());
        return new LoadReport(LoadReport.Status.LOADED, name, parsed.warnings());
    }

    private LoadReport applyCheckpointParse(ParsedDocument<CheckpointDocument> parsed, String name) {
        if (parsed.kind() == ParsedDocument.Kind.CORRUPT) {
            AtomicJsonFiles.backupCorrupt(layout.checkpointsFile(), corruptSuffix());
            checkpoints.replaceAll(List.of(), null, null);
            try {
                saveCheckpoints();
            } catch (IOException ignored) {
            }
            return new LoadReport(LoadReport.Status.CORRUPT, name, parsed.warnings());
        }
        if (parsed.kind() == ParsedDocument.Kind.EMPTY) {
            checkpoints.replaceAll(List.of(), null, null);
            try {
                saveCheckpoints();
            } catch (IOException ignored) {
            }
            return new LoadReport(LoadReport.Status.EMPTY, name, parsed.warnings());
        }
        CheckpointDocument document = parsed.value();
        checkpoints.replaceAll(document.orderedPositions(), document.start(), document.finish());
        return new LoadReport(LoadReport.Status.LOADED, name, parsed.warnings());
    }

    private void applyEmpty(boolean waypointSide) {
        if (waypointSide) {
            waypoints.replaceAll(List.of());
        } else {
            checkpoints.replaceAll(List.of(), null, null);
        }
    }

    private void writeEmpty(boolean waypointSide) throws IOException {
        if (waypointSide) {
            waypoints.replaceAll(List.of());
            saveWaypoints();
        } else {
            checkpoints.replaceAll(List.of(), null, null);
            saveCheckpoints();
        }
    }

    private static String corruptSuffix() {
        return ".corrupt-" + LocalDateTime.now().format(BACKUP_TIME);
    }
}
