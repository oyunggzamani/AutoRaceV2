package com.autoracev2.waypoints.store;

import com.autoracev2.waypoints.api.Checkpoint;
import com.autoracev2.waypoints.api.CheckpointCatalog;
import com.autoracev2.waypoints.api.WorldPosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Ordered checkpoints plus independent START / FINISH markers.
 */
public final class CheckpointStore implements CheckpointCatalog {
    private final List<WorldPosition> points = new ArrayList<>();
    private WorldPosition start;
    private WorldPosition finish;

    public Checkpoint add(WorldPosition position) {
        points.add(position);
        return numbered(points.size());
    }

    public Optional<Checkpoint> remove(int id) {
        if (!isValidId(id)) {
            return Optional.empty();
        }
        WorldPosition removed = points.remove(id - 1);
        return Optional.of(new Checkpoint(id, removed));
    }

    public void clearCheckpoints() {
        points.clear();
    }

    public void setStart(WorldPosition position) {
        this.start = position;
    }

    public void setFinish(WorldPosition position) {
        this.finish = position;
    }

    public void replaceAll(List<WorldPosition> ordered, WorldPosition startPoint, WorldPosition finishPoint) {
        points.clear();
        if (ordered != null) {
            points.addAll(ordered);
        }
        this.start = startPoint;
        this.finish = finishPoint;
    }

    @Override
    public List<Checkpoint> all() {
        List<Checkpoint> result = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) {
            result.add(new Checkpoint(i + 1, points.get(i)));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<Checkpoint> inDimension(String dimension) {
        List<Checkpoint> result = new ArrayList<>();
        for (Checkpoint checkpoint : all()) {
            if (checkpoint.isInDimension(dimension)) {
                result.add(checkpoint);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Optional<Checkpoint> byId(int id) {
        if (!isValidId(id)) {
            return Optional.empty();
        }
        return Optional.of(numbered(id));
    }

    @Override
    public Optional<WorldPosition> start() {
        return Optional.ofNullable(start);
    }

    @Override
    public Optional<WorldPosition> finish() {
        return Optional.ofNullable(finish);
    }

    @Override
    public Optional<WorldPosition> startInDimension(String dimension) {
        return filterDimension(start, dimension);
    }

    @Override
    public Optional<WorldPosition> finishInDimension(String dimension) {
        return filterDimension(finish, dimension);
    }

    @Override
    public int count() {
        return points.size();
    }

    public boolean isEmpty() {
        return points.isEmpty() && start == null && finish == null;
    }

    private static Optional<WorldPosition> filterDimension(WorldPosition position, String dimension) {
        if (position == null || !position.isInDimension(dimension)) {
            return Optional.empty();
        }
        return Optional.of(position);
    }

    private boolean isValidId(int id) {
        return id >= 1 && id <= points.size();
    }

    private Checkpoint numbered(int id) {
        return new Checkpoint(id, points.get(id - 1));
    }
}
