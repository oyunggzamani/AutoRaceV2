package com.autoracev2.vehicles.runtime;

import com.autoracev2.vehicles.api.TierDefinition;
import com.autoracev2.vehicles.api.VehiclePose;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Pure in-memory world used by unit tests. No Immersive Vehicles types.
 */
public final class InMemoryVehicleRuntime implements VehicleRuntime {
    private final Map<String, SpawnedVehicleHandle> live = new LinkedHashMap<>();
    private boolean nextSpawnFails;
    private boolean nextDespawnFails;

    public void failNextSpawn() {
        nextSpawnFails = true;
    }

    public void failNextDespawn() {
        nextDespawnFails = true;
    }

    public boolean isLive(String ivUniqueId) {
        return live.containsKey(ivUniqueId);
    }

    public int liveCount() {
        return live.size();
    }

    /**
     * Removes the live entity without going through {@link #despawn}, simulating a stale IV reference.
     */
    public void dropLive(String ivUniqueId) {
        if (ivUniqueId != null) {
            live.remove(ivUniqueId);
        }
    }

    @Override
    public Optional<SpawnedVehicleHandle> spawn(TierDefinition tier, VehiclePose pose) {
        if (tier == null || pose == null) {
            return Optional.empty();
        }
        if (nextSpawnFails) {
            nextSpawnFails = false;
            return Optional.empty();
        }
        SpawnedVehicleHandle handle = new SpawnedVehicleHandle(UUID.randomUUID().toString(), pose);
        live.put(handle.ivUniqueId(), handle);
        return Optional.of(handle);
    }

    @Override
    public boolean despawn(String ivUniqueId) {
        if (ivUniqueId == null || ivUniqueId.isBlank()) {
            return true;
        }
        if (nextDespawnFails) {
            nextDespawnFails = false;
            return false;
        }
        live.remove(ivUniqueId);
        return true;
    }

    @Override
    public Optional<VehiclePose> readLivePose(String ivUniqueId) {
        if (ivUniqueId == null || ivUniqueId.isBlank()) {
            return Optional.empty();
        }
        SpawnedVehicleHandle handle = live.get(ivUniqueId);
        return handle == null ? Optional.empty() : Optional.of(handle.pose());
    }

    @Override
    public boolean relocate(String ivUniqueId, VehiclePose pose) {
        if (ivUniqueId == null || ivUniqueId.isBlank() || pose == null) {
            return false;
        }
        SpawnedVehicleHandle current = live.get(ivUniqueId);
        if (current == null) {
            return false;
        }
        live.put(ivUniqueId, new SpawnedVehicleHandle(ivUniqueId, pose));
        return true;
    }
}
