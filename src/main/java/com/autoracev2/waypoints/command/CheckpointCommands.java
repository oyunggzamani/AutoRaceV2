package com.autoracev2.waypoints.command;

import com.autoracev2.waypoints.api.Checkpoint;
import com.autoracev2.waypoints.api.WorldPosition;
import com.autoracev2.waypoints.debug.DebugNetwork;
import com.autoracev2.waypoints.server.ServerContext;
import com.autoracev2.waypoints.store.CheckpointStore;
import com.autoracev2.waypoints.store.ClearConfirmGuard;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class CheckpointCommands {
    private CheckpointCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("arv2cp")
                .requires(CommandSupport::isAdmin)
                .then(Commands.literal("add").executes(ctx -> add(ctx.getSource(), CommandSupport.player(ctx))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(ctx -> remove(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("clear")
                        .executes(ctx -> requestClear(ctx.getSource()))
                        .then(Commands.literal("confirm").executes(ctx -> confirmClear(ctx.getSource()))))
                .then(Commands.literal("setstart").executes(ctx -> setStart(ctx.getSource(), CommandSupport.player(ctx))))
                .then(Commands.literal("setfinish").executes(ctx -> setFinish(ctx.getSource(), CommandSupport.player(ctx))))
                .then(Commands.literal("info").executes(ctx -> info(ctx.getSource())))
                .then(Commands.literal("show").executes(ctx -> show(CommandSupport.player(ctx), true)))
                .then(Commands.literal("hide").executes(ctx -> show(CommandSupport.player(ctx), false)))
        );
    }

    private static int add(CommandSourceStack source, ServerPlayer player) {
        if (!CommandSupport.ensureReady(source)) {
            return 0;
        }
        Checkpoint checkpoint = ServerContext.storage().checkpoints().add(CommandSupport.positionOf(player));
        return CommandSupport.persistAndOk(source, ServerContext::persistCheckpoints,
                checkpoint.label() + " eklendi. (" + checkpoint.position().formatShort() + ")",
                "Checkpoint kaydedilemedi");
    }

    private static int remove(CommandSourceStack source, int id) {
        if (!CommandSupport.ensureReady(source)) {
            return 0;
        }
        return ServerContext.storage().checkpoints().remove(id)
                .map(removed -> CommandSupport.persistAndOk(source, ServerContext::persistCheckpoints,
                        removed.label() + " silindi. Kalan checkpointler yeniden numaralandi.",
                        "Checkpoint kaydedilemedi"))
                .orElseGet(() -> CommandSupport.fail(source, "Checkpoint bulunamadi: CP" + id));
    }

    private static int list(CommandSourceStack source) {
        if (!CommandSupport.ensureReady(source)) {
            return 0;
        }
        List<Checkpoint> checkpoints = ServerContext.storage().checkpoints().all();
        if (checkpoints.isEmpty()) {
            CommandSupport.info(source, "Kayitli checkpoint yok.", ChatFormatting.YELLOW);
            return 1;
        }
        CommandSupport.info(source, "Checkpoints (" + checkpoints.size() + "):", ChatFormatting.GOLD);
        for (Checkpoint checkpoint : checkpoints) {
            CommandSupport.info(source, "  " + checkpoint, ChatFormatting.WHITE);
        }
        return checkpoints.size();
    }

    private static int requestClear(CommandSourceStack source) {
        if (!CommandSupport.ensureReady(source)) {
            return 0;
        }
        ServerContext.checkpointClear().request(CommandSupport.actorId(source));
        CommandSupport.info(source, "Tekrar /arv2cp clear confirm yaz.", ChatFormatting.YELLOW);
        return 1;
    }

    private static int confirmClear(CommandSourceStack source) {
        if (!CommandSupport.ensureReady(source)) {
            return 0;
        }
        ClearConfirmGuard.Outcome outcome = ServerContext.checkpointClear().confirm(CommandSupport.actorId(source));
        return switch (outcome) {
            case CONFIRMED -> {
                ServerContext.storage().checkpoints().clearCheckpoints();
                yield CommandSupport.persistAndOk(source, ServerContext::persistCheckpoints,
                        "Tum checkpointler temizlendi.",
                        "Checkpoint kaydedilemedi");
            }
            case EXPIRED -> CommandSupport.fail(source, "Onay suresi doldu. Once /arv2cp clear yaz.");
            case NOT_PENDING, REQUESTED -> CommandSupport.fail(source, "Onay beklenmiyor. Once /arv2cp clear yaz.");
        };
    }

    private static int setStart(CommandSourceStack source, ServerPlayer player) {
        if (!CommandSupport.ensureReady(source)) {
            return 0;
        }
        WorldPosition position = CommandSupport.positionOf(player);
        ServerContext.storage().checkpoints().setStart(position);
        return CommandSupport.persistAndOk(source, ServerContext::persistCheckpoints,
                "START kaydedildi. (" + position.formatShort() + ")",
                "START kaydedilemedi");
    }

    private static int setFinish(CommandSourceStack source, ServerPlayer player) {
        if (!CommandSupport.ensureReady(source)) {
            return 0;
        }
        WorldPosition position = CommandSupport.positionOf(player);
        ServerContext.storage().checkpoints().setFinish(position);
        return CommandSupport.persistAndOk(source, ServerContext::persistCheckpoints,
                "FINISH kaydedildi. (" + position.formatShort() + ")",
                "FINISH kaydedilemedi");
    }

    private static int info(CommandSourceStack source) {
        if (!CommandSupport.ensureReady(source)) {
            return 0;
        }
        CheckpointStore store = ServerContext.storage().checkpoints();
        CommandSupport.info(source, "START: " + store.start().map(WorldPosition::formatShort).orElse("yok"), ChatFormatting.GREEN);
        CommandSupport.info(source, "FINISH: " + store.finish().map(WorldPosition::formatShort).orElse("yok"), ChatFormatting.RED);
        CommandSupport.info(source, "Checkpoint sayisi: " + store.count(), ChatFormatting.GOLD);
        return 1;
    }

    private static int show(ServerPlayer player, boolean visible) {
        CommandSourceStack source = player.createCommandSourceStack();
        if (!CommandSupport.ensureReady(source)) {
            return 0;
        }
        ServerContext.debugVisibility().toggleCheckpoints(player.getUUID(), visible);
        DebugNetwork.syncPlayer(player, ServerContext.storage(), ServerContext.debugVisibility());
        return CommandSupport.ok(source,
                visible ? "Checkpoint debug gosterimi acildi." : "Checkpoint debug gosterimi kapatildi.",
                ChatFormatting.GOLD);
    }
}
