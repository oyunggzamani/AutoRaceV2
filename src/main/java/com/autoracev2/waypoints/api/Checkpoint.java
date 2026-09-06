package com.autoracev2.waypoints.api;

import java.util.Objects;

/**
 * A numbered race-progress marker. Independent from waypoints.
 * START and FINISH are not checkpoints and live on {@link CheckpointCatalog}.
 */
public final class Checkpoint {
    private final int id;
    private final WorldPosition position;

    public Checkpoint(int id, WorldPosition position) {
        if (id < 1) {
            throw new IllegalArgumentException("checkpoint id must be >= 1");
        }
        this.id = id;
        this.position = Objects.requireNonNull(position, "position");
    }

    public int id() {
        return id;
    }

    public WorldPosition position() {
        return position;
    }

    public String label() {
        return "CP" + id;
    }

    public boolean isInDimension(String dimension) {
        return position.isInDimension(dimension);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Checkpoint other)) {
            return false;
        }
        return id == other.id && position.equals(other.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, position);
    }

    @Override
    public String toString() {
        return label() + " " + position.formatShort();
    }
}
