package com.autoracev2.waypoints;

import com.autoracev2.waypoints.command.CheckpointCommands;
import com.autoracev2.waypoints.command.WaypointCommands;
import com.autoracev2.waypoints.debug.DebugNetwork;
import com.autoracev2.waypoints.server.ServerContext;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(AutoRaceV2Waypoints.MOD_ID)
public class AutoRaceV2Waypoints {
    public static final String MOD_ID = "autoracev2_waypoints";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AutoRaceV2Waypoints() {
        MinecraftForge.EVENT_BUS.register(this);
        DebugNetwork.register();
        LOGGER.info("AutoRaceV2 Waypoints 1.0.0 loaded");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ServerContext.start(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        ServerContext.stop();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        WaypointCommands.register(event.getDispatcher());
        CheckpointCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerContext.debugVisibility().remove(event.getEntity().getUUID());
        String actor = event.getEntity().getUUID().toString();
        ServerContext.waypointClear().cancel(actor);
        ServerContext.checkpointClear().cancel(actor);
    }
}
