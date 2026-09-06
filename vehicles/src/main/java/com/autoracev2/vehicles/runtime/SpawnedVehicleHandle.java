package com.autoracev2.vehicles.runtime;

import com.autoracev2.vehicles.api.VehiclePose;

import java.util.Objects;

public final class SpawnedVehicleHandle {
    private final String ivUniqueId;
    private final VehiclePose pose;

    public SpawnedVehicleHandle(String ivUniqueId, VehiclePose pose) {
        if (ivUniqueId == null || ivUniqueId.isBlank()) {
            throw new IllegalArgumentException("iv unique id must not be blank");
        }
        this.ivUniqueId = ivUniqueId.trim();
        this.pose = Objects.requireNonNull(pose, "pose");
    }

    public String ivUniqueId() {
        return ivUniqueId;
    }

    public VehiclePose pose() {
        return pose;
    }
}
