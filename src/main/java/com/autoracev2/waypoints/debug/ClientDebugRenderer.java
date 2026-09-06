package com.autoracev2.waypoints.debug;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Lightweight client-only debug overlay. No entities are spawned.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientDebugRenderer {
    private static final double MAX_DISTANCE_SQ = 128.0 * 128.0;

    private ClientDebugRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        if (!ClientDebugState.hasVisibleMarkers()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        String dimension = minecraft.level.dimension().location().toString();
        PoseStack pose = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        VertexConsumer lines = buffer.getBuffer(RenderType.lines());
        for (DebugMarker marker : ClientDebugState.current().markers()) {
            if (!marker.dimension().equals(dimension) || tooFar(marker, cameraPos)) {
                continue;
            }
            float[] color = colorFor(marker.kind());
            AABB box = new AABB(marker.x() - 0.30, marker.y(), marker.z() - 0.30,
                    marker.x() + 0.30, marker.y() + 0.60, marker.z() + 0.30)
                    .move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            AABB beam = new AABB(marker.x() - 0.04, marker.y(), marker.z() - 0.04,
                    marker.x() + 0.04, marker.y() + beamHeight(marker.kind()), marker.z() + 0.04)
                    .move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            LevelRenderer.renderLineBox(pose, lines, box, color[0], color[1], color[2], 1.0f);
            LevelRenderer.renderLineBox(pose, lines, beam, color[0], color[1], color[2], 0.75f);
        }
        buffer.endBatch(RenderType.lines());

        for (DebugMarker marker : ClientDebugState.current().markers()) {
            if (!marker.dimension().equals(dimension) || tooFar(marker, cameraPos)) {
                continue;
            }
            renderLabel(pose, camera, marker.label(), marker.x(), marker.y() + 1.35, marker.z(), labelColor(marker.kind()));
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static boolean tooFar(DebugMarker marker, Vec3 cameraPos) {
        double dx = marker.x() - cameraPos.x;
        double dy = marker.y() - cameraPos.y;
        double dz = marker.z() - cameraPos.z;
        return (dx * dx) + (dy * dy) + (dz * dz) > MAX_DISTANCE_SQ;
    }

    private static float[] colorFor(byte kind) {
        return switch (kind) {
            case DebugMarker.CHECKPOINT -> new float[]{1.00f, 0.54f, 0.08f};
            case DebugMarker.START -> new float[]{0.20f, 1.00f, 0.35f};
            case DebugMarker.FINISH -> new float[]{1.00f, 0.20f, 0.22f};
            default -> new float[]{0.15f, 0.85f, 1.00f};
        };
    }

    private static int labelColor(byte kind) {
        return switch (kind) {
            case DebugMarker.CHECKPOINT -> 0xFFFF8A14;
            case DebugMarker.START -> 0xFF33FF59;
            case DebugMarker.FINISH -> 0xFFFF3C3C;
            default -> 0xFF3DD6FF;
        };
    }

    private static double beamHeight(byte kind) {
        return switch (kind) {
            case DebugMarker.START, DebugMarker.FINISH -> 4.0;
            default -> 2.6;
        };
    }

    private static void renderLabel(PoseStack pose, Camera camera, String text, double x, double y, double z, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 cameraPos = camera.getPosition();
        pose.pushPose();
        pose.translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z);
        pose.mulPose(camera.rotation());
        pose.scale(-0.027f, -0.027f, 0.027f);
        Matrix4f matrix = pose.last().pose();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        float width = minecraft.font.width(text) / 2.0f;
        minecraft.font.drawInBatch(text, -width, 0, color, false, matrix, buffer,
                Font.DisplayMode.SEE_THROUGH, 0x66000000, LightTexture.FULL_BRIGHT);
        buffer.endBatch();
        pose.popPose();
    }
}
