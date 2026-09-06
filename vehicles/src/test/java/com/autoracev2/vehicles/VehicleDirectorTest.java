package com.autoracev2.vehicles;

import com.autoracev2.vehicles.api.OwnerRef;
import com.autoracev2.vehicles.api.VehiclePose;
import com.autoracev2.vehicles.api.VehicleRecord;
import com.autoracev2.vehicles.api.VehicleResult;
import com.autoracev2.vehicles.registry.VehicleTierRegistry;
import com.autoracev2.vehicles.runtime.InMemoryVehicleRuntime;
import com.autoracev2.vehicles.service.VehicleDirector;
import com.autoracev2.vehicles.store.OwnerVehicleMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleDirectorTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final VehiclePose START = new VehiclePose(OVERWORLD, 10.5, 64.0, -3.25, 45.0F, -8.0F);

    private InMemoryVehicleRuntime runtime;
    private VehicleDirector director;

    @BeforeEach
    void setUp() {
        runtime = new InMemoryVehicleRuntime();
        director = new VehicleDirector(new VehicleTierRegistry(), new OwnerVehicleMap(), runtime);
    }

    @Test
    void spawnAddsOwnerMapping() {
        VehicleResult result = director.spawnVehicle(OwnerRef.named("Ali"), 1, START);
        assertTrue(result.success());
        VehicleRecord record = director.getVehicleForOwner("Ali").orElseThrow();
        assertEquals(1, record.tierId());
        assertEquals(START, record.pose());
        assertTrue(runtime.isLive(record.ivUniqueId()));
        assertEquals(1, director.getAllActiveVehicles().size());
    }

    @Test
    void spawnReplaceKeepsOneActiveVehicle() {
        director.spawnVehicle(OwnerRef.named("Ali"), 1, START);
        String oldIv = director.getVehicleForOwner("Ali").orElseThrow().ivUniqueId();
        VehiclePose nextPose = new VehiclePose(OVERWORLD, 20, 70, 8, 10, 2);
        VehicleResult replaced = director.spawnVehicle(OwnerRef.named("Ali"), 3, nextPose);
        assertTrue(replaced.success());
        VehicleRecord live = director.getVehicleForOwner("Ali").orElseThrow();
        assertEquals(3, live.tierId());
        assertEquals(nextPose, live.pose());
        assertNotEquals(oldIv, live.ivUniqueId());
        assertFalse(runtime.isLive(oldIv));
        assertEquals(1, director.getAllActiveVehicles().size());
        assertEquals(1, runtime.liveCount());
    }

    @Test
    void swapPreservesDimensionPositionAndOrientation() {
        director.spawnVehicle(OwnerRef.named("Ali"), 1, START);
        VehicleRecord before = director.getVehicleForOwner("Ali").orElseThrow();
        VehicleResult swap = director.swapVehicleTier(OwnerRef.named("Ali"), 6);
        assertTrue(swap.success());
        VehicleRecord after = director.getVehicleForOwner("Ali").orElseThrow();
        assertEquals(6, after.tierId());
        assertEquals(START.dimension(), after.pose().dimension());
        assertEquals(START.x(), after.pose().x());
        assertEquals(START.y(), after.pose().y());
        assertEquals(START.z(), after.pose().z());
        assertEquals(START.yaw(), after.pose().yaw());
        assertEquals(START.pitch(), after.pose().pitch());
        assertNotEquals(before.vehicleId(), after.vehicleId());
        assertNotEquals(before.ivUniqueId(), after.ivUniqueId());
        assertFalse(runtime.isLive(before.ivUniqueId()));
        assertTrue(runtime.isLive(after.ivUniqueId()));
        assertEquals(1, runtime.liveCount());
    }

    @Test
    void swapDoesNotLeaveStaleOldMapping() {
        director.spawnVehicle(OwnerRef.named("Ali"), 2, START);
        VehicleRecord old = director.getVehicleForOwner("Ali").orElseThrow();
        director.swapVehicleTier(OwnerRef.named("Ali"), 4);
        VehicleRecord live = director.getVehicleForOwner("Ali").orElseThrow();
        assertEquals(4, live.tierId());
        assertNotEquals(old.ivUniqueId(), live.ivUniqueId());
        assertEquals(1, director.getAllActiveVehicles().stream()
                .filter(record -> record.owner().username().equals("Ali"))
                .count());
    }

    @Test
    void failedReplacementKeepsOldVehicle() {
        director.spawnVehicle(OwnerRef.named("Ali"), 1, START);
        VehicleRecord old = director.getVehicleForOwner("Ali").orElseThrow();
        runtime.failNextSpawn();
        VehicleResult failed = director.swapVehicleTier(OwnerRef.named("Ali"), 5);
        assertFalse(failed.success());
        assertEquals(old.vehicleId(), director.getVehicleForOwner("Ali").orElseThrow().vehicleId());
        assertTrue(runtime.isLive(old.ivUniqueId()));
    }

    @Test
    void unknownOwnerSwapAndInfo() {
        assertTrue(director.getVehicleForOwner("ghost").isEmpty());
        VehicleResult swap = director.swapVehicleTier(OwnerRef.named("ghost"), 1);
        assertFalse(swap.success());
        assertTrue(swap.message().contains("Unknown owner"));
    }

    @Test
    void invalidTierDoesNotSpawn() {
        VehicleResult result = director.spawnVehicle(OwnerRef.named("Ali"), 99, START);
        assertFalse(result.success());
        assertTrue(director.getVehicleForOwner("Ali").isEmpty());
        assertEquals(0, runtime.liveCount());
    }

    @Test
    void repeatedDespawnIsSafe() {
        director.spawnVehicle(OwnerRef.named("Ali"), 1, START);
        assertTrue(director.despawnVehicle("Ali").success());
        assertTrue(director.getVehicleForOwner("Ali").isEmpty());
        assertTrue(director.despawnVehicle("Ali").success());
        assertTrue(director.despawnVehicle(OwnerRef.named("Ali")).success());
        assertFalse(director.despawnVehicle((OwnerRef) null).success());
        assertEquals(0, runtime.liveCount());
    }

    @Test
    void despawnAllClearsEveryOwner() {
        director.spawnVehicle(OwnerRef.named("Ali"), 1, START);
        director.spawnVehicle(OwnerRef.named("Furkan"), 2, START);
        assertEquals(2, director.getAllActiveVehicles().size());
        assertTrue(director.despawnAllVehicles().success());
        assertTrue(director.getAllActiveVehicles().isEmpty());
        assertEquals(0, runtime.liveCount());
        assertTrue(director.despawnAllVehicles().success());
    }

    @Test
    void ownersStayIsolatedDuringSwap() {
        director.spawnVehicle(OwnerRef.named("Ali"), 1, START);
        director.spawnVehicle(OwnerRef.named("Furkan"), 2, START);
        String furkan = director.getVehicleForOwner("Furkan").orElseThrow().ivUniqueId();
        director.swapVehicleTier(OwnerRef.named("Ali"), 6);
        assertEquals(furkan, director.getVehicleForOwner("Furkan").orElseThrow().ivUniqueId());
        assertEquals(6, director.getVehicleForOwner("Ali").orElseThrow().tierId());
        assertEquals(2, director.getAllActiveVehicles().size());
    }
}
