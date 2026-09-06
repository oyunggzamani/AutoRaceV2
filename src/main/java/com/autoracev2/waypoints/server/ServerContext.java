package com.autoracev2.waypoints.server;

import com.autoracev2.waypoints.AutoRaceV2Waypoints;
import com.autoracev2.waypoints.api.AutoRaceV2Data;
import com.autoracev2.waypoints.debug.DebugNetwork;
import com.autoracev2.waypoints.debug.DebugVisibility;
import com.autoracev2.waypoints.persist.LoadReport;
import com.autoracev2.waypoints.persist.StorageLayout;
import com.autoracev2.waypoints.persist.TrackStorage;
import com.autoracev2.waypoints.store.ClearConfirmGuard;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.util.List;

public final class ServerContext {
    public static final long CLEAR_CONFIRM_TIMEOUT_MS = 30_000L;

    private static TrackStorage storage;
    private static final ClearConfirmGuard waypointClear = new ClearConfirmGuard(CLEAR_CONFIRM_TIMEOUT_MS);
    private static final ClearConfirmGuard checkpointClear = new ClearConfirmGuard(CLEAR_CONFIRM_TIMEOUT_MS);
    private static final DebugVisibility debugVisibility = new DebugVisibility();

    private ServerContext() {
    }

    public static void start(MinecraftServer server) {
        StorageLayout layout = StorageLayout.underConfig(FMLPaths.CONFIGDIR.get());
        storage = new TrackStorage(layout);
        List<LoadReport> reports = storage.open();
        for (LoadReport report : reports) {
            logReport(report);
        }
        AutoRaceV2Waypoints.LOGGER.info("AutoRaceV2 data directory: {}", layout.directory().toAbsolutePath());
    }

    public static void stop() {
        if (storage != null) {
            try {
                storage.saveAll();
            } catch (IOException exception) {
                AutoRaceV2Waypoints.LOGGER.error("Failed to save AutoRaceV2 data on shutdown", exception);
            }
        }
        AutoRaceV2Data.unbind();
        debugVisibility.clear();
        storage = null;
    }

    public static TrackStorage storage() {
        return storage;
    }

    public static boolean isReady() {
        return storage != null;
    }

    public static ClearConfirmGuard waypointClear() {
        return waypointClear;
    }

    public static ClearConfirmGuard checkpointClear() {
        return checkpointClear;
    }

    public static DebugVisibility debugVisibility() {
        return debugVisibility;
    }

    public static void persistWaypoints() throws IOException {
        requireStorage().saveWaypoints();
        DebugNetwork.syncAllViewers(requireStorage(), debugVisibility);
    }

    public static void persistCheckpoints() throws IOException {
        requireStorage().saveCheckpoints();
        DebugNetwork.syncAllViewers(requireStorage(), debugVisibility);
    }

    public static TrackStorage requireStorage() {
        if (storage == null) {
            throw new IllegalStateException("AutoRaceV2 storage is not loaded");
        }
        return storage;
    }

    private static void logReport(LoadReport report) {
        String prefix = "AutoRaceV2 " + report.fileName() + " [" + report.status() + "]";
        if (report.details().isEmpty()) {
            AutoRaceV2Waypoints.LOGGER.info(prefix);
            return;
        }
        switch (report.status()) {
            case CORRUPT, IO_ERROR -> AutoRaceV2Waypoints.LOGGER.error("{} {}", prefix, report.details());
            case EMPTY, CREATED -> AutoRaceV2Waypoints.LOGGER.warn("{} {}", prefix, report.details());
            default -> AutoRaceV2Waypoints.LOGGER.info("{} {}", prefix, report.details());
        }
    }
}
