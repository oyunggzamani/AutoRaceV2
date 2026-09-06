package com.autoracev2.vehicles.api;

import java.util.Locale;
import java.util.Objects;

/**
 * World placement that later modules can reuse for spawn and swap.
 * Yaw/pitch are Minecraft facing degrees so race heading is preserved.
 */
public final class VehiclePose {
    private final String dimension;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public VehiclePose(String dimension, double x, double y, double z, float yaw, float pitch) {
        if (dimension == null || dimension.isBlank()) {
            throw new IllegalArgumentException("dimension must not be blank");
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("coordinates must be finite");
        }
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("orientation must be finite");
        }
        this.dimension = dimension.trim();
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
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

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public String formatShort() {
        return dimension + " "
                + format(x) + " " + format(y) + " " + format(z)
                + " yaw=" + format(yaw) + " pitch=" + format(pitch);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VehiclePose other)) {
            return false;
        }
        return dimension.equals(other.dimension)
                && Double.compare(x, other.x) == 0
                && Double.compare(y, other.y) == 0
                && Double.compare(z, other.z) == 0
                && Float.compare(yaw, other.yaw) == 0
                && Float.compare(pitch, other.pitch) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, x, y, z, yaw, pitch);
    }

    @Override
    public String toString() {
        return "VehiclePose{" + formatShort() + "}";
    }
}
