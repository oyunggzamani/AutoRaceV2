package com.autoracev2.waypoints.persist;

import java.nio.file.Path;

public final class StorageLayout {
    public static final String DIRECTORY_NAME = "autoracev2";
    public static final String WAYPOINTS_FILE = "waypoints.json";
    public static final String CHECKPOINTS_FILE = "checkpoints.json";

    private final Path directory;

    public StorageLayout(Path directory) {
        this.directory = directory;
    }

    public static StorageLayout underConfig(Path configDirectory) {
        return new StorageLayout(configDirectory.resolve(DIRECTORY_NAME));
    }

    public Path directory() {
        return directory;
    }

    public Path waypointsFile() {
        return directory.resolve(WAYPOINTS_FILE);
    }

    public Path checkpointsFile() {
        return directory.resolve(CHECKPOINTS_FILE);
    }
}
