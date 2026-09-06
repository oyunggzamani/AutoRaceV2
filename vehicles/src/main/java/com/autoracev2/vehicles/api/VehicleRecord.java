package com.autoracev2.vehicles.api;

import java.util.Objects;
import java.util.UUID;

/**
 * AutoRaceV2 identity for one active race vehicle.
 * {@code vehicleId} is ours. {@code ivUniqueId} is Immersive Vehicles uniqueUUID.
 */
public final class VehicleRecord {
    private final UUID vehicleId;
    private final OwnerRef owner;
    private final int tierId;
    private final String ivUniqueId;
    private final VehiclePose pose;
    private final VehicleState state;
    private final VehicleMomentum momentum;

    public VehicleRecord(
            UUID vehicleId,
            OwnerRef owner,
            int tierId,
            String ivUniqueId,
            VehiclePose pose,
            VehicleState state,
            VehicleMomentum momentum
    ) {
        this.vehicleId = Objects.requireNonNull(vehicleId, "vehicleId");
        this.owner = Objects.requireNonNull(owner, "owner");
        if (tierId < 1) {
            throw new IllegalArgumentException("tier id must be >= 1");
        }
        if (ivUniqueId == null || ivUniqueId.isBlank()) {
            throw new IllegalArgumentException("iv unique id must not be blank");
        }
        this.tierId = tierId;
        this.ivUniqueId = ivUniqueId.trim();
        this.pose = Objects.requireNonNull(pose, "pose");
        this.state = Objects.requireNonNull(state, "state");
        this.momentum = momentum == null ? VehicleMomentum.NONE : momentum;
    }

    public static VehicleRecord active(OwnerRef owner, int tierId, String ivUniqueId, VehiclePose pose) {
        return new VehicleRecord(UUID.randomUUID(), owner, tierId, ivUniqueId, pose, VehicleState.ACTIVE, VehicleMomentum.NONE);
    }

    public UUID vehicleId() {
        return vehicleId;
    }

    public OwnerRef owner() {
        return owner;
    }

    public int tierId() {
        return tierId;
    }

    public String ivUniqueId() {
        return ivUniqueId;
    }

    public VehiclePose pose() {
        return pose;
    }

    public VehicleState state() {
        return state;
    }

    public VehicleMomentum momentum() {
        return momentum;
    }

    public boolean isActive() {
        return state == VehicleState.ACTIVE;
    }

    public VehicleRecord withPose(VehiclePose nextPose) {
        return new VehicleRecord(vehicleId, owner, tierId, ivUniqueId, nextPose, state, momentum);
    }

    public VehicleRecord inactive() {
        return new VehicleRecord(vehicleId, owner, tierId, ivUniqueId, pose, VehicleState.INACTIVE, momentum);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VehicleRecord other)) {
            return false;
        }
        return vehicleId.equals(other.vehicleId)
                && owner.equals(other.owner)
                && tierId == other.tierId
                && ivUniqueId.equals(other.ivUniqueId)
                && pose.equals(other.pose)
                && state == other.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vehicleId, owner, tierId, ivUniqueId, pose, state);
    }

    @Override
    public String toString() {
        return owner.username() + " T" + tierId + " " + vehicleId + " iv=" + ivUniqueId + " " + pose.formatShort();
    }
}
