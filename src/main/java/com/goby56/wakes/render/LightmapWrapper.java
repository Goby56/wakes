package com.goby56.wakes.render;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public class LightmapWrapper {
    public static long imgPtr = -1;
    public static WakeTexture texture;

    public static void initTexture() {
        imgPtr = MemoryUtil.nmemAlloc(16 * 16 * 3);
        texture = new WakeTexture(16, false);
    }

    public static void updateTexture(LightmapTextureManager lightmapTextureManager) {
        if (imgPtr == -1) {
            initTexture();
        }

        RenderSystem.bindTexture(lightmapTextureManager.lightmapFramebuffer.getColorAttachment());
        GlStateManager._getTexImage(GlConst.GL_TEXTURE_2D, 0, GlConst.GL_BGR, GlConst.GL_UNSIGNED_BYTE, imgPtr);
    }

    public static int readPixel(int block, int sky) {
        if (imgPtr == -1) {
            return 0;
        }
        int index = (block + sky * 16) * 3;

        return MemoryUtil.memGetInt(imgPtr + index);
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
