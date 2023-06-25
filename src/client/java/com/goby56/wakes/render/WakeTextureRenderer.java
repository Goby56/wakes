package com.goby56.wakes.render;

import com.goby56.wakes.utils.WakeHandler;
import com.goby56.wakes.utils.WakeNode;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.Random;

public class WakeTextureRenderer implements WorldRenderEvents.AfterEntities {
    @Override
    public void afterEntities(WorldRenderContext context) {
        WakeHandler wakeHandler = WakeHandler.getInstance();
//        if (wakeHandler.glTexId == -1) {
//            wakeHandler.glTexId = TextureUtil.generateTextureId();
//
//            GlStateManager._bindTexture(wakeHandler.glTexId);
//            GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
//            GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0);
//            GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, 0);
//            GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0f);
//
//            GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_FILTER, GL12.GL_NEAREST);
//            GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAG_FILTER, GL12.GL_NEAREST);
//
//            GlStateManager._texImage2D(GlConst.GL_TEXTURE_2D, 0, GlConst.GL_RGBA, 16, 16, 0, GlConst.GL_RGBA, GlConst.GL_UNSIGNED_BYTE, null);
//        }
//
//        if (wakeHandler.imagePointer == -1) {
//            wakeHandler.imagePointer = MemoryUtil.nmemAlloc(16 * 16 * 4);
//        }

//        Random r = new Random();
//        for (int i = 0; i < 16 * 16; i++) {
//            MemoryUtil.memPutInt(wakeHandler.imagePointer + i * 4L, 0xFF << 8 * 3 | r.nextInt(0xFFFFFF));
//        }
//
//        GlStateManager._bindTexture(wakeHandler.glTexId);
//        GlStateManager._pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, 0);
//        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
//        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
//        GlStateManager._pixelStore(GlConst.GL_UNPACK_ALIGNMENT, 4);
//        GlStateManager._texSubImage2D(GlConst.GL_TEXTURE_2D, 0, 0, 0, 16, 16, GlConst.GL_RGBA, GlConst.GL_UNSIGNED_BYTE, wakeHandler.imagePointer);

        ArrayList<WakeNode> nodes = WakeHandler.getInstance().getNearby(context.camera().getPos());
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();

        Random r = new Random();
        for (WakeNode node : nodes) {
            Vec3d pos = node.getPos().add(context.camera().getPos().negate());

            for (int i = 0; i < 16 * 16; i++) {
                MemoryUtil.memPutInt(wakeHandler.imagePointer + i * 4L, 0xFF << 8 * 3 | (int) node.values[i%4][i%4]);
            }

            GlStateManager._bindTexture(wakeHandler.glTexId);
            GlStateManager._pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, 0);
            GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
            GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
            GlStateManager._pixelStore(GlConst.GL_UNPACK_ALIGNMENT, 4);
            GlStateManager._texSubImage2D(GlConst.GL_TEXTURE_2D, 0, 0, 0, 16, 16, GlConst.GL_RGBA, GlConst.GL_UNSIGNED_BYTE, wakeHandler.imagePointer);

            RenderSystem.setShaderTexture(0, wakeHandler.glTexId);

            renderTexture(matrix, (float) pos.x, (float) pos.y, (float) pos.z, (float) (pos.x + 1), (float) pos.y, (float) (pos.z + 1));
        }
    }

    private static void renderTexture(Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.enableDepthTest();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(matrix, x0, y0, z0).texture(0, 0).next();
        buffer.vertex(matrix, x0, (y0+y1)/2, z1).texture(0, 1).next();
        buffer.vertex(matrix, x1, y1, z1).texture(1, 1).next();
        buffer.vertex(matrix, x1, (y0+y1)/2, z0).texture(1, 0).next();
        Tessellator.getInstance().draw();
        // TODO DRAW TEXTURE ON UNDER SIDE
    }
}
