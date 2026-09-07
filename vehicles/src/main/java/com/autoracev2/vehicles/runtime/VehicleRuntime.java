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

    default VehicleMomentum captureMomentum(String ivUniqueId) {
        return VehicleMomentum.NONE;
    }
}
