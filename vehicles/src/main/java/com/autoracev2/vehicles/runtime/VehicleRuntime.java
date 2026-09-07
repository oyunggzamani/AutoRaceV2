package com.autoracev2.vehicles.runtime;

import com.autoracev2.vehicles.api.TierDefinition;
import com.autoracev2.vehicles.api.VehicleMomentum;
import com.autoracev2.vehicles.api.VehiclePose;

import java.util.Optional;

/**
 * World-facing adapter. Unit tests use {@link InMemoryVehicleRuntime}.
 * The Forge adapter talks to Immersive Vehicles without leaking IV types.
 */
public interface VehicleRuntime {
    Optional<SpawnedVehicleHandle> spawn(TierDefinition tier, VehiclePose pose);

    boolean despawn(String ivUniqueId);

    /**
     * Current live IV pose for {@code uniqueUUID}. Empty when the entity is gone or invalid.
     * Spawn coordinates must not be used as a substitute.
     */
    Optional<VehiclePose> readLivePose(String ivUniqueId);

    /**
     * Test/runtime-verification helper: move the live IV entity. Production commands never call this.
     */
    default boolean relocate(String ivUniqueId, VehiclePose pose) {
        return false;
    }

    default VehicleMomentum captureMomentum(String ivUniqueId) {
        return VehicleMomentum.NONE;
    }
}
