package com.autoracev2.waypoints.command;

import com.autoracev2.waypoints.api.Waypoint;
import com.autoracev2.waypoints.debug.DebugNetwork;
import com.autoracev2.waypoints.server.ServerContext;
import com.autoracev2.waypoints.store.ClearConfirmGuard;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class WaypointCommands {
    private WaypointCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("arv2wp")
                .requires(CommandSupport::isAdmin)
                .then(Commands.literal("add").executes(ctx -> add(ctx.getSource(), CommandSupport.player(ctx))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(ctx -> remove(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("clear")
                        .executes(ctx -> requestClear(ctx.getSource()))
                        .then(Commands.literal("confirm").executes(ctx -> confirmClear(ctx.getSource()))))
                .then(Commands.literal("show").executes(ctx -> show(CommandSupport.player(ctx), true)))
                .then(Commands.literal("hide").executes(ctx -> show(CommandSupport.player(ctx), false)))
        );
    }

    private static int add(CommandSourceStack source, ServerPlayer player) {
        if (!CommandSupport.ensureReady(source)) {
            return 0;
        }
        Waypoint waypoint = ServerContext.storage().waypoints().add(CommandSupport.positionOf(player));
        return CommandSupport.persistAndOk(source, ServerContext::persistWaypoints,
                waypoint.label() + " eklendi. (" + waypoint.position().formatShort() + ")",
                "Waypoint kaydedilemedi");
    }

    private static int remove(CommandSourceStack source, int id) {
        if (!CommandSupport.ensureReady(source)) {
            return 0;
        }
        return ServerContext.storage().waypoints().remove(id)
                .map(removed -> CommandSupport.persistAndOk(source, ServerContext::persistWaypoints,
                        removed.label() + " silindi. Kalan waypointler yeniden numaralandi.",
                        "Waypoint kaydedilemedi"))
                .orElseGet(() -> CommandSupport.fail(source, "Waypoint bulunamadi: WP" + id));
    }

    private static int list(CommandSourceStack source) {
        if (!CommandSupport.ensureReady(source)) {
            return 0;
        }
        List<Waypoint> waypoints = ServerContext.storage().waypoints().all();
        if (waypoints.isEmpty()) {
            CommandSupport.info(source, "Kayitli waypoint yok.", ChatFormatting.YELLOW);
            return 1;
        }
        CommandSupport.info(source, "Waypoints (" + waypoints.size() + "):", ChatFormatting.AQUA);
        for (Waypoint waypoint : waypoints) {
            CommandSupport.info(source, "  " + waypoint, ChatFormatting.WHITE);
        }
        return waypoints.size();
    }

    private static int requestClear(CommandSourceStack source) {
        if (!CommandSupport.ensureReady(source)) {
            return 0;
        }
        ServerContext.waypointClear().request(CommandSupport.actorId(source));
        CommandSupport.info(source, "Tekrar /arv2wp clear confirm yaz.", ChatFormatting.YELLOW);
        return 1;
    }

    private static int confirmClear(CommandSourceStack source) {
        if (!CommandSupport.ensureReady(source)) {
            return 0;
        }
        ClearConfirmGuard.Outcome outcome = ServerContext.waypointClear().confirm(CommandSupport.actorId(source));
        return switch (outcome) {
            case CONFIRMED -> {
                ServerContext.storage().waypoints().clear();
                yield CommandSupport.persistAndOk(source, ServerContext::persistWaypoints,
                        "Tum waypointler temizlendi.",
                        "Waypoint kaydedilemedi");
            }
            case EXPIRED -> CommandSupport.fail(source, "Onay suresi doldu. Once /arv2wp clear yaz.");
            case NOT_PENDING, REQUESTED -> CommandSupport.fail(source, "Onay beklenmiyor. Once /arv2wp clear yaz.");
        };
    }

    private static int show(ServerPlayer player, boolean visible) {
        CommandSourceStack source = player.createCommandSourceStack();
        if (!CommandSupport.ensureReady(source)) {
            return 0;
        }
        ServerContext.debugVisibility().toggleWaypoints(player.getUUID(), visible);
        DebugNetwork.syncPlayer(player, ServerContext.storage(), ServerContext.debugVisibility());
        return CommandSupport.ok(source,
                visible ? "Waypoint debug gosterimi acildi." : "Waypoint debug gosterimi kapatildi.",
                ChatFormatting.AQUA);
    }
}
