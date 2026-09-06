package com.autoracev2.waypoints.api;

import java.util.List;
import java.util.Optional;

/**
 * Read-only access to the waypoint route for future AutoRaceV2 modules.
 */
public interface WaypointCatalog {
    List<Waypoint> all();

    List<Waypoint> inDimension(String dimension);

    Optional<Waypoint> byId(int id);

    int count();
}
