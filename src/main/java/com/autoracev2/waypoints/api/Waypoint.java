package com.autoracev2.waypoints.api;

import java.util.Objects;

/**
 * A numbered route point. IDs are sequential (WP1, WP2, ...) and are rebuilt
 * whenever the ordered list changes so later driving modules never see gaps.
 */
public final class Waypoint {
    private final int id;
    private final WorldPosition position;

    public Waypoint(int id, WorldPosition position) {
        if (id < 1) {
            throw new IllegalArgumentException("waypoint id must be >= 1");
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
        return "WP" + id;
    }

    public boolean isInDimension(String dimension) {
        return position.isInDimension(dimension);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Waypoint other)) {
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
