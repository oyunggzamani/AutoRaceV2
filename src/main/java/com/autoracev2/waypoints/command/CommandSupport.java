package com.autoracev2.waypoints.command;

import com.autoracev2.waypoints.AutoRaceV2Waypoints;
import com.autoracev2.waypoints.api.WorldPosition;
import com.autoracev2.waypoints.server.ServerContext;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;

final class CommandSupport {
    static final int ADMIN_PERMISSION = 2;

    private CommandSupport() {
    }

    static boolean isAdmin(CommandSourceStack source) {
        return source.hasPermission(ADMIN_PERMISSION);
    }

    static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message).withStyle(ChatFormatting.RED));
        return 0;
    }

    static int ok(CommandSourceStack source, String message, ChatFormatting color) {
        source.sendSuccess(() -> Component.literal(message).withStyle(color), true);
        return 1;
    }

    static int ok(CommandSourceStack source, String message) {
        return ok(source, message, ChatFormatting.GREEN);
    }

    static void info(CommandSourceStack source, String message, ChatFormatting color) {
        source.sendSuccess(() -> Component.literal(message).withStyle(color), false);
    }

    static boolean ensureReady(CommandSourceStack source) {
        if (!ServerContext.isReady()) {
            fail(source, "AutoRaceV2 veri katmani henuz yuklenmedi.");
            return false;
        }
        return true;
    }

    static ServerPlayer player(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return context.getSource().getPlayerOrException();
    }

    static WorldPosition positionOf(ServerPlayer player) {
        return new WorldPosition(
                player.level().dimension().location().toString(),
                player.getX(),
                player.getY(),
                player.getZ()
        );
    }

    static String actorId(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return player != null ? player.getUUID().toString() : "console";
    }

    static int persist(CommandSourceStack source, PersistAction action, String failedMessage) {
        try {
            action.run();
            return 1;
        } catch (IOException exception) {
            AutoRaceV2Waypoints.LOGGER.error(failedMessage, exception);
            fail(source, failedMessage + ": " + exception.getMessage());
            return 0;
        }
    }

    static int persistAndOk(CommandSourceStack source, PersistAction action, String success, String failedMessage) {
        int saved = persist(source, action, failedMessage);
        if (saved == 0) {
            return 0;
        }
        return ok(source, success);
    }

    @FunctionalInterface
    interface PersistAction {
        void run() throws IOException;
    }
}
