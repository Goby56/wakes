package com.goby56.wakes.render;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.render.*;
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
        lightmapTextureManager.lightmapFramebuffer.beginRead();
        GlStateManager._readPixels(0, 0, 16, 16, GlConst.GL_BGR, GlConst.GL_UNSIGNED_BYTE, imgPtr);
        lightmapTextureManager.lightmapFramebuffer.endRead();
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

            buffer.vertex(matrix, 0, 0, 0)
                    .texture(0, 0)
                    .color(1f, 1f, 1f, 1f);
            buffer.vertex(matrix, 100, 0, 0)
                    .texture(0, 1)
                    .color(1f, 1f, 1f, 1f);
            buffer.vertex(matrix, 100, 100, 0)
                    .texture(1, 1)
                    .color(1f, 1f, 1f, 1f);
            buffer.vertex(matrix, 0, 100, 0)
                    .texture(1, 0)
                    .color(1f, 1f, 1f, 1f);

            BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
}
