package com.autoracev2.waypoints.store;

import com.autoracev2.waypoints.api.Waypoint;
import com.autoracev2.waypoints.api.WaypointCatalog;
import com.autoracev2.waypoints.api.WorldPosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Ordered waypoint route. IDs are always 1..N with no gaps.
 */
public final class WaypointStore implements WaypointCatalog {
    private final List<WorldPosition> points = new ArrayList<>();

    public Waypoint add(WorldPosition position) {
        points.add(position);
        return numbered(points.size());
    }

    public Optional<Waypoint> remove(int id) {
        if (!isValidId(id)) {
            return Optional.empty();
        }
        WorldPosition removed = points.remove(id - 1);
        return Optional.of(new Waypoint(id, removed));
    }

    public void clear() {
        points.clear();
    }

    public void replaceAll(List<WorldPosition> ordered) {
        points.clear();
        if (ordered != null) {
            points.addAll(ordered);
        }
    }

    @Override
    public List<Waypoint> all() {
        List<Waypoint> result = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) {
            result.add(new Waypoint(i + 1, points.get(i)));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<Waypoint> inDimension(String dimension) {
        List<Waypoint> result = new ArrayList<>();
        for (Waypoint waypoint : all()) {
            if (waypoint.isInDimension(dimension)) {
                result.add(waypoint);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Optional<Waypoint> byId(int id) {
        if (!isValidId(id)) {
            return Optional.empty();
        }
        return Optional.of(numbered(id));
    }

    @Override
    public int count() {
        return points.size();
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    private boolean isValidId(int id) {
        return id >= 1 && id <= points.size();
    }

    private Waypoint numbered(int id) {
        return new Waypoint(id, points.get(id - 1));
    }
}
