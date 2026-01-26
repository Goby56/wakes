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
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.*;

public class WakeRenderer implements WorldRenderEvents.EndMain {

    private record PreparedDraw(GpuBuffer vbo, GpuBuffer ibo, int indexCount, VertexFormat.IndexType indexType, WakeTexture texture) {

    }

    @Override
    public void endMain(WorldRenderContext context) {
        // ===== Prepare =====
        if (WakesConfig.disableMod) {
            WakesDebugInfo.quadsRendered = 0;
            return;
        }

        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null || WakeHandler.resolutionResetScheduled) return;

        ArrayList<Brick> bricks = wakeHandler.getVisible(Brick.class);

        Vec3 cameraPos = context.worldState().cameraRenderState.pos;
        // PoseStack matrices = context.matrices();
        // matrices.pushPose();
        // matrices.translate(cameraPos.reverse());
        // Matrix4f matrix = matrices.last().pose();

        VertexFormat vertexFormat = RenderPipelines.TRANSLUCENT_MOVING_BLOCK.getVertexFormat();

        ArrayList<PreparedDraw> preparedDraws = new ArrayList<>();
        for (Brick brick : bricks) {
            if (!brick.hasPopulatedPixels) continue;

            MeshData mesh = createBrickMesh(cameraPos.toVector3f(), brick);
            GpuBuffer buffer = vertexFormat.uploadImmediateVertexBuffer(mesh.vertexBuffer());
            GpuBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).getBuffer(mesh.drawState().indexCount());
            //GpuBuffer indices = vertexFormat.uploadImmediateIndexBuffer(mesh.indexBuffer());

            int indexCount = mesh.drawState().indexCount();

            VertexFormat.IndexType indexType = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).type();
            //VertexFormat.IndexType indexType = mesh.drawState().indexType();

            brick.wakeTexture.loadTexture(brick.imgPtr, GlConst.GL_RGBA);

            preparedDraws.add(new PreparedDraw(buffer, indices, indexCount, indexType, brick.wakeTexture));

            mesh.close();
        }
        //matrices.popPose();

        // ===== Draw =====
        GpuBufferSlice dynamicUniforms = RenderSystem.getDynamicUniforms().writeTransform(
                RenderSystem.getModelViewMatrix(),
                new Vector4f(1,1,1,1),
                new Vector3f(),
                new Matrix4f()
        );
        GpuSampler sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Wake", Minecraft.getInstance().getMainRenderTarget().getColorTextureView(),
                OptionalInt.empty(),
                Minecraft.getInstance().getMainRenderTarget().getDepthTextureView(),
                OptionalDouble.empty()))
        {
            pass.setPipeline(RenderPipelines.TRANSLUCENT_MOVING_BLOCK);
            pass.bindTexture("Sampler2", Minecraft.getInstance().gameRenderer.lightTexture().getTextureView(), sampler);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicUniforms);

            int n = 0;
            long tRendering = System.nanoTime();
            for (PreparedDraw draw : preparedDraws) {
                pass.bindTexture("Sampler0", draw.texture.getTextureView(), sampler);

                pass.setVertexBuffer(0, draw.vbo);
                pass.setIndexBuffer(draw.ibo, draw.indexType);
                pass.drawIndexed(0, 0, draw.indexCount, 1);

                n++;
            }
            WakesDebugInfo.renderingTime.add(System.nanoTime() - tRendering);
            WakesDebugInfo.quadsRendered = n;
        }
    }

    private MeshData createBrickMesh(Vector3f cameraPos, Brick brick) {
        Vector3f pos = new Vector3f(
                (float) (brick.pos.x - cameraPos.x),
                (float) (brick.pos.y - cameraPos.y + WakeNode.WATER_OFFSET),
                (float) (brick.pos.z - cameraPos.z));
        Matrix4f matrix = new Matrix4f();

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
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

        return bb.buildOrThrow();
    }
}
