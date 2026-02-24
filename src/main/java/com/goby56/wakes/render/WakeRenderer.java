package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.simulation.WakeChunk;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import com.goby56.wakes.debug.WakesDebugInfo;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.*;

public class WakeRenderer implements WorldRenderEvents.EndMain {

    private static final ByteBufferBuilder allocator = new ByteBufferBuilder(RenderType.BIG_BUFFER_SIZE);
    private BufferBuilder buffer;

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private MappableRingBuffer vertexBuffer;

    private final RenderPipeline PIPELINE = RenderPipelines.TRANSLUCENT_MOVING_BLOCK;

    @Override
    public void endMain(WorldRenderContext context) {
        if (WakesConfig.disableMod) {
            WakesDebugInfo.quadsRendered = 0;
            return;
        }

        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null) return;

        List<WakeChunk> wakeChunks = wakeHandler.getVisibleChunks();

        if (wakeChunks.isEmpty()) return;

        long tRendering = System.nanoTime();

        if (buffer == null) {
            buffer = new BufferBuilder(allocator, PIPELINE.getVertexFormatMode(), PIPELINE.getVertexFormat());
        }


        PoseStack matrices = context.matrices();
        Vec3 camera = context.worldState().cameraRenderState.pos;

        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        addVertices(matrices.last().pose(), buffer, wakeChunks);

        matrices.popPose();

        MeshData builtBuffer = buffer.buildOrThrow();
        MeshData.DrawState drawParameters = builtBuffer.drawState();
        VertexFormat format = drawParameters.format();

        GpuBuffer vertices = uploadMesh(drawParameters, format, builtBuffer);

        BetterDynamicTexture texture = wakeHandler.getTextureAtlas().dynamicTexture;
        texture.uploadIfDirty();
        draw(builtBuffer, drawParameters, vertices, format, texture.getTextureView());

        // Rotate the vertex buffer so we are less likely to use buffers that the GPU is using
        vertexBuffer.rotate();
        buffer = null;

        WakesDebugInfo.renderingTime.add(System.nanoTime() - tRendering);
        WakesDebugInfo.quadsRendered = wakeChunks.size();
    }

    private void addVertices(Matrix4fc matrix, BufferBuilder bb, List<WakeChunk> chunks) {
        for (WakeChunk wakeChunk : chunks) {
            UVPair uv = wakeChunk.drawContext.getUV();
            float uvOffset = wakeChunk.drawContext.getUVOffset();

            float x0 = (float) wakeChunk.pos.x;
            float y = (float) (wakeChunk.pos.y + WakeNode.WATER_OFFSET);
            float z0 = (float) wakeChunk.pos.z;

            float x1 = x0 + WakeChunk.WIDTH;
            float z1 = z0 + WakeChunk.WIDTH;

            bb.addVertex(matrix, x0, y, z0)
                    .setUv(uv.u(), uv.v())
                    .setColor(1f, 1f, 1f, 1f)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(0f, 1f, 0f);
            bb.addVertex(matrix, x0, y, z1)
                    .setUv(uv.u(), uv.v() + uvOffset)
                    .setColor(1f, 1f, 1f, 1f)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(0f, 1f, 0f);
            bb.addVertex(matrix, x1, y, z1)
                    .setUv(uv.u() + uvOffset, uv.v() + uvOffset)
                    .setColor(1f, 1f, 1f, 1f)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(0f, 1f, 0f);
            bb.addVertex(matrix, x1, y, z0)
                    .setUv(uv.u() + uvOffset, uv.v())
                    .setColor(1f, 1f, 1f, 1f)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(0f, 1f, 0f);
        }
    }

    private GpuBuffer uploadMesh(MeshData.DrawState drawParameters, VertexFormat format, MeshData builtBuffer) {
        // Calculate the size needed for the vertex buffer
        int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();

        // Initialize or resize the vertex buffer as needed
        if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
            if (vertexBuffer != null) {
                vertexBuffer.close();
            }

            vertexBuffer = new MappableRingBuffer(() -> WakesClient.MOD_ID + " wake ring buffer", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, vertexBufferSize);
        }

        // Copy vertex data into the vertex buffer
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(vertexBuffer.currentBuffer().slice(0, builtBuffer.vertexBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(builtBuffer.vertexBuffer(), mappedView.data());
        }

        return vertexBuffer.currentBuffer();
    }

    private void draw(MeshData builtBuffer, MeshData.DrawState drawParameters, GpuBuffer vertices, VertexFormat format, GpuTextureView texture) {
        GpuBuffer indices;
        VertexFormat.IndexType indexType;

        if (PIPELINE.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
            // Sort the quads if there is translucency
            builtBuffer.sortQuads(allocator, RenderSystem.getProjectionType().vertexSorting());
            // Upload the index buffer
            indices = PIPELINE.getVertexFormat().uploadImmediateIndexBuffer(builtBuffer.indexBuffer());
            indexType = builtBuffer.drawState().indexType();
        } else {
            // Use the general shape index buffer for non-quad draw modes
            RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(PIPELINE.getVertexFormatMode());
            indices = shapeIndexBuffer.getBuffer(drawParameters.indexCount());
            indexType = shapeIndexBuffer.type();
        }

        // Actually execute the draw
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        GpuSampler sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
        Minecraft client = Minecraft.getInstance();
        try (RenderPass pass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> WakesClient.MOD_ID + " wake render pipeline rendering",
                        client.getMainRenderTarget().getColorTextureView(),
                        OptionalInt.empty(),
                        client.getMainRenderTarget().getDepthTextureView(),
                        OptionalDouble.empty())) {

            pass.setPipeline(PIPELINE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicTransforms);

            pass.bindTexture("Sampler0", texture, sampler);
            pass.bindTexture("Sampler2", Minecraft.getInstance().gameRenderer.lightTexture().getTextureView(), sampler);

            pass.setVertexBuffer(0, vertices);
            pass.setIndexBuffer(indices, indexType);

            // The base vertex is the starting index when we copied the data into the vertex buffer divided by vertex size
            //noinspection ConstantValue
            pass.drawIndexed(0 / format.getVertexSize(), 0, drawParameters.indexCount(), 1);

        }

        builtBuffer.close();
    }

    public void close() {
        allocator.close();

        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}
