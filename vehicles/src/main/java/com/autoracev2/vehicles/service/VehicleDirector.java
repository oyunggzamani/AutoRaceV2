package com.autoracev2.vehicles.service;

import com.autoracev2.vehicles.api.OwnerRef;
import com.autoracev2.vehicles.api.TierDefinition;
import com.autoracev2.vehicles.api.VehicleRecord;
import com.autoracev2.vehicles.api.VehicleResult;
import com.autoracev2.vehicles.api.VehiclePose;
import com.autoracev2.vehicles.registry.VehicleTierRegistry;
import com.autoracev2.vehicles.runtime.SpawnedVehicleHandle;
import com.autoracev2.vehicles.runtime.VehicleRuntime;
import com.autoracev2.vehicles.store.OwnerVehicleMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Owner mapping + spawn/swap/despawn orchestration.
 *
 * Immersive Vehicles cannot swap two entities in one atomic world operation.
 * Sequence used here:
 * <ol>
 *   <li>Read the current pose (and later, momentum).</li>
 *   <li>Spawn the replacement first. The old vehicle stays in the world.</li>
 *   <li>If spawn fails, the owner map is unchanged.</li>
 *   <li>If spawn succeeds, the owner map is replaced in one put, then the old
 *       vehicle is removed.</li>
 *   <li>If old removal fails, the map already points at the new vehicle.
 *       The leftover entity is removed on the next despawn/despawnAll.</li>
 * </ol>
 */
public final class VehicleDirector {
    private final VehicleTierRegistry tiers;
    private final OwnerVehicleMap map;
    private final VehicleRuntime runtime;

    public VehicleDirector(VehicleTierRegistry tiers, OwnerVehicleMap map, VehicleRuntime runtime) {
        this.tiers = tiers;
        this.map = map;
        this.runtime = runtime;
    }

    public Optional<VehicleRecord> getVehicleForOwner(OwnerRef owner) {
        return map.get(owner);
    }

    public Optional<VehicleRecord> getVehicleForOwner(String username) {
        return map.get(username);
    }

    public List<VehicleRecord> getAllActiveVehicles() {
        return map.all();
    }

    public Optional<TierDefinition> getTierDefinition(int tierId) {
        return tiers.byId(tierId);
    }

    public Optional<TierDefinition> getTierForThreshold(long value) {
        return tiers.byThreshold(value);
    }

    public VehicleResult spawnVehicle(OwnerRef owner, int tierId, VehiclePose pose) {
        Optional<TierDefinition> tier = requireOwnerAndTier(owner, tierId);
        if (tier.isEmpty()) {
            if (owner == null) {
                return VehicleResult.fail("Unknown owner.");
            }
            return VehicleResult.fail("Invalid tier: " + tierId);
        }
        if (pose == null) {
            return VehicleResult.fail("Spawn pose is required.");
        }
        Optional<VehicleRecord> current = map.get(owner);
        return current.isPresent()
                ? replacePrepared(owner, current.get(), tier.get(), pose)
                : spawnFresh(owner, tier.get(), pose);
    }

    public VehicleResult swapVehicleTier(OwnerRef owner, int tierId) {
        if (owner == null) {
            return VehicleResult.fail("Unknown owner.");
        }
        Optional<VehicleRecord> current = map.get(owner);
        if (current.isEmpty()) {
            return VehicleResult.fail("Unknown owner: " + owner.username());
        }
        Optional<TierDefinition> tier = tiers.byId(tierId);
        if (tier.isEmpty()) {
            return VehicleResult.fail("Invalid tier: " + tierId);
        }
        VehicleRecord live = current.get();
        return replacePrepared(owner, live, tier.get(), live.pose());
    }

    public VehicleResult despawnVehicle(OwnerRef owner) {
        if (owner == null) {
            return VehicleResult.fail("Unknown owner.");
        }
        return despawnVehicle(owner.username());
    }

    public VehicleResult despawnVehicle(String username) {
        Optional<VehicleRecord> current = map.get(username);
        if (current.isEmpty()) {
            return VehicleResult.ok("No active vehicle for owner.", null);
        }
        VehicleRecord removed = map.remove(username).orElse(current.get());
        runtime.despawn(removed.ivUniqueId());
        return VehicleResult.ok("Despawned vehicle for " + removed.owner().username() + ".", removed.inactive());
    }

    public VehicleResult despawnAllVehicles() {
        List<VehicleRecord> snapshot = new ArrayList<>(map.clear());
        for (VehicleRecord record : snapshot) {
            runtime.despawn(record.ivUniqueId());
        }
        return VehicleResult.ok("Despawned " + snapshot.size() + " vehicle(s).", null);
    }

    private Optional<TierDefinition> requireOwnerAndTier(OwnerRef owner, int tierId) {
        if (owner == null) {
            return Optional.empty();
        }
        return tiers.byId(tierId);
    }

    private VehicleResult spawnFresh(OwnerRef owner, TierDefinition tier, VehiclePose pose) {
        Optional<SpawnedVehicleHandle> spawned = runtime.spawn(tier, pose);
        if (spawned.isEmpty()) {
            return VehicleResult.fail("Failed to spawn " + tier.displayName() + ".");
        }
        VehicleRecord record = VehicleRecord.active(owner, tier.id(), spawned.get().ivUniqueId(), spawned.get().pose());
        map.put(record);
        return VehicleResult.ok("Spawned " + tier.displayName() + " for " + owner.username() + ".", record);
    }

    private VehicleResult replacePrepared(OwnerRef owner, VehicleRecord current, TierDefinition tier, VehiclePose pose) {
        runtime.captureMomentum(current.ivUniqueId());
        Optional<SpawnedVehicleHandle> spawned = runtime.spawn(tier, pose);
        if (spawned.isEmpty()) {
            return VehicleResult.fail("Failed to spawn replacement " + tier.displayName() + ". Previous vehicle kept.");
        }
        VehicleRecord next = VehicleRecord.active(owner, tier.id(), spawned.get().ivUniqueId(), spawned.get().pose());
        map.put(next);
        runtime.despawn(current.ivUniqueId());
        return VehicleResult.replaced(
                "Swapped " + owner.username() + " to " + tier.displayName() + ".",
                next,
                current.inactive()
        );
    }
}
