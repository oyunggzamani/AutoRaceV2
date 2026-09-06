package com.autoracev2.waypoints.debug;

public final class ClientDebugState {
    private static DebugSyncPacket current = new DebugSyncPacket(false, false, java.util.List.of());

    private ClientDebugState() {
    }

    public static void accept(DebugSyncPacket packet) {
        current = packet;
    }

    public static DebugSyncPacket current() {
        return current;
    }

    public static boolean hasVisibleMarkers() {
        return current.showWaypoints() || current.showCheckpoints();
    }
}
