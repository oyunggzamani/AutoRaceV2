package com.autoracev2.waypoints.debug;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class DebugVisibility {
    private final Set<UUID> waypointViewers = new HashSet<>();
    private final Set<UUID> checkpointViewers = new HashSet<>();

    public boolean toggleWaypoints(UUID playerId, boolean show) {
        if (show) {
            waypointViewers.add(playerId);
        } else {
            waypointViewers.remove(playerId);
        }
        return show;
    }

    public boolean toggleCheckpoints(UUID playerId, boolean show) {
        if (show) {
            checkpointViewers.add(playerId);
        } else {
            checkpointViewers.remove(playerId);
        }
        return show;
    }

    public boolean showsWaypoints(UUID playerId) {
        return waypointViewers.contains(playerId);
    }

    public boolean showsCheckpoints(UUID playerId) {
        return checkpointViewers.contains(playerId);
    }

    public boolean showsAnything(UUID playerId) {
        return showsWaypoints(playerId) || showsCheckpoints(playerId);
    }

    public Set<UUID> allViewers() {
        Set<UUID> viewers = new HashSet<>(waypointViewers);
        viewers.addAll(checkpointViewers);
        return viewers;
    }

    public void remove(UUID playerId) {
        waypointViewers.remove(playerId);
        checkpointViewers.remove(playerId);
    }

    public void clear() {
        waypointViewers.clear();
        checkpointViewers.clear();
    }
}
