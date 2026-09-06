package com.autoracev2.waypoints.api;

/**
 * Stable entry point for later AutoRaceV2 JARs (Vehicles, Race, Leaderboard).
 * Those modules should only read through this facade.
 */
public final class AutoRaceV2Data {
    private static volatile WaypointCatalog waypoints = EmptyCatalogs.waypoints();
    private static volatile CheckpointCatalog checkpoints = EmptyCatalogs.checkpoints();

    private AutoRaceV2Data() {
    }

    public static WaypointCatalog waypoints() {
        return waypoints;
    }

    public static CheckpointCatalog checkpoints() {
        return checkpoints;
    }

    public static void bind(WaypointCatalog waypointCatalog, CheckpointCatalog checkpointCatalog) {
        waypoints = waypointCatalog != null ? waypointCatalog : EmptyCatalogs.waypoints();
        checkpoints = checkpointCatalog != null ? checkpointCatalog : EmptyCatalogs.checkpoints();
    }

    public static void unbind() {
        waypoints = EmptyCatalogs.waypoints();
        checkpoints = EmptyCatalogs.checkpoints();
    }
}
