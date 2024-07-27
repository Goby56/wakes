package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.render.enums.RenderType;
import com.goby56.wakes.render.enums.WakeColor;
import com.goby56.wakes.simulation.Brick;
import com.goby56.wakes.simulation.WakeNode;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.system.MemoryUtil;

public class WakeTexture {
    public int res;
    public int glTexId;
    public long imgPtr;

    public WakeTexture(int res) {
        this.res = res;
        this.glTexId = TextureUtil.generateTextureId();
        this.imgPtr = MemoryUtil.nmemAlloc((long) 32 * res * 32 * res * 4);

        GlStateManager._bindTexture(glTexId);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0f);

        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_FILTER, GL12.GL_NEAREST);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAG_FILTER, GL12.GL_NEAREST);

        GlStateManager._texImage2D(GlConst.GL_TEXTURE_2D, 0, GlConst.GL_RGBA, 32 * res, 32 * res, 0, GlConst.GL_RGBA, GlConst.GL_UNSIGNED_BYTE, null);
    }

    public void render(Matrix4f matrix, Camera camera, Brick brick, WakeQuad quad, World world) {
        int x = quad.x, z = quad.z, w = quad.w, h = quad.h;
        WakeNode[][] nodes = quad.nodes;

        Vec3d screenSpace = brick.getPos().add(camera.getPos().negate());
        float xPos = (float) screenSpace.x + x;
        float zPos = (float) screenSpace.z + z;
        GlStateManager._bindTexture(glTexId);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ALIGNMENT, 4);
        GlStateManager._texSubImage2D(GlConst.GL_TEXTURE_2D, 0, x * res, z * res, w * res, h * res, GlConst.GL_RGBA, GlConst.GL_UNSIGNED_BYTE, imgPtr);

        RenderSystem.setShaderTexture(0, glTexId);
        RenderSystem.setShader(RenderType.getProgram());
        RenderSystem.enableDepthTest(); // Is it THIS simple? https://github.com/Goby56/wakes/issues/46

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

        int X = nodes.length - 1;
        int Z = nodes[0].length - 1;
        buffer.vertex(matrix, xPos, nodes[0][0].height, zPos)
                .color(1f, 1f, 1f, 1f)
                .texture(0, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light(nodes[0][0], world))
                .normal(0f, 1f, 0f).next();
        buffer.vertex(matrix, xPos, nodes[0][Z].height, zPos + 1)
                .color(1f, 1f, 1f, 1f)
                .texture(0, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light(nodes[0][Z], world))
                .normal(0f, 1f, 0f).next();
        buffer.vertex(matrix, xPos + 1, nodes[X][Z].height, zPos + 1)
                .color(1f, 1f, 1f, 1f)
                .texture(1, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light(nodes[X][Z], world))
                .normal(0f, 1f, 0f).next();
        buffer.vertex(matrix, xPos + 1, nodes[X][0].height, zPos)
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
