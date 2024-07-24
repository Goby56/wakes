package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.render.enums.RenderType;
import com.goby56.wakes.render.enums.WakeColor;
import com.goby56.wakes.simulation.Brick;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;

public class WakeRenderer implements WorldRenderEvents.AfterTranslucent {
    public static int nodesRendered = 0;
    public static int res;
    public static int glTexId;
    public static long imgPtr;

    public record WakeQuad(int x, int z, int w, int h, WakeNode[][] affectedNodes) {
    }

    @Override
    public void afterTranslucent(WorldRenderContext context) {
        if (WakesClient.CONFIG_INSTANCE.disableMod) {
            return;
        }
        WakeHandler wakeHandler = WakeHandler.getInstance();
        if (wakeHandler == null || wakeHandler.resolutionResetScheduled) return;

        ArrayList<Brick> bricks = wakeHandler.getVisible(context.frustum());
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        context.lightmapTextureManager().enable();
        prepTextures();

        float x, z;

        int n = 0;
        for (var brick : bricks) {
            if (WakesClient.CONFIG_INSTANCE.wakeResolution.res != WakeNode.res) continue;
            Vec3d screenSpace = brick.getPos().add(context.camera().getPos().negate());
            x = (float) screenSpace.x;
            z = (float) screenSpace.z;

           for (var quad : BrickMesher.generateMesh(brick)) {
               render(matrix, quad, x, z, wakeHandler.world);
           }
           n++;
        }
        RenderSystem.enableCull();

        nodesRendered = n;

    }

    public static void prepTextures() {
        GlStateManager._bindTexture(glTexId);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0f);

        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_FILTER, GL12.GL_NEAREST);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAG_FILTER, GL12.GL_NEAREST);
    }

    public static void render(Matrix4f matrix, WakeQuad quad, float gx, float gz, World world) {
        int x = quad.x();
        int z = quad.z();
        int w = quad.w();
        int h = quad.h();
        WakeNode[][] nodes = quad.affectedNodes();

        res = WakesClient.CONFIG_INSTANCE.wakeResolution.res;
        glTexId = TextureUtil.generateTextureId();
        imgPtr = MemoryUtil.nmemAlloc((long) w * res * h * res * 4);

        populatePixels(x, z, w, h, nodes, world);
        draw(matrix, x + gx, z + gz, w, h, nodes, world);
    }

    private static void populatePixels(int x, int z, int w, int h, WakeNode[][] nodes, World world) {
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                WakeNode node = nodes[i][j];
                long nodeOffset = (((i*(long) w)+j)*h);
                int waterCol = BiomeColors.getWaterColor(world, node.blockPos());
                float opacity = (float) ((-Math.pow(node.t, 2) + 1) * WakesClient.CONFIG_INSTANCE.wakeOpacity);
                for (int r = 0; r < res; r++) {
                    for (int c = 0; c < res; c++) {
                        float avg = 0;
                        avg += (node.u[0][r + 1][c + 1] + node.u[1][r + 1][c + 1] + node.u[2][r + 1][c + 1]) / 3;
                        int color = WakeColor.getColor(avg, waterCol, opacity);
                        long pixelOffset = (((r*(long) res)+c)*4);
                        MemoryUtil.memPutInt(imgPtr + nodeOffset + pixelOffset, color);
                    }
                }
            }
        }
    }

    private static void draw(Matrix4f matrix, float x, float z, int w, int h, WakeNode[][] nodes, World world) {
        GlStateManager._bindTexture(glTexId);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ALIGNMENT, 4);
        GlStateManager._texSubImage2D(GlConst.GL_TEXTURE_2D, 0, 0, 0, w * res, h * res, GlConst.GL_RGBA, GlConst.GL_UNSIGNED_BYTE, imgPtr);

        RenderSystem.setShaderTexture(0, glTexId);
        RenderSystem.setShader(RenderType.getProgram());
        RenderSystem.enableDepthTest(); // Is it THIS simple? https://github.com/Goby56/wakes/issues/46

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

        int X = nodes.length - 1;
        int Z = nodes[0].length - 1;
        buffer.vertex(matrix, x, nodes[0][0].height, z)
                .color(1f, 1f, 1f, 1f)
                .texture(0, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light(nodes[0][0], world))
                .normal(0f, 1f, 0f).next();
        buffer.vertex(matrix, x, nodes[0][Z].height, z + 1)
                .color(1f, 1f, 1f, 1f)
                .texture(0, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light(nodes[0][Z], world))
                .normal(0f, 1f, 0f).next();
        buffer.vertex(matrix, x + 1, nodes[X][Z].height, z + 1)
                .color(1f, 1f, 1f, 1f)
                .texture(1, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light(nodes[X][Z], world))
                .normal(0f, 1f, 0f).next();
        buffer.vertex(matrix, x + 1, nodes[X][0].height, z)
                .color(1f, 1f, 1f, 1f)
                .texture(1, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light(nodes[X][0], world))
                .normal(0f, 1f, 0f).next();

        Tessellator.getInstance().draw();
    }

    private static int light(WakeNode node, World world) {
        return WorldRenderer.getLightmapCoordinates(world, node.blockPos());
    }
}
