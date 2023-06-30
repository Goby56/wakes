package com.goby56.wakes.render;

import com.goby56.wakes.utils.WakeHandler;
import com.goby56.wakes.utils.WakeNode;
import com.goby56.wakes.utils.WakesUtils;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;

public class WakeTextureRenderer implements WorldRenderEvents.AfterTranslucent {

    public enum WakeColor {
        TRANSPARENT(0, 0, 0, 0, 0, 125),
        DARK_GRAY(147, 153, 166, 210, 125, 175),
        GRAY(158, 165, 176, 210, 175, 215),
        LIGHT_GRAY(196, 202, 209, 210, 215, 230),
        WHITE(255, 255, 255, 255, 230, 255);

        public final int argb;
        private final int from;
        private final int to;

        WakeColor(int red, int green, int blue, int alpha, int from, int to) {
            this.from = from;
            this.to = to;
            this.argb = ColorHelper.Argb.getArgb(alpha, blue, green, red); // abgr actually because big-endian?
        }

        public static int getColor(float avg) {
            double clampedRange = 255 * (1 - 1 / (0.1 * Math.abs(avg) + 1));
            for (WakeColor color : WakeColor.values()) {
                if (color.from <= clampedRange && clampedRange <= color.to) {
                    return color.argb;
                }
            }
            return WHITE.argb;
        }
    }

    @Override
    public void afterTranslucent(WorldRenderContext context) {
        WakeHandler wakeHandler = WakeHandler.getInstance();
        ArrayList<WakeNode> nodes = WakeHandler.getInstance().getVisible(context.frustum());
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        RenderSystem.enableBlend();
//        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

        if (wakeHandler.glTexId == -1) {
            wakeHandler.glTexId = TextureUtil.generateTextureId();

            GlStateManager._bindTexture(wakeHandler.glTexId);
            GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
            GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0);
            GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, 0);
            GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0f);

            GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_FILTER, GL12.GL_NEAREST);
            GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAG_FILTER, GL12.GL_NEAREST);

            GlStateManager._texImage2D(GlConst.GL_TEXTURE_2D, 0, GlConst.GL_RGBA, 16, 16, 0, GlConst.GL_RGBA, GlConst.GL_UNSIGNED_BYTE, null);
        }

        if (wakeHandler.imagePointer == -1) {
            wakeHandler.imagePointer = MemoryUtil.nmemAlloc(16 * 16 * 4);
        }

        float avg;
        int col;
        float a;
        float r;
        float g;
        float b;
        for (WakeNode node : nodes) {
            Vec3d pos = node.getPos().add(context.camera().getPos().negate());

            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    avg = (node.u[0][z+1][x+1] + node.u[1][z+1][x+1] + node.u[2][z+1][x+1]) / 3;
                    MemoryUtil.memPutInt(wakeHandler.imagePointer + (z*16+x)*4, WakeColor.getColor(avg));
                }
            }

            GlStateManager._bindTexture(wakeHandler.glTexId);
            GlStateManager._pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, 0);
            GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
            GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
            GlStateManager._pixelStore(GlConst.GL_UNPACK_ALIGNMENT, 4);
            GlStateManager._texSubImage2D(GlConst.GL_TEXTURE_2D, 0, 0, 0, 16, 16, GlConst.GL_RGBA, GlConst.GL_UNSIGNED_BYTE, wakeHandler.imagePointer);

            RenderSystem.setShaderTexture(0, wakeHandler.glTexId);

            col = BiomeColors.getWaterColor(context.world(), new BlockPos(node.x, (int) node.height, node.z));
            r = (float) (col >> 16 & 0xFF) / 255f;
            g = (float) (col >> 8 & 0xFF) / 255f;
            b = (float) (col & 0xFF) / 255f;
            // TODO FIX WATER COLOR BLENDING
            renderTexture(matrix, (float) pos.x, (float) pos.y, (float) pos.z, (float) (pos.x + 1), (float) pos.y, (float) (pos.z + 1), 1, 1, 1, 0.9f);
        }
        RenderSystem.defaultBlendFunc();
    }

    private static void renderTexture(Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, float r, float g, float b, float a) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, x0, y0, z0).texture(0, 0).color(r, g, b, a).next();
        buffer.vertex(matrix, x0, (y0+y1)/2, z1).texture(0, 1).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y1, z1).texture(1, 1).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, (y0+y1)/2, z0).texture(1, 0).color(r, g, b, a).next();
        Tessellator.getInstance().draw();
        // TODO DRAW TEXTURE ON UNDER SIDE MAYBE
    }
}
