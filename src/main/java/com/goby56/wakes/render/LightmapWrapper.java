package com.goby56.wakes.render;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

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

    public static int readPixel(int block, int sky) {
        if (texture == null) {
            return 0;
        }
        return texture.getPixel(block, sky);
    }

    public static void render(Matrix4f matrix) {
        texture.loadTexture(imgPtr, GlConst.GL_BGR);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        int middleX = MinecraftClient.getInstance().getWindow().getScaledWidth() / 2;
        buffer.vertex(matrix, middleX - 50, 0, 0)
                .texture(0, 0)
                .color(1f, 1f, 1f, 1f);
        buffer.vertex(matrix,  middleX + 50, 0, 0)
                .texture(0, 1)
                .color(1f, 1f, 1f, 1f);
        buffer.vertex(matrix, middleX + 50, 100, 0)
                .texture(1, 1)
                .color(1f, 1f, 1f, 1f);
        buffer.vertex(matrix, middleX - 50, 100, 0)
                .texture(1, 0)
                .color(1f, 1f, 1f, 1f);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
}
