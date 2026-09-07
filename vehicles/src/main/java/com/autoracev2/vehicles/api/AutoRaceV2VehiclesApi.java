package com.autoracev2.vehicles.api;

import com.autoracev2.vehicles.registry.VehicleTierRegistry;
import com.autoracev2.vehicles.service.VehicleDirector;

import java.util.List;
import java.util.Optional;

/**
 * Public entry for later AutoRaceV2 modules. Commands are not required.
 */
public final class AutoRaceV2VehiclesApi {
    private static volatile VehicleDirector director;
    private static volatile VehicleTierRegistry tiers = new VehicleTierRegistry();

    private AutoRaceV2VehiclesApi() {
    }

    public static void bind(VehicleDirector nextDirector, VehicleTierRegistry nextTiers) {
        director = nextDirector;
        tiers = nextTiers != null ? nextTiers : new VehicleTierRegistry();
    }

    public static void unbind() {
        director = null;
        tiers = new VehicleTierRegistry();
    }

    public static boolean isReady() {
        return director != null;
    }

    public static Optional<VehicleRecord> getVehicleForOwner(OwnerRef owner) {
        return director == null ? Optional.empty() : director.getVehicleForOwner(owner);
    }

    public static Optional<VehicleRecord> getVehicleForOwner(String username) {
        return director == null ? Optional.empty() : director.getVehicleForOwner(username);
    }

    /**
     * Owner mapping without live IV pose overlay. Used to distinguish unknown owner vs stale entity.
     */
    public static Optional<VehicleRecord> getMappedVehicleForOwner(String username) {
        return director == null ? Optional.empty() : director.getMappedVehicleForOwner(username);
    }

    public static List<VehicleRecord> getAllActiveVehicles() {
        return director == null ? List.of() : director.getAllActiveVehicles();
    }

    public static VehicleResult spawnVehicle(OwnerRef owner, int tierId, VehiclePose pose) {
        if (director == null) {
            return VehicleResult.fail("AutoRaceV2 Vehicles is not loaded.");
        }
        return director.spawnVehicle(owner, tierId, pose);
    }

    public static VehicleResult swapVehicleTier(OwnerRef owner, int tierId) {
        if (director == null) {
            return VehicleResult.fail("AutoRaceV2 Vehicles is not loaded.");
        }
        return director.swapVehicleTier(owner, tierId);
    }

    public static VehicleResult despawnVehicle(OwnerRef owner) {
        if (director == null) {
            return VehicleResult.fail("AutoRaceV2 Vehicles is not loaded.");
        }
        return director.despawnVehicle(owner);
    }

    public static VehicleResult despawnVehicle(String username) {
        if (director == null) {
            return VehicleResult.fail("AutoRaceV2 Vehicles is not loaded.");
        }
        return director.despawnVehicle(username);
    }

    public static VehicleResult despawnAllVehicles() {
        if (director == null) {
            return VehicleResult.fail("AutoRaceV2 Vehicles is not loaded.");
        }
        return director.despawnAllVehicles();
    }

    public static Optional<TierDefinition> getTierDefinition(int tierId) {
        return tiers.byId(tierId);
    }

    public static Optional<TierDefinition> getTierForThreshold(long value) {
        return tiers.byThreshold(value);
    }

    public static List<TierDefinition> getAllTiers() {
        return tiers.all();
    }
}
