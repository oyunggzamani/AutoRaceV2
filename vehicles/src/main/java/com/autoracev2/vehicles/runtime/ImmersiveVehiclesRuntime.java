package com.autoracev2.vehicles.runtime;

import com.autoracev2.vehicles.AutoRaceV2Vehicles;
import com.autoracev2.vehicles.api.EngineOverride;
import com.autoracev2.vehicles.api.TierDefinition;
import com.autoracev2.vehicles.api.VehiclePose;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Immersive Vehicles 1.20.1-24.0.0 adapter.
 *
 * Spawn sequence is the official {@code ItemVehicle.onBlockClicked} path from
 * DonBruce64/MinecraftTransportSimulator:
 * {@code new EntityVehicleF_Physics(world, player, item, data)}, set pose,
 * {@code world.spawnEntity(vehicle)}, {@code addPartsPostAddition}.
 *
 * Orientation uses {@code WrapperEntity.getOrientation()}: IV yaw = {@code -mcYaw}.
 * The +90 from block-face item placement is not applied, so race heading is kept.
 *
 * IV cannot replace two entities atomically. {@link com.autoracev2.vehicles.service.VehicleDirector}
 * spawns the new vehicle first, then remaps the owner, then removes the old one.
 */
public final class ImmersiveVehiclesRuntime implements VehicleRuntime {
    private static final String WRAPPER_WORLD = "mcinterface1201.WrapperWorld";
    private static final String BUILDER_ENTITY = "mcinterface1201.BuilderEntityExisting";
    private static final String VEHICLE_CLASS = "minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics";
    private static final String ITEM_VEHICLE = "minecrafttransportsimulator.items.instances.ItemVehicle";
    private static final String PACK_PARSER = "minecrafttransportsimulator.packloading.PackParser";
    private static final String POINT3D = "minecrafttransportsimulator.baseclasses.Point3D";
    private static final String PART_ENGINE = "minecrafttransportsimulator.entities.instances.PartEngine";

    private final Supplier<MinecraftServer> server;

    public ImmersiveVehiclesRuntime(Supplier<MinecraftServer> server) {
        this.server = server;
    }

    @Override
    public Optional<SpawnedVehicleHandle> spawn(TierDefinition tier, VehiclePose pose) {
        MinecraftServer minecraft = server.get();
        if (minecraft == null) {
            AutoRaceV2Vehicles.LOGGER.error("Cannot spawn vehicle: server is not running.");
            return Optional.empty();
        }
        ServerLevel level = levelOf(minecraft, pose.dimension());
        if (level == null) {
            AutoRaceV2Vehicles.LOGGER.error("Cannot spawn vehicle: unknown dimension {}.", pose.dimension());
            return Optional.empty();
        }
        try {
            Object world = wrapperWorld(level);
            if (world == null || (boolean) world.getClass().getMethod("isClient").invoke(world)) {
                AutoRaceV2Vehicles.LOGGER.error("Immersive Vehicles world wrapper is unavailable.");
                return Optional.empty();
            }
            Object item = resolveVehicleItem(tier);
            if (item == null) {
                return Optional.empty();
            }
            Class<?> vehicleClass = Class.forName(VEHICLE_CLASS);
            Constructor<?> constructor = vehicleClass.getConstructor(
                    Class.forName("minecrafttransportsimulator.mcinterface.AWrapperWorld"),
                    Class.forName("minecrafttransportsimulator.mcinterface.IWrapperPlayer"),
                    Class.forName(ITEM_VEHICLE),
                    Class.forName("minecrafttransportsimulator.mcinterface.IWrapperNBT")
            );
            Object vehicle = constructor.newInstance(world, null, item, null);
            applyPose(vehicle, pose);
            zeroMotion(vehicle);
            world.getClass().getMethod("spawnEntity", Class.forName("minecrafttransportsimulator.entities.components.AEntityB_Existing"))
                    .invoke(world, vehicle);
            vehicle.getClass().getMethod(
                    "addPartsPostAddition",
                    Class.forName("minecrafttransportsimulator.mcinterface.IWrapperPlayer"),
                    Class.forName("minecrafttransportsimulator.mcinterface.IWrapperNBT")
            ).invoke(vehicle, null, null);
            if (!readBoolean(vehicle, "isValid")) {
                AutoRaceV2Vehicles.LOGGER.error("IV entity was invalid after spawn for {}.", tier.packVehicleId());
                safeRemove(vehicle);
                return Optional.empty();
            }
            if (tier.engineOverride().isPresent() && !applyEngineOverride(vehicle, tier.engineOverride().get())) {
                AutoRaceV2Vehicles.LOGGER.error("Tuned engine override failed for {}. Rolling back spawn.", tier.displayName());
                safeRemove(vehicle);
                return Optional.empty();
            }
            UUID unique = (UUID) fieldValue(vehicle, "uniqueUUID");
            if (unique == null) {
                AutoRaceV2Vehicles.LOGGER.error("Spawned IV vehicle has no uniqueUUID.");
                safeRemove(vehicle);
                return Optional.empty();
            }
            AutoRaceV2Vehicles.LOGGER.info("Spawned {} iv={} at {}", tier.packVehicleId(), unique, pose.formatShort());
            return Optional.of(new SpawnedVehicleHandle(unique.toString(), pose));
        } catch (ClassNotFoundException missing) {
            AutoRaceV2Vehicles.LOGGER.error("Immersive Vehicles classes are not loaded. Install Immersive Vehicles 1.20.1-24.0.0.");
            return Optional.empty();
        } catch (ReflectiveOperationException exception) {
            AutoRaceV2Vehicles.LOGGER.error("Failed to spawn Immersive Vehicles entity for {}.", tier.packVehicleId(), exception);
            return Optional.empty();
        }
    }

