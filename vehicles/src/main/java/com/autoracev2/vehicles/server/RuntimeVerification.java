package com.autoracev2.vehicles.server;

import com.autoracev2.vehicles.AutoRaceV2Vehicles;
import com.autoracev2.vehicles.api.AutoRaceV2VehiclesApi;
import com.autoracev2.vehicles.api.OwnerRef;
import com.autoracev2.vehicles.api.VehiclePose;
import com.autoracev2.vehicles.api.VehicleRecord;
import com.autoracev2.vehicles.api.VehicleResult;
import com.autoracev2.vehicles.service.VehicleDirector;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Optional in-world check used only when {@code -Dautoracev2.vehicles.runtimeTest=true}.
 * Normal play never runs this.
 */
final class RuntimeVerification {
    private RuntimeVerification() {
    }

    static void runIfRequested(MinecraftServer server) {
        if (!Boolean.getBoolean("autoracev2.vehicles.runtimeTest")) {
            return;
        }
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            fail("overworld is missing");
            return;
        }
        OwnerRef owner = OwnerRef.named("RuntimeTest");
        String dimension = overworld.dimension().location().toString();
        VehiclePose spawnPose = new VehiclePose(dimension, 8.0D, 80.0D, 8.0D, 90.0F, 0.0F);
        VehiclePose movedPose = new VehiclePose(dimension, 48.0D, 81.0D, -22.0D, 135.0F, -12.0F);

        AutoRaceV2Vehicles.LOGGER.info("Runtime test: spawn T1");
        VehicleResult spawn = AutoRaceV2VehiclesApi.spawnVehicle(owner, 1, spawnPose);
        AutoRaceV2Vehicles.LOGGER.info("Runtime test spawn: success={} {}", spawn.success(), spawn.message());
        if (!spawn.success()) {
            fail("spawn T1 failed: " + spawn.message());
            return;
        }
        VehicleRecord afterSpawn = AutoRaceV2VehiclesApi.getVehicleForOwner(owner.username()).orElse(null);
        if (afterSpawn == null) {
            fail("info after spawn returned no live vehicle");
            return;
        }
        logInfo("after spawn", afterSpawn);
        if (!posesMatch(spawnPose, afterSpawn.pose())) {
            fail("info after spawn did not match spawn pose. expected=" + spawnPose.formatShort()
                    + " actual=" + afterSpawn.pose().formatShort());
            cleanup();
            return;
        }

        AutoRaceV2Vehicles.LOGGER.info("Runtime test: move live IV entity");
        VehicleDirector director = VehicleServerContext.director();
        if (director == null || !director.relocateLiveVehicle(afterSpawn.ivUniqueId(), movedPose)) {
            fail("could not relocate the live IV entity");
            cleanup();
            return;
        }
        VehicleRecord afterMove = AutoRaceV2VehiclesApi.getVehicleForOwner(owner.username()).orElse(null);
        if (afterMove == null) {
            fail("info after move returned no live vehicle");
            cleanup();
            return;
        }
        logInfo("after move", afterMove);
        if (posesMatch(spawnPose, afterMove.pose())) {
            fail("info still reports spawn coordinates after the live vehicle moved");
            cleanup();
            return;
        }
        if (!posesMatch(movedPose, afterMove.pose())) {
            fail("info after move did not match live pose. expected=" + movedPose.formatShort()
                    + " actual=" + afterMove.pose().formatShort());
            cleanup();
            return;
        }
        VehiclePose liveBeforeSwap = afterMove.pose();
        String oldIv = afterMove.ivUniqueId();

        AutoRaceV2Vehicles.LOGGER.info("Runtime test: swap T2 at current live pose");
        VehicleResult swap = AutoRaceV2VehiclesApi.swapVehicleTier(owner, 2);
        AutoRaceV2Vehicles.LOGGER.info("Runtime test swap: success={} {}", swap.success(), swap.message());
        if (!swap.success()) {
            fail("swap failed: " + swap.message());
            cleanup();
            return;
        }
        VehicleRecord afterSwap = AutoRaceV2VehiclesApi.getVehicleForOwner(owner.username()).orElse(null);
        if (afterSwap == null) {
            fail("info after swap returned no live vehicle");
            cleanup();
            return;
        }
        logInfo("after swap", afterSwap);
        if (oldIv.equals(afterSwap.ivUniqueId())) {
            fail("mapping still points at the pre-swap IV uniqueUUID");
            cleanup();
            return;
        }
        if (afterSwap.tierId() != 2) {
            fail("swap did not update tier, found T" + afterSwap.tierId());
            cleanup();
            return;
        }
        if (!posesMatch(liveBeforeSwap, afterSwap.pose())) {
            fail("replacement spawned at the wrong pose. expected=" + liveBeforeSwap.formatShort()
                    + " actual=" + afterSwap.pose().formatShort());
            cleanup();
            return;
        }
        if (posesMatch(spawnPose, afterSwap.pose())) {
            fail("replacement spawned at the original spawn point instead of the moved location");
            cleanup();
            return;
        }
        if (AutoRaceV2VehiclesApi.getAllActiveVehicles().size() != 1) {
            fail("expected 1 active vehicle after swap, found "
                    + AutoRaceV2VehiclesApi.getAllActiveVehicles().size());
            cleanup();
            return;
        }

        AutoRaceV2Vehicles.LOGGER.info("RUNTIME_LIVE_POSE: PASS");
        AutoRaceV2Vehicles.LOGGER.info("Runtime test: despawn");
        VehicleResult despawn = AutoRaceV2VehiclesApi.despawnVehicle(owner);
        AutoRaceV2Vehicles.LOGGER.info("Runtime test despawn: success={} {}", despawn.success(), despawn.message());

        AutoRaceV2VehiclesApi.spawnVehicle(owner, 1, spawnPose);
        AutoRaceV2VehiclesApi.spawnVehicle(OwnerRef.named("RuntimeTest2"), 2, spawnPose);
        VehicleResult despawnAll = AutoRaceV2VehiclesApi.despawnAllVehicles();
        AutoRaceV2Vehicles.LOGGER.info("Runtime test despawnall: success={} {} remaining={}",
                despawnAll.success(),
                despawnAll.message(),
                AutoRaceV2VehiclesApi.getAllActiveVehicles().size());
        AutoRaceV2Vehicles.LOGGER.info("Runtime test finished.");
    }

    private static void cleanup() {
        AutoRaceV2VehiclesApi.despawnAllVehicles();
    }

    private static void fail(String reason) {
        AutoRaceV2Vehicles.LOGGER.error("RUNTIME_LIVE_POSE: FAIL {}", reason);
    }

    private static void logInfo(String step, VehicleRecord record) {
        AutoRaceV2Vehicles.LOGGER.info("Runtime test info {}: owner={} tier={} arv2={} iv={} {}",
                step,
                record.owner().username(),
                record.tierId(),
                record.vehicleId(),
                record.ivUniqueId(),
                record.pose().formatShort());
    }

    private static boolean posesMatch(VehiclePose expected, VehiclePose actual) {
        return expected.dimension().equals(actual.dimension())
                && Math.abs(expected.x() - actual.x()) < 0.01D
                && Math.abs(expected.y() - actual.y()) < 0.01D
                && Math.abs(expected.z() - actual.z()) < 0.01D
                && Math.abs(expected.yaw() - actual.yaw()) < 0.05F
                && Math.abs(expected.pitch() - actual.pitch()) < 0.05F;
    }
}
