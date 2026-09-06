package com.autoracev2.vehicles.api;

import java.util.Objects;
import java.util.Optional;

public final class TierDefinition {
    private final int id;
    private final String displayName;
    private final int threshold;
    private final String packId;
    private final String vehicleSystemName;
    private final EngineOverride engineOverride;

    public TierDefinition(
            int id,
            String displayName,
            int threshold,
            String packId,
            String vehicleSystemName,
            EngineOverride engineOverride
    ) {
        if (id < 1) {
            throw new IllegalArgumentException("tier id must be >= 1");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("display name must not be blank");
        }
        if (threshold < 0) {
            throw new IllegalArgumentException("threshold must be >= 0");
        }
        if (packId == null || packId.isBlank()) {
            throw new IllegalArgumentException("pack id must not be blank");
        }
        if (vehicleSystemName == null || vehicleSystemName.isBlank()) {
            throw new IllegalArgumentException("vehicle system name must not be blank");
        }
        this.id = id;
        this.displayName = displayName.trim();
        this.threshold = threshold;
        this.packId = packId.trim();
        this.vehicleSystemName = vehicleSystemName.trim();
        this.engineOverride = engineOverride;
    }

    public int id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public int threshold() {
        return threshold;
    }

    public String packId() {
        return packId;
    }

    public String vehicleSystemName() {
        return vehicleSystemName;
    }

    public String packVehicleId() {
        return packId + ":" + vehicleSystemName;
    }

    public Optional<EngineOverride> engineOverride() {
        return Optional.ofNullable(engineOverride);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TierDefinition other)) {
            return false;
        }
        return id == other.id
                && threshold == other.threshold
                && displayName.equals(other.displayName)
                && packId.equals(other.packId)
                && vehicleSystemName.equals(other.vehicleSystemName)
                && Objects.equals(engineOverride, other.engineOverride);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayName, threshold, packId, vehicleSystemName, engineOverride);
    }

    @Override
    public String toString() {
        return "T" + id + " " + displayName + " threshold=" + threshold + " " + packVehicleId();
    }
}
