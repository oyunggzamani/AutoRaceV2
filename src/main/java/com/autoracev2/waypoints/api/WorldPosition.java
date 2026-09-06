package com.autoracev2.waypoints.api;

import java.util.Objects;

/**
 * A world-space point used by both waypoint and checkpoint systems.
 * Dimension is required so later modules can refuse points from the wrong world.
 */
public final class WorldPosition {
    private final String dimension;
    private final double x;
    private final double y;
    private final double z;

    public WorldPosition(String dimension, double x, double y, double z) {
        if (dimension == null || dimension.isBlank()) {
            throw new IllegalArgumentException("dimension must not be blank");
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("coordinates must be finite");
        }
        this.dimension = dimension.trim();
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String dimension() {
        return dimension;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public boolean isInDimension(String otherDimension) {
        return otherDimension != null && dimension.equals(otherDimension);
    }

    public String formatShort() {
        return dimension + " " + formatCoord(x) + " " + formatCoord(y) + " " + formatCoord(z);
    }

    private static String formatCoord(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WorldPosition other)) {
            return false;
        }
        return dimension.equals(other.dimension)
                && Double.compare(x, other.x) == 0
                && Double.compare(y, other.y) == 0
                && Double.compare(z, other.z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, x, y, z);
    }

    @Override
    public String toString() {
        return "WorldPosition{" + formatShort() + "}";
    }
}
