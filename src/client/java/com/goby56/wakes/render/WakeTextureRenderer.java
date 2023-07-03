package com.goby56.wakes.render;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.utils.WakeColor;
import com.goby56.wakes.utils.WakeHandler;
import com.goby56.wakes.utils.WakeNode;
import com.goby56.wakes.utils.WakesUtils;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.isxander.yacl3.api.NameableEnum;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.*;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;

public class WakeTextureRenderer implements WorldRenderEvents.AfterTranslucent {

    public static float averageClampedValue = 0;
    @Override
    public void afterTranslucent(WorldRenderContext context) {
        WakeHandler wakeHandler = WakeHandler.getInstance();
        ArrayList<WakeNode> nodes = WakeHandler.getInstance().getVisible(context.frustum());
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        RenderSystem.enableBlend();
//        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR);

        if (wakeHandler.glFoamTexId == -1) {
            wakeHandler.glFoamTexId = initTexture();
        }
        if (wakeHandler.glWakeTexId == -1) {
            wakeHandler.glWakeTexId = initTexture();
        }
        if (wakeHandler.foamImgPtr == -1) {
            wakeHandler.foamImgPtr = MemoryUtil.nmemAlloc(16 * 16 * 4);
        }
        if (wakeHandler.wakeImgPtr == -1) {
            wakeHandler.wakeImgPtr = MemoryUtil.nmemAlloc(16 * 16 * 4);
        }

        float avg;
        int waterCol;
        WakeColor wakeCol;
        float a, r, g, b;
        float x, y, z;
        for (WakeNode node : nodes) {
            Vec3d pos = node.getPos().add(context.camera().getPos().negate());
            x = (float) pos.x;
            y = (float) pos.y;
            z = (float) pos.z;
            a = WakesConfig.useAgeDecay ? (float) Math.pow(2, -node.t) : 1f;

            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    avg = (node.u[0][i+1][j+1] + node.u[1][i+1][j+1] + node.u[2][i+1][j+1]) / 3;
                    wakeCol = WakeColor.getColor(avg);
                    if (wakeCol == WakeColor.WHITE) {
                        MemoryUtil.memPutInt(wakeHandler.foamImgPtr + (i*16+j)*4, wakeCol.argb);
                        MemoryUtil.memPutInt(wakeHandler.wakeImgPtr + (i*16+j)*4, 0);
                    } else {
                        MemoryUtil.memPutInt(wakeHandler.foamImgPtr + (i*16+j)*4, 0);
                        MemoryUtil.memPutInt(wakeHandler.wakeImgPtr + (i*16+j)*4, wakeCol.argb);
                    }
                }
            }


            waterCol = BiomeColors.getWaterColor(context.world(), new BlockPos(node.x, (int) node.height, node.z));
            r = (float) (waterCol >> 16 & 0xFF) / 255f;
            g = (float) (waterCol >> 8 & 0xFF) / 255f;
            b = (float) (waterCol & 0xFF) / 255f;
            if (WakesConfig.useWaterBlending) {
                renderTexture(wakeHandler.glWakeTexId, wakeHandler.wakeImgPtr, matrix, x, y, z, x + 1, y, z + 1, r, g, b, a);
            } else {
                renderTexture(wakeHandler.glWakeTexId, wakeHandler.wakeImgPtr, matrix, x, y, z, x + 1, y, z + 1, 1f, 1f, 1f, a);
            }
            renderTexture(wakeHandler.glFoamTexId, wakeHandler.foamImgPtr, matrix, x, y, z, x + 1, y, z + 1, 1f, 1f, 1f, a);
        }
        RenderSystem.defaultBlendFunc();
    }

    private static void renderTexture(int textureID, long texture, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, float r, float g, float b, float a) {
//        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        GlStateManager._bindTexture(textureID);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ALIGNMENT, 4);
        GlStateManager._texSubImage2D(GlConst.GL_TEXTURE_2D, 0, 0, 0, 16, 16, GlConst.GL_RGBA, GlConst.GL_UNSIGNED_BYTE, texture);

        RenderSystem.setShaderTexture(0, textureID);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.enableDepthTest();
        // TODO SWITCH TO STANDARD RENDER LAYERS (DIRECT DRAW CALLS MAY BE SLOW)
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, x0, y0, z0).texture(0, 0).color(r, g, b, a).next();
        buffer.vertex(matrix, x0, (y0+y1)/2, z1).texture(0, 1).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y1, z1).texture(1, 1).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, (y0+y1)/2, z0).texture(1, 0).color(r, g, b, a).next();
        Tessellator.getInstance().draw();
        // TODO DRAW TEXTURE ON UNDER SIDE MAYBE
    }

    private static int initTexture() {
        int texId = TextureUtil.generateTextureId();
        GlStateManager._bindTexture(texId);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0f);

        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_FILTER, GL12.GL_NEAREST);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAG_FILTER, GL12.GL_NEAREST);

        GlStateManager._texImage2D(GlConst.GL_TEXTURE_2D, 0, GlConst.GL_RGBA, 16, 16, 0, GlConst.GL_RGBA, GlConst.GL_UNSIGNED_BYTE, null);
        return texId;
    }
}
