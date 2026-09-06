package com.autoracev2.waypoints.api;

import java.util.List;
import java.util.Optional;

final class EmptyCatalogs {
    private EmptyCatalogs() {
    }

    static WaypointCatalog waypoints() {
        return Waypoints.INSTANCE;
    }

    static CheckpointCatalog checkpoints() {
        return Checkpoints.INSTANCE;
    }

    private enum Waypoints implements WaypointCatalog {
        INSTANCE;

        @Override
        public List<Waypoint> all() {
            return List.of();
        }

        @Override
        public List<Waypoint> inDimension(String dimension) {
            return List.of();
        }

        @Override
        public Optional<Waypoint> byId(int id) {
            return Optional.empty();
        }

        @Override
        public int count() {
            return 0;
        }
    }

    private enum Checkpoints implements CheckpointCatalog {
        INSTANCE;

        @Override
        public List<Checkpoint> all() {
            return List.of();
        }

        @Override
        public List<Checkpoint> inDimension(String dimension) {
            return List.of();
        }

        @Override
        public Optional<Checkpoint> byId(int id) {
            return Optional.empty();
        }

        @Override
        public Optional<WorldPosition> start() {
            return Optional.empty();
        }

        @Override
        public Optional<WorldPosition> finish() {
            return Optional.empty();
        }

        @Override
        public Optional<WorldPosition> startInDimension(String dimension) {
            return Optional.empty();
        }

        @Override
        public Optional<WorldPosition> finishInDimension(String dimension) {
            return Optional.empty();
        }

        @Override
        public int count() {
            return 0;
        }
    }
}
