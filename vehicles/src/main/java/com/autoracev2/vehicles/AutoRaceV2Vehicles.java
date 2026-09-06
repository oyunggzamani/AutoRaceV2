package com.autoracev2.vehicles;

import com.autoracev2.vehicles.command.VehicleCommands;
import com.autoracev2.vehicles.server.VehicleServerContext;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(AutoRaceV2Vehicles.MOD_ID)
public class AutoRaceV2Vehicles {
    public static final String MOD_ID = "autoracev2_vehicles";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AutoRaceV2Vehicles() {
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("AutoRaceV2 Vehicles 1.0.0 loaded");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        VehicleServerContext.start(event.getServer());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        VehicleServerContext.runRuntimeVerification(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        VehicleServerContext.stop();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        VehicleCommands.register(event.getDispatcher());
    }
}
