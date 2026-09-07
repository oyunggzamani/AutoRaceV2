package com.autoracev2.vehicles.command;

import com.autoracev2.vehicles.AutoRaceV2Vehicles;
import com.autoracev2.vehicles.api.AutoRaceV2VehiclesApi;
import com.autoracev2.vehicles.api.OwnerRef;
import com.autoracev2.vehicles.api.TierDefinition;
import com.autoracev2.vehicles.api.VehicleRecord;
import com.autoracev2.vehicles.api.VehicleResult;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class VehicleCommands {
    private VehicleCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("arv2veh")
                .requires(CommandSupport::isAdmin)
                .then(Commands.literal("spawn")
                        .then(Commands.argument("owner", StringArgumentType.word())
                                .then(Commands.argument("tier", IntegerArgumentType.integer())
                                        .executes(ctx -> spawn(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "owner"),
                                                IntegerArgumentType.getInteger(ctx, "tier"))))))
                .then(Commands.literal("swap")
                        .then(Commands.argument("owner", StringArgumentType.word())
                                .then(Commands.argument("tier", IntegerArgumentType.integer())
                                        .executes(ctx -> swap(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "owner"),
                                                IntegerArgumentType.getInteger(ctx, "tier"))))))
                .then(Commands.literal("despawn")
                        .then(Commands.argument("owner", StringArgumentType.word())
                                .executes(ctx -> despawn(ctx.getSource(), StringArgumentType.getString(ctx, "owner")))))
                .then(Commands.literal("despawnall").executes(ctx -> despawnAll(ctx.getSource())))
                .then(Commands.literal("info")
                        .then(Commands.argument("owner", StringArgumentType.word())
                                .executes(ctx -> info(ctx.getSource(), StringArgumentType.getString(ctx, "owner")))))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
        );
        AutoRaceV2Vehicles.LOGGER.info("Registered /arv2veh test commands");
    }

    private static int spawn(CommandSourceStack source, String ownerName, int tier) {
        if (!CommandSupport.ensureReady(source)) {
            return 0;
        }
        ServerPlayer actor = source.getPlayer();
        if (actor == null) {
            return CommandSupport.fail(source, "Spawn needs an in-world player position.");
        }
        OwnerRef owner = CommandSupport.owner(source, ownerName);
        VehicleResult result = AutoRaceV2VehiclesApi.spawnVehicle(owner, tier, CommandSupport.poseOf(actor));
        return CommandSupport.report(source, result);
    }

    private static int swap(CommandSourceStack source, String ownerName, int tier) {
        if (!CommandSupport.ensureReady(source)) {
            return 0;
        }
        return CommandSupport.report(source, AutoRaceV2VehiclesApi.swapVehicleTier(CommandSupport.owner(source, ownerName), tier));
    }

    private static int despawn(CommandSourceStack source, String ownerName) {
        if (!CommandSupport.ensureReady(source)) {
            return 0;
        }
        return CommandSupport.report(source, AutoRaceV2VehiclesApi.despawnVehicle(ownerName));
    }

    private static int despawnAll(CommandSourceStack source) {
        if (!CommandSupport.ensureReady(source)) {
            return 0;
        }
        return CommandSupport.report(source, AutoRaceV2VehiclesApi.despawnAllVehicles());
    }

    private static int info(CommandSourceStack source, String ownerName) {
        if (!CommandSupport.ensureReady(source)) {
            return 0;
        }
        return AutoRaceV2VehiclesApi.getVehicleForOwner(ownerName)
                .map(record -> {
                    CommandSupport.info(source, format(record), ChatFormatting.AQUA);
                    AutoRaceV2VehiclesApi.getTierDefinition(record.tierId()).ifPresent(tier ->
                            CommandSupport.info(source, "  " + tier, ChatFormatting.WHITE));
                    return 1;
                })
                .orElseGet(() -> {
                    if (AutoRaceV2VehiclesApi.getMappedVehicleForOwner(ownerName).isPresent()) {
                        return CommandSupport.fail(
                                source,
                                "Live IV entity not found for " + ownerName
                                        + ". Mapping is stale; spawn coordinates are not current position."
                        );
                    }
                    return CommandSupport.fail(source, "Unknown owner: " + ownerName);
                });
    }

    private static int list(CommandSourceStack source) {
        if (!CommandSupport.ensureReady(source)) {
            return 0;
        }
        List<VehicleRecord> vehicles = AutoRaceV2VehiclesApi.getAllActiveVehicles();
        if (vehicles.isEmpty()) {
            CommandSupport.info(source, "No active AutoRaceV2 vehicles.", ChatFormatting.YELLOW);
            return 1;
        }
        CommandSupport.info(source, "Active vehicles (" + vehicles.size() + "):", ChatFormatting.AQUA);
        for (VehicleRecord record : vehicles) {
            CommandSupport.info(source, "  " + format(record), ChatFormatting.WHITE);
        }
        return vehicles.size();
    }

    private static String format(VehicleRecord record) {
        String tierName = AutoRaceV2VehiclesApi.getTierDefinition(record.tierId())
                .map(TierDefinition::displayName)
                .orElse("T" + record.tierId());
        return record.owner().username()
                + " tier=" + record.tierId()
                + " (" + tierName + ")"
                + " arv2=" + record.vehicleId()
                + " iv=" + record.ivUniqueId()
                + " " + record.pose().formatShort();
    }
}
