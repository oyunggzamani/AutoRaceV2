package com.autoracev2.vehicles.server;

import com.autoracev2.vehicles.AutoRaceV2Vehicles;
import com.autoracev2.vehicles.api.AutoRaceV2VehiclesApi;
import com.autoracev2.vehicles.registry.VehicleTierRegistry;
import com.autoracev2.vehicles.runtime.ImmersiveVehiclesRuntime;
import com.autoracev2.vehicles.service.VehicleDirector;
import com.autoracev2.vehicles.store.OwnerVehicleMap;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public final class VehicleServerContext {
    private static MinecraftServer server;
    private static VehicleDirector director;

    private VehicleServerContext() {
    }

    public static void start(MinecraftServer minecraft) {
        server = minecraft;
        VehicleTierRegistry tiers = new VehicleTierRegistry();
        director = new VehicleDirector(tiers, new OwnerVehicleMap(), new ImmersiveVehiclesRuntime(() -> server));
        AutoRaceV2VehiclesApi.bind(director, tiers);
        Path directory = FMLPaths.CONFIGDIR.get().resolve("autoracev2");
        AutoRaceV2Vehicles.LOGGER.info("AutoRaceV2 Vehicles ready. Config directory: {}", directory.toAbsolutePath());
        AutoRaceV2Vehicles.LOGGER.info("Locked tiers: {}", tiers.size());
    }

    public static void runRuntimeVerification(MinecraftServer minecraft) {
        RuntimeVerification.runIfRequested(minecraft);
    }

    public static void stop() {
        if (director != null) {
            director.despawnAllVehicles();
        }
        AutoRaceV2VehiclesApi.unbind();
        director = null;
        server = null;
    }

    public static boolean isReady() {
        return director != null;
    }

    public static VehicleDirector director() {
        return director;
    }

    public static MinecraftServer server() {
        return server;
    }
}
