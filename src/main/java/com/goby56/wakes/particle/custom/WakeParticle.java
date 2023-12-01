package com.goby56.wakes.particle.custom;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.particle.SplashPlaneParticleType;
import com.goby56.wakes.particle.WakeParticleType;
import com.goby56.wakes.render.BlendingFunction;
import com.goby56.wakes.render.WakeTextureRenderer;
import com.goby56.wakes.utils.WakeColor;
import com.goby56.wakes.utils.WakeHandler;
import com.goby56.wakes.utils.WakeNode;
import com.goby56.wakes.utils.WakesUtils;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.system.MemoryUtil;

public class WakeParticle extends Particle {
    WakeNode node;
    Vec3d pos;

    protected WakeParticle(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    public void tick() {
        if (this.node == null || this.node.isDead()) {
            this.markDead();
        }
    }

    @Override
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        if (!WakesClient.CONFIG_INSTANCE.renderWakes) {
            return;
        }
        WakeHandler wakeHandler = WakeHandler.getInstance();
        if (wakeHandler == null || wakeHandler.resetScheduled) return;

        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer buffer = immediate.getBuffer(RenderLayer.getTranslucent());

        Matrix4f matrix = WakesUtils.getMatrixStackFromCamera(camera, tickDelta, this.pos, this.pos).peek().getPositionMatrix();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.disableCull(); // Rendering from underneath the water
        MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();

        BlendingFunction blendMode = WakesClient.CONFIG_INSTANCE.blendMode;
        if (blendMode == BlendingFunction.DEFAULT) {
            RenderSystem.defaultBlendFunc();
        } else {
            RenderSystem.blendFunc(blendMode.srcFactor, blendMode.dstFactor);
        }

        // TODO MOVE THIS TO RESOLUTION CHANGE EVENT
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
        if (node.isDead() || res != node.res) return;

        Vec3d screenSpace = node.getPos().add(camera.getPos().negate());
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

        waterCol = BiomeColors.getWaterColor(wakeHandler.world, node.blockPos());
        light = WorldRenderer.getLightmapCoordinates(wakeHandler.world, node.blockPos());

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

        // TODO REMOVE REDUNDANT "FOAM" RENDERING. FIND BLEND FUNCTION THAT ALLOWS COMPLETELY WHITE COLORS
        renderTexture(buffer, res, wakeHandler.glWakeTexId, wakeHandler.wakeImgPtr, matrix, x, y, z, x + 1, y, z + 1, r, g, b, a, light);
        renderTexture(buffer, res, wakeHandler.glFoamTexId, wakeHandler.foamImgPtr, matrix, x, y, z, x + 1, y, z + 1, 1f, 1f, 1f, a, light);

        immediate.draw();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
    }

    private static void renderTexture(VertexConsumer buffer, int resolution, int textureID, long texture, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, float r, float g, float b, float a, int light) {
        GlStateManager._bindTexture(textureID);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ALIGNMENT, 4);
        GlStateManager._texSubImage2D(GlConst.GL_TEXTURE_2D, 0, 0, 0, resolution, resolution, GlConst.GL_RGBA, GlConst.GL_UNSIGNED_BYTE, texture);

        // TODO SWITCH TO STANDARD RENDER LAYERS (DIRECT DRAW CALLS MAY BE SLOW)
        RenderSystem.setShaderTexture(0, textureID);
        RenderSystem.enableDepthTest();

        RenderSystem.setShader(GameRenderer::getRenderTypeTranslucentProgram);

        buffer.vertex(matrix, x0, y0, z0).color(r, g, b, a).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0f, 1f, 0f).next();
        buffer.vertex(matrix, x0, (y0+y1)/2, z1).color(r, g, b, a).texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0f, 1f, 0f).next();
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0f, 1f, 0f).next();
        buffer.vertex(matrix, x1, (y0+y1)/2, z0).color(r, g, b, a).texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0f, 1f, 0f).next();
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

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.CUSTOM;
    }


    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {

        public Factory(SpriteProvider spriteSet) {
        }

        @Nullable
        @Override
        public Particle createParticle(DefaultParticleType parameters, ClientWorld world, double x, double y, double z, double velX, double velY, double velZ) {
            WakeParticle wake = new WakeParticle(world, x, y, z);
            wake.pos = new Vec3d(x, y, z);
            if (parameters instanceof WakeParticleType type) {
                wake.node = type.node;
            }
            return wake;
        }
    }
}
