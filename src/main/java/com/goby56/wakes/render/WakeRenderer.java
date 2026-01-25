package com.goby56.wakes.render;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.config.enums.Resolution;
import com.goby56.wakes.simulation.Brick;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import com.goby56.wakes.debug.WakesDebugInfo;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.*;

public class WakeRenderer implements WorldRenderEvents.EndMain {
    public static Map<Resolution, WakeTexture> wakeTextures = null;

    private void initTextures() {
        wakeTextures = Map.of(
                Resolution.EIGHT, new WakeTexture(Resolution.EIGHT.res, true),
                Resolution.SIXTEEN, new WakeTexture(Resolution.SIXTEEN.res, true),
                Resolution.THIRTYTWO, new WakeTexture(Resolution.THIRTYTWO.res, true)
        );
    }

    @Override
    public void endMain(WorldRenderContext context) {
        if (WakesConfig.disableMod) {
            WakesDebugInfo.quadsRendered = 0;
            return;
        }

        // context.gameRenderer().lightTexture().turnOnLightLayer();
        if (wakeTextures == null) initTextures();

        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null || WakeHandler.resolutionResetScheduled) return;
        ArrayList<Brick> bricks = wakeHandler.getVisible(Brick.class);
        Vec3 cameraPos = context.worldState().cameraRenderState.pos;
        PoseStack matrices = context.matrices();
        matrices.pushPose();
        matrices.translate(cameraPos.reverse());

        Matrix4f matrix = matrices.last().pose();
        // Matrix4f matrix = new Matrix4f();

        GpuBufferSlice dynamicUniforms = RenderSystem.getDynamicUniforms().writeTransform(
                RenderSystem.getModelViewMatrix(),
                new Vector4f(1,1,1,1),
                new Vector3f(),
                new Matrix4f()
        );

        Resolution resolution = WakeHandler.resolution;
        int n = 0;
        long tRendering = System.nanoTime();
        for (var brick : bricks) {
            render(matrix, brick, wakeTextures.get(resolution), dynamicUniforms);
            n++;
        }
        WakesDebugInfo.renderingTime.add(System.nanoTime() - tRendering);
        WakesDebugInfo.quadsRendered = n;

        matrices.popPose();
    }

    private void render(Matrix4f matrix, Brick brick, WakeTexture texture, GpuBufferSlice dynamicUniforms) {
        if (!brick.hasPopulatedPixels) return;
        texture.loadTexture(brick.imgPtr, GlConst.GL_RGBA);

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        Vector3f pos = brick.pos.toVector3f().add(0, WakeNode.WATER_OFFSET, 0);
        bb.addVertex(matrix, pos.x, pos.y, pos.z)
                .setUv(0, 0)
                .setColor(1f, 1f, 1f, 1f)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0f, 1f, 0f);
        bb.addVertex(matrix, pos.x, pos.y, pos.z + brick.dim)
                .setUv(0, 1)
                .setColor(1f, 1f, 1f, 1f)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0f, 1f, 0f);
        bb.addVertex(matrix, pos.x + brick.dim, pos.y, pos.z + brick.dim)
                .setUv(1, 1)
                .setColor(1f, 1f, 1f, 1f)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0f, 1f, 0f);
        bb.addVertex(matrix, pos.x + brick.dim, pos.y, pos.z)
                .setUv(1, 0)
                .setColor(1f, 1f, 1f, 1f)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0f, 1f, 0f);

        MeshData built = bb.buildOrThrow();

        GpuBuffer buffer = DefaultVertexFormat.BLOCK.uploadImmediateVertexBuffer(built.vertexBuffer());
        GpuBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).getBuffer(built.drawState().indexCount());
        GpuSampler sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Wake", Minecraft.getInstance().getMainRenderTarget().getColorTextureView(), OptionalInt.empty(), Minecraft.getInstance().getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(RenderPipelines.TRANSLUCENT_MOVING_BLOCK);
            pass.bindTexture("Sampler0", texture.getTextureView(), sampler);
            pass.bindTexture("Sampler2", Minecraft.getInstance().gameRenderer.lightTexture().getTextureView(), sampler);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicUniforms);

            pass.setVertexBuffer(0, buffer);
            pass.setIndexBuffer(indices, RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).type());
            pass.drawIndexed(0, 0, built.drawState().indexCount(), 1);
        }
        built.close();
    }
}