    @Override
    public boolean despawn(String ivUniqueId) {
        if (ivUniqueId == null || ivUniqueId.isBlank()) {
            return true;
        }
        MinecraftServer minecraft = server.get();
        if (minecraft == null) {
            return true;
        }
        UUID wanted;
        try {
            wanted = UUID.fromString(ivUniqueId.trim());
        } catch (IllegalArgumentException ignored) {
            return true;
        }
        boolean removed = false;
        try {
            Class<?> builderClass = Class.forName(BUILDER_ENTITY);
            Field entityField = declaredField(builderClass, "entity");
            for (ServerLevel level : minecraft.getAllLevels()) {
                List<Entity> snapshot = new ArrayList<>();
                level.getEntities().getAll().forEach(snapshot::add);
                for (Entity entity : snapshot) {
                    if (!builderClass.isInstance(entity)) {
                        continue;
                    }
                    Object iv = entityField.get(entity);
                    if (iv == null) {
                        continue;
                    }
                    Object unique = fieldValue(iv, "uniqueUUID");
                    if (!(unique instanceof UUID uuid) || !wanted.equals(uuid)) {
                        continue;
                    }
                    safeRemove(iv);
                    if (entity.isAlive()) {
                        entity.discard();
                    }
                    removed = true;
                }
            }
        } catch (ClassNotFoundException ignored) {
            return true;
        } catch (ReflectiveOperationException exception) {
            AutoRaceV2Vehicles.LOGGER.warn("IV despawn scan failed for {}.", ivUniqueId, exception);
            return true;
        }
        if (!removed) {
            AutoRaceV2Vehicles.LOGGER.info("No live IV entity for {}, treating despawn as complete.", ivUniqueId);
        }
        return true;
    }

    @Override
    public Optional<VehiclePose> readLivePose(String ivUniqueId) {
        return findLiveIvVehicle(ivUniqueId).flatMap(this::poseOfLiveEntity);
    }

