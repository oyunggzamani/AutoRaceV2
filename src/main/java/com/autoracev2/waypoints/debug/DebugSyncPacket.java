package com.autoracev2.waypoints.debug;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class DebugSyncPacket {
    private final boolean showWaypoints;
    private final boolean showCheckpoints;
    private final List<DebugMarker> markers;

    public DebugSyncPacket(boolean showWaypoints, boolean showCheckpoints, List<DebugMarker> markers) {
        this.showWaypoints = showWaypoints;
        this.showCheckpoints = showCheckpoints;
        this.markers = List.copyOf(markers);
    }

    public boolean showWaypoints() {
        return showWaypoints;
    }

    public boolean showCheckpoints() {
        return showCheckpoints;
    }

    public List<DebugMarker> markers() {
        return markers;
    }

    public static void encode(DebugSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.showWaypoints);
        buf.writeBoolean(packet.showCheckpoints);
        buf.writeVarInt(packet.markers.size());
        for (DebugMarker marker : packet.markers) {
            marker.encode(buf);
        }
    }

    public static DebugSyncPacket decode(FriendlyByteBuf buf) {
        boolean showWaypoints = buf.readBoolean();
        boolean showCheckpoints = buf.readBoolean();
        int count = buf.readVarInt();
        List<DebugMarker> markers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            markers.add(DebugMarker.decode(buf));
        }
        return new DebugSyncPacket(showWaypoints, showCheckpoints, markers);
    }

    public static void handle(DebugSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientDebugState.accept(packet)));
        context.setPacketHandled(true);
    }
}
