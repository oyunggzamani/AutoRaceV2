package com.autoracev2.vehicles.api;

import java.util.Objects;
import java.util.Optional;

/**
 * Pack part that should replace the default engine after the body is spawned.
 */
public final class EngineOverride {
    private final String packId;
    private final String partSystemName;
    private final int maxRpm;

    public EngineOverride(String packId, String partSystemName, int maxRpm) {
        if (packId == null || packId.isBlank()) {
            throw new IllegalArgumentException("engine pack id must not be blank");
        }
        if (partSystemName == null || partSystemName.isBlank()) {
            throw new IllegalArgumentException("engine part name must not be blank");
        }
        if (maxRpm <= 0) {
            throw new IllegalArgumentException("maxRpm must be positive");
        }
        this.packId = packId.trim();
        this.partSystemName = partSystemName.trim();
        this.maxRpm = maxRpm;
    }

    public String packId() {
        return packId;
    }

    public String partSystemName() {
        return partSystemName;
    }

    public int maxRpm() {
        return maxRpm;
    }

    public String packPartId() {
        return packId + ":" + partSystemName;
    }

    public static Optional<EngineOverride> none() {
        return Optional.empty();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EngineOverride other)) {
            return false;
        }
        return packId.equals(other.packId)
                && partSystemName.equals(other.partSystemName)
                && maxRpm == other.maxRpm;
    }

    @Override
    public int hashCode() {
        return Objects.hash(packId, partSystemName, maxRpm);
    }

    @Override
    public String toString() {
        return packPartId() + " maxRPM=" + maxRpm;
    }
}
