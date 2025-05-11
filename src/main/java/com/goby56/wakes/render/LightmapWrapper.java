package com.goby56.wakes.render;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;

import java.util.OptionalInt;

public class LightmapWrapper {
    public static WritableTexture texture;

    public static void initTexture() {
        texture = new WritableTexture(16, 16, "Lightmap wrapper texture");
    }

    public static void updateTexture(LightmapTextureManager lightmapTextureManager) {
        if (texture == null) {
            initTexture();
        }
        texture.readFromTexture(lightmapTextureManager.getGlTexture());
    }

    public static int getLightColor(int block, int sky) {
        if (texture == null) {
            return 0;
        }
        return texture.getPixel(block, sky);
    }

    public static void render(Matrix4f matrix) {
        RenderSystem.getDevice().createCommandEncoder().presentTexture(texture.texture);
        // BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        // int middleX = MinecraftClient.getInstance().getWindow().getScaledWidth() / 2;

        // bufferBuilder.vertex(matrix, middleX - 50, 0, 0).texture(0, 0).color(0xffffffff);
        // bufferBuilder.vertex(matrix, middleX + 50, 0, 0).texture(0, 1).color(0xffffffff);
        // bufferBuilder.vertex(matrix, middleX + 50, 100, 0).texture(1, 1).color(0xffffffff);
        // bufferBuilder.vertex(matrix, middleX - 50, 100, 0).texture(1, 0).color(0xffffffff);

        // BuiltBuffer builtBuffer = bufferBuilder.end();
        // var buffer = RenderSystem.getDevice().createBuffer(() -> "Wakes lightmap quad buffer", BufferType.VERTICES, BufferUsage.STATIC_READ, builtBuffer.getBuffer());
        // try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(texture.texture, OptionalInt.of(0xffffffff))) {
        //     pass.setPipeline(RenderPipelines.GUI_TEXTURED);
        //     pass.setVertexBuffer(0, buffer);
        //     pass.draw(0, buffer.size);
        // }
    }
}
