package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.simulation.WakeChunk;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import com.goby56.wakes.debug.WakesDebugInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

import java.util.*;

public class WakeRenderer implements LevelRenderEvents.CollectSubmits {

    private static RenderType renderType() {
        return WakesClient.WAKE_RENDER_TYPE;
    }

    @Override
    public void collectSubmits(LevelRenderContext context) {
        submit(context);
    }

    private void submit(LevelRenderContext context) {
        if (WakesConfig.disableMod) {
            return;
        }

        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null) {
            return;
        }

        List<WakeChunk> wakeChunks = wakeHandler.getVisibleChunks();
        if (wakeChunks.isEmpty()) {
            return;
        }

        long tRendering = System.nanoTime();
        try {
            float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            wakeHandler.refreshInterpolatedOcclusion(partialTick);
            wakeHandler.getTextureAtlas().dynamicTexture.uploadIfDirty();

            Vec3 camera = context.levelState().cameraRenderState.pos;

            PoseStack matrices = context.poseStack();
            matrices.pushPose();
            matrices.translate(-camera.x, -camera.y, -camera.z);
            Matrix4f matrixCopy = new Matrix4f(matrices.last().pose());
            matrices.popPose();

            float extraOffset = WakesClient.areShadersEnabled ? WakesConfig.shaderWaterHeightOffset : 0f;
            ClientLevel world = Minecraft.getInstance().level;
            boolean submerged = world.getFluidState(BlockPos.containing(camera.x, camera.y, camera.z)).is(FluidTags.WATER);
            float surfaceBias = (submerged ? -SURFACE_EPSILON : SURFACE_EPSILON) + extraOffset;
            RenderType type = renderType();

            var renderer = (net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer) (pose, vc) -> {
                for (WakeChunk wakeChunk : wakeChunks) {
                    for (WakeNode wakeNode : wakeChunk.getNodes()) {
                        UVPair uv = wakeNode.drawContext.getUV();
                        float uvOffset = wakeNode.drawContext.getUVOffset();
                        int light = LightCoordsUtil.getLightCoords(world, wakeNode.blockPos());
                        float x0 = (float) wakeNode.x;
                        float y  = (float) (wakeNode.y + WakeNode.WATER_OFFSET) + surfaceBias;
                        float z0 = (float) wakeNode.z;
                        float u0 = uv.u(), v0 = uv.v(), u1 = u0 + uvOffset, v1 = v0 + uvOffset;
                        emitQuad(vc, matrixCopy, x0, z0, x0 + 1, z0 + 1, y, u0, v0, u1, v1, light);
                    }
                }
            };

            context.submitNodeCollector().submitCustomGeometry(context.poseStack(), WakesClient.WAKE_COLOR_RENDER_TYPE, renderer);
            context.submitNodeCollector().submitCustomGeometry(context.poseStack(), type, renderer);
        } catch (Throwable t) {
            WakesClient.LOGGER.error("WakeRenderer: EXCEPTION during render", t);
        }

        WakesDebugInfo.addRenderTime(System.nanoTime() - tRendering);
        WakesDebugInfo.visibleNodes = wakeChunks.stream().mapToInt(c -> c.occupied).sum();
    }

    private static final float SURFACE_EPSILON = 0.01f;

    /** Emits a single horizontal quad spanning [x0,z0]..[x1,z1].
     *  entityTranslucent uses NO_CULL so this single quad is visible from both sides. */
    private void emitQuad(VertexConsumer vc, Matrix4fc m,
                          float x0, float z0, float x1, float z1, float y,
                          float u0, float v0, float u1, float v1, int light) {
        vert(vc, m, x0, y, z0, u0, v0, light);
        vert(vc, m, x0, y, z1, u0, v1, light);
        vert(vc, m, x1, y, z1, u1, v1, light);
        vert(vc, m, x1, y, z0, u1, v0, light);
    }

    private void vert(VertexConsumer vc, Matrix4fc m, float x, float y, float z, float u, float v, int light) {
        vc.addVertex(m, x, y, z)
                .setColor(1f, 1f, 1f, WakesConfig.wakeOpacity)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0f, 1f, 0f);
    }

    public void close() {
    }
}
