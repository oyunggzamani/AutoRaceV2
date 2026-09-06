package com.autoracev2.waypoints.api;

import java.util.List;
import java.util.Optional;

/**
 * Read-only access to race control points for future AutoRaceV2 modules.
 * Checkpoints, START and FINISH are independent from the waypoint route.
 */
public interface CheckpointCatalog {
    List<Checkpoint> all();

    List<Checkpoint> inDimension(String dimension);

    Optional<Checkpoint> byId(int id);

    Optional<WorldPosition> start();

    Optional<WorldPosition> finish();

    Optional<WorldPosition> startInDimension(String dimension);

    Optional<WorldPosition> finishInDimension(String dimension);

    int count();
}
