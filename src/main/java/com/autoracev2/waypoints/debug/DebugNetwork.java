package com.autoracev2.waypoints.debug;

import com.autoracev2.waypoints.AutoRaceV2Waypoints;
import com.autoracev2.waypoints.api.Checkpoint;
import com.autoracev2.waypoints.api.Waypoint;
import com.autoracev2.waypoints.persist.TrackStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class DebugNetwork {
    private static final String PROTOCOL = "1";
    private static SimpleChannel channel;

    private DebugNetwork() {
    }

    public static void register() {
        channel = NetworkRegistry.newSimpleChannel(
                ResourceLocation.fromNamespaceAndPath(AutoRaceV2Waypoints.MOD_ID, "debug"),
                () -> PROTOCOL,
                PROTOCOL::equals,
                PROTOCOL::equals
        );
        channel.registerMessage(
                0,
                DebugSyncPacket.class,
                DebugSyncPacket::encode,
                DebugSyncPacket::decode,
                DebugSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void syncPlayer(ServerPlayer player, TrackStorage storage, DebugVisibility visibility) {
        UUID id = player.getUUID();
        boolean showWaypoints = visibility.showsWaypoints(id);
        boolean showCheckpoints = visibility.showsCheckpoints(id);
        List<DebugMarker> markers = new ArrayList<>();
        if (showWaypoints) {
            for (Waypoint waypoint : storage.waypoints().all()) {
                markers.add(new DebugMarker(DebugMarker.WAYPOINT, waypoint.label(), waypoint.position()));
            }
        }
        if (showCheckpoints) {
            for (Checkpoint checkpoint : storage.checkpoints().all()) {
                markers.add(new DebugMarker(DebugMarker.CHECKPOINT, checkpoint.label(), checkpoint.position()));
            }
            storage.checkpoints().start().ifPresent(position ->
                    markers.add(new DebugMarker(DebugMarker.START, "START", position)));
            storage.checkpoints().finish().ifPresent(position ->
                    markers.add(new DebugMarker(DebugMarker.FINISH, "FINISH", position)));
        }
        channel.send(PacketDistributor.PLAYER.with(() -> player),
                new DebugSyncPacket(showWaypoints, showCheckpoints, markers));
    }

    public static void syncAllViewers(TrackStorage storage, DebugVisibility visibility) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (UUID viewerId : visibility.allViewers()) {
            ServerPlayer player = server.getPlayerList().getPlayer(viewerId);
            if (player != null) {
                syncPlayer(player, storage, visibility);
            }
        }
    }
}
