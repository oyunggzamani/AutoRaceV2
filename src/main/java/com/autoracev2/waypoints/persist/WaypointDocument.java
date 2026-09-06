package com.autoracev2.waypoints.persist;

import com.autoracev2.waypoints.api.Waypoint;
import com.autoracev2.waypoints.api.WorldPosition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class WaypointDocument {
    private final List<IndexedPosition> entries;

    WaypointDocument(List<IndexedPosition> entries) {
        this.entries = new ArrayList<>(entries);
        this.entries.sort(Comparator.comparingInt(IndexedPosition::id));
    }

    static WaypointDocument fromStore(List<Waypoint> waypoints) {
        List<IndexedPosition> entries = new ArrayList<>();
        for (Waypoint waypoint : waypoints) {
            entries.add(new IndexedPosition(waypoint.id(), waypoint.position()));
        }
        return new WaypointDocument(entries);
    }

    List<WorldPosition> orderedPositions() {
        List<WorldPosition> positions = new ArrayList<>(entries.size());
        for (IndexedPosition entry : entries) {
            positions.add(entry.position());
        }
        return positions;
    }

    List<IndexedPosition> entries() {
        return List.copyOf(entries);
    }
}
