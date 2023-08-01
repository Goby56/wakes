package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.utils.WakeColor;
import com.goby56.wakes.utils.WakeHandler;
import com.goby56.wakes.utils.WakeNode;
import com.goby56.wakes.utils.WakesUtils;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;

public class WakeTextureRenderer implements WorldRenderEvents.AfterTranslucent {

    @Override
    public void afterTranslucent(WorldRenderContext context) {
        if (!WakesClient.CONFIG_INSTANCE.renderWakes) {
            return;
        }
        WakeHandler wakeHandler = WakeHandler.getInstance();
        if (wakeHandler == null || wakeHandler.resetScheduled) return;

        ArrayList<WakeNode> nodes = wakeHandler.getVisible(context.frustum());
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        context.lightmapTextureManager().enable();

        BlendingFunction blendMode = WakesClient.CONFIG_INSTANCE.blendMode;
        if (blendMode == BlendingFunction.DEFAULT) {
            RenderSystem.defaultBlendFunc();
        } else {
            RenderSystem.blendFunc(blendMode.srcFactor, blendMode.dstFactor);
        }

        int res = wakeHandler.resolution.res;
        if (wakeHandler.glFoamTexId == -1) {
            wakeHandler.glFoamTexId = initTexture(res);
        }
        if (wakeHandler.glWakeTexId == -1) {
            wakeHandler.glWakeTexId = initTexture(res);
        }
        if (wakeHandler.foamImgPtr == -1) {
            wakeHandler.foamImgPtr = MemoryUtil.nmemAlloc((long) res * res * 4);
        }
        if (wakeHandler.wakeImgPtr == -1) {
            wakeHandler.wakeImgPtr = MemoryUtil.nmemAlloc((long) res * res * 4);
        }

        float avg;
        int waterCol;
        WakeColor wakeCol;
        float r, g, b, a;
        float x, y, z;
        int light;
        for (WakeNode node : nodes) {
            if (node.isDead()) continue;
            if (res != node.res) continue;
            Vec3d screenSpace = node.getPos().add(context.camera().getPos().negate());
            x = (float) screenSpace.x;
            y = (float) screenSpace.y;
            z = (float) screenSpace.z;

            for (int i = 0; i < res; i++) {
                for (int j = 0; j < res; j++) {
                    avg = (node.u[0][i+1][j+1] + node.u[1][i+1][j+1] + node.u[2][i+1][j+1]) / 3;
                    wakeCol = WakeColor.getColor(avg);
                    if (wakeCol == WakeColor.WHITE) {
                        MemoryUtil.memPutInt(wakeHandler.foamImgPtr + (((i*(long)res)+j)*4), wakeCol.argb);
                        MemoryUtil.memPutInt(wakeHandler.wakeImgPtr + (((i*(long)res)+j)*4), 0);
                    } else {
                        MemoryUtil.memPutInt(wakeHandler.foamImgPtr + (i*((long)res)+j)*4, 0);
                        MemoryUtil.memPutInt(wakeHandler.wakeImgPtr + (i*((long)res)+j)*4, wakeCol.argb);
                    }
                }
            }

            waterCol = BiomeColors.getWaterColor(context.world(), node.blockPos());
            // TODO LAMB DYNAMIC LIGHTS COMPAT
            light = WorldRenderer.getLightmapCoordinates(context.world(), node.blockPos());

            if (WakesClient.CONFIG_INSTANCE.useWaterBlending) {
                r = (float) (waterCol >> 16 & 0xFF) / 255f;
                g = (float) (waterCol >> 8 & 0xFF) / 255f;
                b = (float) (waterCol & 0xFF) / 255f;
            } else {
                r = 1f;
                g = 1f;
                b = 1f;
            }
            a = (float) (-Math.pow(node.t, 2) + 1) * WakesClient.CONFIG_INSTANCE.wakeOpacity;

            renderTexture(res, wakeHandler.glWakeTexId, wakeHandler.wakeImgPtr, matrix, x, y, z, x + 1, y, z + 1, r, g, b, a, light);
            renderTexture(res, wakeHandler.glFoamTexId, wakeHandler.foamImgPtr, matrix, x, y, z, x + 1, y, z + 1, 1f, 1f, 1f, a, light);
        }
        RenderSystem.defaultBlendFunc();
    }

    private static void renderTexture(int resolution, int textureID, long texture, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, float r, float g, float b, float a, int light) {

        GlStateManager._bindTexture(textureID);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ALIGNMENT, 4);
        GlStateManager._texSubImage2D(GlConst.GL_TEXTURE_2D, 0, 0, 0, resolution, resolution, GlConst.GL_RGBA, GlConst.GL_UNSIGNED_BYTE, texture);

        // TODO SWITCH TO STANDARD RENDER LAYERS (DIRECT DRAW CALLS MAY BE SLOW)
        // TODO DRAW TEXTURE ON UNDER SIDE MAYBE
        RenderSystem.setShaderTexture(0, textureID);
        RenderSystem.enableDepthTest();

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();

        RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapProgram);

        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
        buffer.vertex(matrix, x0, y0, z0).color(r, g, b, a).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0f, 1f, 0f).next();
        buffer.vertex(matrix, x0, (y0+y1)/2, z1).color(r, g, b, a).texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0f, 1f, 0f).next();
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0f, 1f, 0f).next();
        buffer.vertex(matrix, x1, (y0+y1)/2, z0).color(r, g, b, a).texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0f, 1f, 0f).next();

        Tessellator.getInstance().draw();
    }

    private static int initTexture(int resolution) {
        int texId = TextureUtil.generateTextureId();
        GlStateManager._bindTexture(texId);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0f);

        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_FILTER, GL12.GL_NEAREST);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAG_FILTER, GL12.GL_NEAREST);

        GlStateManager._texImage2D(GlConst.GL_TEXTURE_2D, 0, GlConst.GL_RGBA, resolution, resolution, 0, GlConst.GL_RGBA, GlConst.GL_UNSIGNED_BYTE, null);
        return texId;
    }
}
