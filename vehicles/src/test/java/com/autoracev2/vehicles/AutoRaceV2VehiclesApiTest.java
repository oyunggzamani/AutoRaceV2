package com.autoracev2.vehicles;

import com.autoracev2.vehicles.api.AutoRaceV2VehiclesApi;
import com.autoracev2.vehicles.api.OwnerRef;
import com.autoracev2.vehicles.api.VehiclePose;
import com.autoracev2.vehicles.registry.VehicleTierRegistry;
import com.autoracev2.vehicles.runtime.InMemoryVehicleRuntime;
import com.autoracev2.vehicles.service.VehicleDirector;
import com.autoracev2.vehicles.store.OwnerVehicleMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoRaceV2VehiclesApiTest {
    @BeforeEach
    void bind() {
        AutoRaceV2VehiclesApi.bind(
                new VehicleDirector(new VehicleTierRegistry(), new OwnerVehicleMap(), new InMemoryVehicleRuntime()),
                new VehicleTierRegistry()
        );
    }

    @AfterEach
    void unbind() {
        AutoRaceV2VehiclesApi.unbind();
    }

    @Test
    void publicLookupDoesNotNeedCommands() {
        assertEquals(7, AutoRaceV2VehiclesApi.getAllTiers().size());
        assertEquals(1, AutoRaceV2VehiclesApi.getTierForThreshold(0).orElseThrow().id());
        assertTrue(AutoRaceV2VehiclesApi.spawnVehicle(
                OwnerRef.named("Ali"),
                1,
                new VehiclePose("minecraft:overworld", 0, 64, 0, 0, 0)
        ).success());
        assertEquals(1, AutoRaceV2VehiclesApi.getVehicleForOwner("Ali").orElseThrow().tierId());
        assertTrue(AutoRaceV2VehiclesApi.swapVehicleTier(OwnerRef.named("Ali"), 2).success());
        assertEquals(2, AutoRaceV2VehiclesApi.getVehicleForOwner("Ali").orElseThrow().tierId());
        assertTrue(AutoRaceV2VehiclesApi.despawnAllVehicles().success());
        assertTrue(AutoRaceV2VehiclesApi.getAllActiveVehicles().isEmpty());
    }
}