    @Override
    public boolean relocate(String ivUniqueId, VehiclePose pose) {
        if (pose == null) {
            return false;
        }
        Optional<Object> live = findLiveIvVehicle(ivUniqueId);
        if (live.isEmpty()) {
            return false;
        }
        try {
            applyPose(live.get(), pose);
            return true;
        } catch (ReflectiveOperationException exception) {
            AutoRaceV2Vehicles.LOGGER.warn("Failed to relocate live IV entity {}.", ivUniqueId, exception);
            return false;
        }
    }

    /**
     * Resolves the live IV vehicle by AutoRaceV2-mapped {@code uniqueUUID}.
     * Official lookup is {@code AWrapperWorld.getEntity(UUID)}; BuilderEntityExisting is fallback only.
     */
    private Optional<Object> findLiveIvVehicle(String ivUniqueId) {
        MinecraftServer minecraft = server.get();
        if (minecraft == null || ivUniqueId == null || ivUniqueId.isBlank()) {
            return Optional.empty();
        }
        UUID wanted;
        try {
            wanted = UUID.fromString(ivUniqueId.trim());
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
        try {
            Class<?> vehicleClass = Class.forName(VEHICLE_CLASS);
            for (ServerLevel level : minecraft.getAllLevels()) {
                Object world = wrapperWorld(level);
                if (world == null || (boolean) world.getClass().getMethod("isClient").invoke(world)) {
                    continue;
                }
                Object entity = world.getClass().getMethod("getEntity", UUID.class).invoke(world, wanted);
                if (isLiveVehicle(entity, vehicleClass)) {
                    return Optional.of(entity);
                }
            }
            Class<?> builderClass = Class.forName(BUILDER_ENTITY);
            Field entityField = declaredField(builderClass, "entity");
            for (ServerLevel level : minecraft.getAllLevels()) {
                List<Entity> snapshot = new ArrayList<>();
                level.getEntities().getAll().forEach(snapshot::add);
                for (Entity entity : snapshot) {
                    if (!builderClass.isInstance(entity)) {
                        continue;
                    }
                    Object iv = entityField.get(entity);
                    if (!isLiveVehicle(iv, vehicleClass)) {
                        continue;
                    }
                    Object unique = fieldValue(iv, "uniqueUUID");
                    if (unique instanceof UUID uuid && wanted.equals(uuid)) {
                        return Optional.of(iv);
                    }
                }
            }
        } catch (ClassNotFoundException ignored) {
            return Optional.empty();
        } catch (ReflectiveOperationException exception) {
            AutoRaceV2Vehicles.LOGGER.warn("Live IV lookup failed for {}.", ivUniqueId, exception);
            return Optional.empty();
        }
        return Optional.empty();
    }

    private Optional<VehiclePose> poseOfLiveEntity(Object vehicle) {
        try {
            if (!readBoolean(vehicle, "isValid")) {
                return Optional.empty();
            }
            Object world = fieldValue(vehicle, "world");
            Object position = fieldValue(vehicle, "position");
            Object orientation = fieldValue(vehicle, "orientation");
            if (world == null || position == null || orientation == null) {
                return Optional.empty();
            }
            String dimension = liveDimension(world);
            if (dimension == null || dimension.isBlank()) {
                return Optional.empty();
            }
            double x = ((Number) fieldValue(position, "x")).doubleValue();
            double y = ((Number) fieldValue(position, "y")).doubleValue();
            double z = ((Number) fieldValue(position, "z")).doubleValue();
            Object angles = fieldValue(orientation, "angles");
            if (angles == null) {
                return Optional.empty();
            }
            // WrapperEntity: IV pitch = mcPitch, IV yaw = -mcYaw.
            float pitch = ((Number) fieldValue(angles, "x")).floatValue();
            float yaw = (float) -((Number) fieldValue(angles, "y")).doubleValue();
            return Optional.of(new VehiclePose(dimension, x, y, z, yaw, pitch));
        } catch (ReflectiveOperationException exception) {
            AutoRaceV2Vehicles.LOGGER.warn("Failed to read live IV pose.", exception);
            return Optional.empty();
        }
    }

    private boolean isLiveVehicle(Object entity, Class<?> vehicleClass) throws ReflectiveOperationException {
        return entity != null && vehicleClass.isInstance(entity) && readBoolean(entity, "isValid");
    }

    /**
     * IV {@code WrapperWorld.getName()} is {@code Level.dimension().location().getPath()}
     * (e.g. {@code overworld}). Spawn/swap need the full key {@code minecraft:overworld},
     * so we read the live wrapper's MC {@code Level} when present.
     */
    private static String liveDimension(Object ivWorld) throws ReflectiveOperationException {
        try {
            Object mcLevel = declaredField(ivWorld.getClass(), "world").get(ivWorld);
            if (mcLevel instanceof Level level) {
                return level.dimension().location().toString();
            }
        } catch (NoSuchFieldException ignored) {
            // Fall through to getName() and normalize.
        }
        String name = (String) ivWorld.getClass().getMethod("getName").invoke(ivWorld);
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.contains(":") ? name : "minecraft:" + name;
    }

    private Object resolveVehicleItem(TierDefinition tier) throws ReflectiveOperationException {
        Class<?> parser = Class.forName(PACK_PARSER);
        Class<?> itemVehicle = Class.forName(ITEM_VEHICLE);
        Method getItem = parser.getMethod("getItem", String.class, String.class, String.class);
        for (String subName : List.of("", "_white")) {
            Object item = getItem.invoke(null, tier.packId(), tier.vehicleSystemName(), subName);
            if (item != null && itemVehicle.isInstance(item)) {
                return item;
            }
        }
        Object scanned = firstMatchingPackVehicle(parser, itemVehicle, tier);
        if (scanned != null) {
            return scanned;
        }
        AutoRaceV2Vehicles.LOGGER.error(
                "Immersive Vehicles item not found for {} (pack={}, systemName={}). Loaded packs: {}",
                tier.packVehicleId(),
                tier.packId(),
                tier.vehicleSystemName(),
                parser.getMethod("getAllPackIDs").invoke(null)
        );
        return null;
    }

    private static Object firstMatchingPackVehicle(Class<?> parser, Class<?> itemVehicle, TierDefinition tier) throws ReflectiveOperationException {
        @SuppressWarnings("unchecked")
        java.util.Set<String> packIds = (java.util.Set<String>) parser.getMethod("getAllPackIDs").invoke(null);
        if (packIds == null || !packIds.contains(tier.packId())) {
            return null;
        }
        Object packItems = parser.getMethod("getAllItemsForPack", String.class, boolean.class).invoke(null, tier.packId(), false);
        if (!(packItems instanceof Collection<?> items)) {
            return null;
        }
        for (Object packItem : items) {
            if (!itemVehicle.isInstance(packItem)) {
                continue;
            }
            Object definition = fieldValue(packItem, "definition");
            Object systemName = definition == null ? null : fieldValue(definition, "systemName");
            if (tier.vehicleSystemName().equals(String.valueOf(systemName))) {
                return packItem;
            }
        }
        return null;
    }

    private void applyPose(Object vehicle, VehiclePose pose) throws ReflectiveOperationException {
        Object position = fieldValue(vehicle, "position");
        Object prevPosition = fieldValue(vehicle, "prevPosition");
        invokeSet(position, pose.x(), pose.y(), pose.z());
        invokeSet(prevPosition, pose.x(), pose.y(), pose.z());
        Class<?> pointClass = Class.forName(POINT3D);
        Object angles = pointClass.getConstructor(double.class, double.class, double.class)
                .newInstance((double) pose.pitch(), (double) -pose.yaw(), 0.0D);
        Object orientation = fieldValue(vehicle, "orientation");
        Object prevOrientation = fieldValue(vehicle, "prevOrientation");
        orientation.getClass().getMethod("setToAngles", pointClass).invoke(orientation, angles);
        prevOrientation.getClass().getMethod("set", orientation.getClass()).invoke(prevOrientation, orientation);
    }

    private void zeroMotion(Object vehicle) throws ReflectiveOperationException {
        Object motion = fieldValue(vehicle, "motion");
        Object prevMotion = fieldValue(vehicle, "prevMotion");
        invokeSet(motion, 0.0D, 0.0D, 0.0D);
        prevMotion.getClass().getMethod("set", motion.getClass()).invoke(prevMotion, motion);
    }

    private boolean applyEngineOverride(Object vehicle, EngineOverride override) throws ReflectiveOperationException {
        Class<?> parser = Class.forName(PACK_PARSER);
        Object engineItem = parser.getMethod("getItem", String.class, String.class, String.class)
                .invoke(null, override.packId(), override.partSystemName(), "");
        if (engineItem == null) {
            AutoRaceV2Vehicles.LOGGER.error("Tuned engine item missing: {}.", override.packPartId());
            return false;
        }
        List<Object> engines = collectEngines(vehicle);
        if (engines.isEmpty()) {
            AutoRaceV2Vehicles.LOGGER.error("Spawned GT-R has no engine parts to override.");
            return false;
        }
        Method removePart = findMethod(vehicle.getClass(), "removePart", 3);
        Method addPartFromStack = findMethod(vehicle.getClass(), "addPartFromStack", 5);
        Method getNewStack = engineItem.getClass().getMethod("getNewStack", Class.forName("minecrafttransportsimulator.mcinterface.IWrapperNBT"));
        boolean replaced = false;
        for (Object engine : engines) {
            Object slot = fieldValue(engine, "placementSlot");
            if (!(slot instanceof Integer placementSlot)) {
                continue;
            }
            removePart.invoke(vehicle, engine, false, true);
            Object stack = getNewStack.invoke(engineItem, new Object[]{null});
            Object added = addPartFromStack.invoke(vehicle, stack, null, placementSlot, true, false);
            if (added == null) {
                AutoRaceV2Vehicles.LOGGER.error("Could not attach {} in slot {}.", override.packPartId(), placementSlot);
                return false;
            }
            replaced = true;
        }
        return replaced;
    }

    private List<Object> collectEngines(Object vehicle) throws ReflectiveOperationException {
        List<Object> engines = new ArrayList<>();
        Class<?> engineClass = Class.forName(PART_ENGINE);
        Object enginesField = fieldValue(vehicle, "engines");
        addIfEngine(engines, enginesField, engineClass);
        Object partsField = fieldValue(vehicle, "parts");
        addIfEngine(engines, partsField, engineClass);
        return engines;
    }

    private static void addIfEngine(List<Object> out, Object container, Class<?> engineClass) {
        if (container instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (engineClass.isInstance(item) && !out.contains(item)) {
                    out.add(item);
                }
            }
        }
    }

