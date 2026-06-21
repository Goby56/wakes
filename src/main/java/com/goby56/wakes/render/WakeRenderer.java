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
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

import java.util.*;

public class WakeRenderer implements LevelRenderEvents.AfterTranslucentTerrain, LevelRenderEvents.BeforeTranslucentTerrain {

    private static RenderType renderType() {
        return RenderTypes.entityTranslucent(WakeTextureAtlas.ATLAS_ID, false);
    }

    private static boolean cameraSubmerged(LevelRenderContext context) {
        return context.gameRenderer().getMainCamera().getFluidInCamera()
                == net.minecraft.world.level.material.FogType.WATER;
    }

    @Override
    public void afterTranslucentTerrain(LevelRenderContext context) {
        if (cameraSubmerged(context)) return;
        render(context);
    }

    @Override
    public void beforeTranslucentTerrain(LevelRenderContext context) {
        if (!cameraSubmerged(context)) return;
        render(context);
    }

    private void render(LevelRenderContext context) {
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
            // Make sure the GPU texture has the latest simulation pixels
            wakeHandler.getTextureAtlas().dynamicTexture.uploadIfDirty();

            Vec3 camera = context.levelState().cameraRenderState.pos;

            PoseStack matrices = context.poseStack();
            matrices.pushPose();
            matrices.translate(-camera.x, -camera.y, -camera.z);
            Matrix4fc matrix = matrices.last().pose();

            MultiBufferSource.BufferSource bufferSource = context.bufferSource();
            RenderType type = renderType();
            VertexConsumer vc = bufferSource.getBuffer(type);

            addVertices(matrix, vc, wakeChunks);

            matrices.popPose();

            // Flush immediately so it draws into the currently-bound (Iris) framebuffer
            bufferSource.endBatch(type);
        } catch (Throwable t) {
            WakesClient.LOGGER.error("WakeRenderer: EXCEPTION during render", t);
        }

        WakesDebugInfo.addRenderTime(System.nanoTime() - tRendering);
        WakesDebugInfo.visibleNodes = wakeChunks.stream().mapToInt(c -> c.occupied).sum();
    }

    private static final float SURFACE_EPSILON = 0.01f;

    private void addVertices(Matrix4fc matrix, VertexConsumer vc, List<WakeChunk> chunks) {
        // Water-displacing shaders write a wavy water surface into the depth buffer, which can
        // occlude (clip) the flat wake plane. Lift the wake by a configurable amount when
        // shaders are active so it stays above the displaced surface.
        float extraOffset = WakesClient.areShadersEnabled ? WakesConfig.shaderWaterHeightOffset : 0f;
        float surfaceBias = SURFACE_EPSILON + extraOffset;
        ClientLevel world = Minecraft.getInstance().level;
        for (WakeChunk wakeChunk : chunks) {
            for (WakeNode wakeNode : wakeChunk.getNodes()) {
                UVPair uv = wakeNode.drawContext.getUV();
                float uvOffset = wakeNode.drawContext.getUVOffset();
                int light = LevelRenderer.getLightCoords(world, wakeNode.blockPos());

                float x0 = (float) wakeNode.x;
                float y = (float) (wakeNode.y + WakeNode.WATER_OFFSET) + surfaceBias;
                float z0 = (float) wakeNode.z;

                emitQuad(vc, matrix,
                        x0, z0,
                        x0 + 1, z0 + 1,
                        y,
                        uv.u(), uv.v(),
                        uv.u() + uvOffset,  uv.v() + uvOffset,
                        light
                );
            }
        }
    }

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
                .setColor(1f, 1f, 1f, 1f) // white: let the atlas texture provide the wake color/alpha
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0f, 1f, 0f);
    }

    public void close() {
    }
}
