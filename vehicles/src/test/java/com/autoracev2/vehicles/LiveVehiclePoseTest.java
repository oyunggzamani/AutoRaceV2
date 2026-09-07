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

class LiveVehiclePoseTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final VehiclePose SPAWN = new VehiclePose(OVERWORLD, 10.5, 64.0, -3.25, 45.0F, -8.0F);
    private static final VehiclePose MOVED = new VehiclePose(OVERWORLD, 48.0, 81.0, -22.0, 135.0F, -12.0F);

    private InMemoryVehicleRuntime runtime;
    private VehicleDirector director;

    @BeforeEach
    void setUp() {
        runtime = new InMemoryVehicleRuntime();
        director = new VehicleDirector(new VehicleTierRegistry(), new OwnerVehicleMap(), runtime);
    }

    @Test
    void infoReportsInitialSpawnPosition() {
        assertTrue(director.spawnVehicle(OwnerRef.named("Ali"), 1, SPAWN).success());
        VehicleRecord live = director.getVehicleForOwner("Ali").orElseThrow();
        assertEquals(SPAWN, live.pose());
        assertEquals(SPAWN, director.getMappedVehicleForOwner("Ali").orElseThrow().pose());
    }

    @Test
    void livePositionChangeDoesNotUseStoredSpawnPose() {
        director.spawnVehicle(OwnerRef.named("Ali"), 1, SPAWN);
        String iv = director.getVehicleForOwner("Ali").orElseThrow().ivUniqueId();
        assertTrue(runtime.relocate(iv, MOVED));
        assertEquals(SPAWN, director.getMappedVehicleForOwner("Ali").orElseThrow().pose());
        assertEquals(MOVED, director.getVehicleForOwner("Ali").orElseThrow().pose());
    }

    @Test
    void infoReturnsUpdatedLivePosition() {
        director.spawnVehicle(OwnerRef.named("Ali"), 1, SPAWN);
        String iv = director.getVehicleForOwner("Ali").orElseThrow().ivUniqueId();
        runtime.relocate(iv, MOVED);
        VehicleRecord info = director.getVehicleForOwner("Ali").orElseThrow();
        assertEquals(MOVED.dimension(), info.pose().dimension());
        assertEquals(MOVED.x(), info.pose().x());
        assertEquals(MOVED.y(), info.pose().y());
        assertEquals(MOVED.z(), info.pose().z());
        assertEquals(MOVED.yaw(), info.pose().yaw());
        assertEquals(MOVED.pitch(), info.pose().pitch());
        assertEquals(1, info.tierId());
        assertEquals(iv, info.ivUniqueId());
    }

    @Test
    void swapAfterMovementUsesCurrentLivePositionAndOrientation() {
        director.spawnVehicle(OwnerRef.named("Ali"), 1, SPAWN);
        VehicleRecord before = director.getVehicleForOwner("Ali").orElseThrow();
        runtime.relocate(before.ivUniqueId(), MOVED);
        VehicleResult swap = director.swapVehicleTier(OwnerRef.named("Ali"), 2);
        assertTrue(swap.success());
        VehicleRecord after = director.getVehicleForOwner("Ali").orElseThrow();
        assertEquals(2, after.tierId());
        assertEquals(MOVED, after.pose());
        assertEquals(MOVED.yaw(), after.pose().yaw());
        assertEquals(MOVED.pitch(), after.pose().pitch());
        assertNotEquals(before.vehicleId(), after.vehicleId());
        assertNotEquals(before.ivUniqueId(), after.ivUniqueId());
        assertFalse(runtime.isLive(before.ivUniqueId()));
        assertTrue(runtime.isLive(after.ivUniqueId()));
        assertEquals(after.ivUniqueId(), director.getMappedVehicleForOwner("Ali").orElseThrow().ivUniqueId());
        assertEquals(1, director.getAllActiveVehicles().size());
        assertEquals(1, runtime.liveCount());
    }

    @Test
    void yawAndPitchArePreservedThroughMoveAndSwap() {
        VehiclePose angled = new VehiclePose(OVERWORLD, 12.0, 70.0, 4.5, -27.5F, 11.25F);
        director.spawnVehicle(OwnerRef.named("Ali"), 3, SPAWN);
        runtime.relocate(director.getVehicleForOwner("Ali").orElseThrow().ivUniqueId(), angled);
        assertTrue(director.swapVehicleTier(OwnerRef.named("Ali"), 5).success());
        VehiclePose live = director.getVehicleForOwner("Ali").orElseThrow().pose();
        assertEquals(angled.yaw(), live.yaw());
        assertEquals(angled.pitch(), live.pitch());
        assertEquals(angled.x(), live.x());
        assertEquals(angled.y(), live.y());
        assertEquals(angled.z(), live.z());
    }

    @Test
    void mappingUpdatesToNewLiveEntityAfterSwap() {
        director.spawnVehicle(OwnerRef.named("Ali"), 1, SPAWN);
        String oldIv = director.getVehicleForOwner("Ali").orElseThrow().ivUniqueId();
        runtime.relocate(oldIv, MOVED);
        director.swapVehicleTier(OwnerRef.named("Ali"), 4);
        VehicleRecord mapped = director.getMappedVehicleForOwner("Ali").orElseThrow();
        VehicleRecord live = director.getVehicleForOwner("Ali").orElseThrow();
        assertEquals(4, mapped.tierId());
        assertEquals(live.ivUniqueId(), mapped.ivUniqueId());
        assertNotEquals(oldIv, mapped.ivUniqueId());
        assertEquals(MOVED, live.pose());
    }

    @Test
    void swapAfterMovementLeavesExactlyOneActiveVehicle() {
        director.spawnVehicle(OwnerRef.named("Ali"), 1, SPAWN);
        director.spawnVehicle(OwnerRef.named("Furkan"), 2, SPAWN);
        runtime.relocate(director.getVehicleForOwner("Ali").orElseThrow().ivUniqueId(), MOVED);
        director.swapVehicleTier(OwnerRef.named("Ali"), 6);
        assertEquals(1, director.getAllActiveVehicles().stream()
                .filter(record -> record.owner().username().equals("Ali"))
                .count());
        assertEquals(2, director.getAllActiveVehicles().size());
        assertEquals(2, runtime.liveCount());
        assertEquals(2, director.getVehicleForOwner("Furkan").orElseThrow().tierId());
    }

    @Test
    void staleEntityDoesNotUseSpawnCoordinatesForInfoOrSwap() {
        director.spawnVehicle(OwnerRef.named("Ali"), 1, SPAWN);
        VehicleRecord mapped = director.getMappedVehicleForOwner("Ali").orElseThrow();
        runtime.dropLive(mapped.ivUniqueId());
        assertTrue(director.getVehicleForOwner("Ali").isEmpty());
        assertTrue(director.getAllActiveVehicles().isEmpty());
        assertEquals(mapped.vehicleId(), director.getMappedVehicleForOwner("Ali").orElseThrow().vehicleId());
        VehicleResult swap = director.swapVehicleTier(OwnerRef.named("Ali"), 2);
        assertFalse(swap.success());
        assertTrue(swap.message().contains("stale"));
        assertEquals(0, runtime.liveCount());
        assertEquals(mapped.ivUniqueId(), director.getMappedVehicleForOwner("Ali").orElseThrow().ivUniqueId());
        assertEquals(SPAWN, director.getMappedVehicleForOwner("Ali").orElseThrow().pose());
    }
}
