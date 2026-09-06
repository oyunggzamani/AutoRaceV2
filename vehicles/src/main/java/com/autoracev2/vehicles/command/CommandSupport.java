package com.autoracev2.vehicles.command;

import com.autoracev2.vehicles.api.OwnerRef;
import com.autoracev2.vehicles.api.VehiclePose;
import com.autoracev2.vehicles.api.VehicleResult;
import com.autoracev2.vehicles.server.VehicleServerContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class CommandSupport {
    static final int ADMIN_PERMISSION = 2;

    private CommandSupport() {
    }

    static boolean isAdmin(CommandSourceStack source) {
        return source.hasPermission(ADMIN_PERMISSION);
    }

    static boolean ensureReady(CommandSourceStack source) {
        if (!VehicleServerContext.isReady()) {
            fail(source, "AutoRaceV2 Vehicles is not ready.");
            return false;
        }
        return true;
    }

    static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message).withStyle(ChatFormatting.RED));
        return 0;
    }

    static int ok(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    static void info(CommandSourceStack source, String message, ChatFormatting color) {
        source.sendSuccess(() -> Component.literal(message).withStyle(color), false);
    }

    static int report(CommandSourceStack source, VehicleResult result) {
        if (!result.success()) {
            return fail(source, result.message());
        }
        return ok(source, result.message());
    }

    static OwnerRef owner(CommandSourceStack source, String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        ServerPlayer online = source.getServer() == null ? null : source.getServer().getPlayerList().getPlayerByName(username);
        return online == null ? OwnerRef.named(username) : new OwnerRef(username, online.getUUID());
    }

    static VehiclePose poseOf(ServerPlayer player) {
        return new VehiclePose(
                player.level().dimension().location().toString(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
        );
    }
}
