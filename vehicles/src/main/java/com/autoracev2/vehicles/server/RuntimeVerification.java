package com.autoracev2.vehicles.server;

import com.autoracev2.vehicles.AutoRaceV2Vehicles;
import com.autoracev2.vehicles.api.AutoRaceV2VehiclesApi;
import com.autoracev2.vehicles.api.OwnerRef;
import com.autoracev2.vehicles.api.VehiclePose;
import com.autoracev2.vehicles.api.VehicleRecord;
import com.autoracev2.vehicles.api.VehicleResult;
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
            AutoRaceV2Vehicles.LOGGER.error("Runtime test skipped: overworld is missing.");
            return;
        }
        OwnerRef owner = OwnerRef.named("RuntimeTest");
        VehiclePose pose = new VehiclePose(
                overworld.dimension().location().toString(),
                8.0D,
                80.0D,
                8.0D,
                90.0F,
                0.0F
        );
        AutoRaceV2Vehicles.LOGGER.info("Runtime test: spawn T1");
        VehicleResult spawn = AutoRaceV2VehiclesApi.spawnVehicle(owner, 1, pose);
        AutoRaceV2Vehicles.LOGGER.info("Runtime test spawn: success={} {}", spawn.success(), spawn.message());
        spawn.vehicle().ifPresent(record -> logInfo(record));

        AutoRaceV2Vehicles.LOGGER.info("Runtime test: swap T3");
        VehicleResult swap = AutoRaceV2VehiclesApi.swapVehicleTier(owner, 3);
        AutoRaceV2Vehicles.LOGGER.info("Runtime test swap: success={} {}", swap.success(), swap.message());
        swap.vehicle().ifPresent(record -> {
            logInfo(record);
            if (!pose.equals(record.pose())) {
                AutoRaceV2Vehicles.LOGGER.error("Runtime test: swap pose mismatch {} vs {}", pose, record.pose());
            }
            if (AutoRaceV2VehiclesApi.getAllActiveVehicles().size() != 1) {
                AutoRaceV2Vehicles.LOGGER.error("Runtime test: expected 1 active vehicle, found {}",
                        AutoRaceV2VehiclesApi.getAllActiveVehicles().size());
            }
        });

        AutoRaceV2Vehicles.LOGGER.info("Runtime test: despawn");
        VehicleResult despawn = AutoRaceV2VehiclesApi.despawnVehicle(owner);
        AutoRaceV2Vehicles.LOGGER.info("Runtime test despawn: success={} {}", despawn.success(), despawn.message());

        AutoRaceV2VehiclesApi.spawnVehicle(owner, 1, pose);
        AutoRaceV2VehiclesApi.spawnVehicle(OwnerRef.named("RuntimeTest2"), 2, pose);
        VehicleResult despawnAll = AutoRaceV2VehiclesApi.despawnAllVehicles();
        AutoRaceV2Vehicles.LOGGER.info("Runtime test despawnall: success={} {} remaining={}",
                despawnAll.success(),
                despawnAll.message(),
                AutoRaceV2VehiclesApi.getAllActiveVehicles().size());
        AutoRaceV2Vehicles.LOGGER.info("Runtime test finished.");
    }

    private static void logInfo(VehicleRecord record) {
        AutoRaceV2Vehicles.LOGGER.info("Runtime test info: owner={} tier={} arv2={} iv={} {}",
                record.owner().username(),
                record.tierId(),
                record.vehicleId(),
                record.ivUniqueId(),
                record.pose().formatShort());
    }
}
