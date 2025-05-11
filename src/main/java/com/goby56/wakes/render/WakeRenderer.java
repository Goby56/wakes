package com.goby56.wakes.render;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.config.enums.Resolution;
import com.goby56.wakes.simulation.Brick;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import com.goby56.wakes.debug.WakesDebugInfo;
import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;

public class WakeRenderer implements WorldRenderEvents.AfterTranslucent {
    public static Map<Resolution, WakeTexture> wakeTextures = null;

    private void initTextures() {
        wakeTextures = Map.of(
                Resolution.EIGHT, new WakeTexture(Resolution.EIGHT.res, true),
                Resolution.SIXTEEN, new WakeTexture(Resolution.SIXTEEN.res, true),
                Resolution.THIRTYTWO, new WakeTexture(Resolution.THIRTYTWO.res, true)
        );
    }

    @Override
    public void afterTranslucent(WorldRenderContext context) {
        if (WakesConfig.disableMod) {
            WakesDebugInfo.quadsRendered = 0;
            return;
        }

        context.gameRenderer().getLightmapTextureManager().enable();
        LightmapWrapper.updateTexture(context.gameRenderer().getLightmapTextureManager());
        if (wakeTextures == null) initTextures();

        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null || WakeHandler.resolutionResetScheduled) return;

        ArrayList<Brick> bricks = wakeHandler.getVisible(context.frustum(), Brick.class);

        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();

        Resolution resolution = WakeHandler.resolution;
        int n = 0;
        long tRendering = System.nanoTime();
        for (var brick : bricks) {
            render(matrix, context.camera(), brick, wakeTextures.get(resolution));
            n++;
        }
        WakesDebugInfo.renderingTime.add(System.nanoTime() - tRendering);
        WakesDebugInfo.quadsRendered = n;
    }

    private void render(Matrix4f matrix, Camera camera, Brick brick, WakeTexture texture) {
        if (!brick.hasPopulatedPixels) return;

        texture.loadTexture(brick.pixels);

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);

        Vector3f pos = brick.pos.add(camera.getPos().negate()).toVector3f().add(0, WakeNode.WATER_OFFSET, 0);
        bufferBuilder.vertex(matrix, pos.x, pos.y, pos.z)
                .texture(0, 0)
                .color(1f, 1f, 1f, 1f)
                .normal(0f, 1f, 0f);
        bufferBuilder.vertex(matrix, pos.x, pos.y, pos.z + brick.dim)
                .texture(0, 1)
                .color(1f, 1f, 1f, 1f)
                .normal(0f, 1f, 0f);
        bufferBuilder.vertex(matrix, pos.x + brick.dim, pos.y, pos.z + brick.dim)
                .texture(1, 1)
                .color(1f, 1f, 1f, 1f)
                .normal(0f, 1f, 0f);
        bufferBuilder.vertex(matrix, pos.x + brick.dim, pos.y, pos.z)
                .texture(1, 0)
                .color(1f, 1f, 1f, 1f)
                .normal(0f, 1f, 0f);

        BuiltBuffer builtBuffer = bufferBuilder.end();
        var buffer = RenderSystem.getDevice().createBuffer(() -> "Wakes wake quad buffer", BufferType.VERTICES, BufferUsage.DYNAMIC_WRITE, builtBuffer.getBuffer());

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(texture.texture, OptionalInt.of(0xffffffff))) {
            pass.setPipeline(RenderPipelines.TRANSLUCENT);
            pass.setVertexBuffer(0, buffer);
            pass.draw(0, buffer.size);
        }
    }
}
