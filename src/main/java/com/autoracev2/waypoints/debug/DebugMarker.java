package com.autoracev2.waypoints.debug;

import com.autoracev2.waypoints.api.WorldPosition;
import net.minecraft.network.FriendlyByteBuf;

public final class DebugMarker {
    public static final byte WAYPOINT = 0;
    public static final byte CHECKPOINT = 1;
    public static final byte START = 2;
    public static final byte FINISH = 3;

    private final byte kind;
    private final String label;
    private final String dimension;
    private final double x;
    private final double y;
    private final double z;

    public DebugMarker(byte kind, String label, WorldPosition position) {
        this(kind, label, position.dimension(), position.x(), position.y(), position.z());
    }

    public DebugMarker(byte kind, String label, String dimension, double x, double y, double z) {
        this.kind = kind;
        this.label = label;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public byte kind() {
        return kind;
    }

    public String label() {
        return label;
    }

    public String dimension() {
        return dimension;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(kind);
        buf.writeUtf(label);
        buf.writeUtf(dimension);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    public static DebugMarker decode(FriendlyByteBuf buf) {
        return new DebugMarker(
                buf.readByte(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
        );
    }
}