    private static void invokeSet(Object point, double x, double y, double z) throws ReflectiveOperationException {
        point.getClass().getMethod("set", double.class, double.class, double.class).invoke(point, x, y, z);
    }

    private static void safeRemove(Object ivEntity) {
        try {
            ivEntity.getClass().getMethod("remove").invoke(ivEntity);
        } catch (ReflectiveOperationException ignored) {
            // Despawn must stay safe if the entity is already gone.
        }
    }

    private static boolean readBoolean(Object target, String name) throws ReflectiveOperationException {
        Object value = fieldValue(target, name);
        return value instanceof Boolean flag && flag;
    }

    private static Object fieldValue(Object target, String name) throws ReflectiveOperationException {
        return declaredField(target.getClass(), name).get(target);
    }

    private static Field declaredField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Method findMethod(Class<?> type, String name, int parameterCount) throws NoSuchMethodException {
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                    method.setAccessible(true);
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchMethodException(name);
    }

    private static Object wrapperWorld(Level level) throws ReflectiveOperationException {
        return Class.forName(WRAPPER_WORLD).getMethod("getWrapperFor", Level.class).invoke(null, level);
    }

    private static ServerLevel levelOf(MinecraftServer server, String dimension) {
        ResourceLocation location = ResourceLocation.tryParse(dimension);
        if (location == null) {
            return null;
        }
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, location));
    }
}
