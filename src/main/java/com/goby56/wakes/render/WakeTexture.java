package com.goby56.wakes.render;

import com.goby56.wakes.render.enums.RenderType;
import com.goby56.wakes.simulation.Brick;
import com.goby56.wakes.simulation.QuadTree;
import com.goby56.wakes.simulation.WakeNode;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;

public class WakeTexture {
    public int res;
    public int glTexId;

    public WakeTexture(int res) {
        this.res = res;
        this.glTexId = TextureUtil.generateTextureId();

        GlStateManager._bindTexture(glTexId);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0f);

        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_FILTER, GL12.GL_NEAREST);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAG_FILTER, GL12.GL_NEAREST);

        GlStateManager._texImage2D(GlConst.GL_TEXTURE_2D, 0, GlConst.GL_RGBA, QuadTree.BRICK_WIDTH * res, QuadTree.BRICK_WIDTH * res, 0, GlConst.GL_RGBA, GlConst.GL_UNSIGNED_BYTE, null);
    }

    public void render(Matrix4f matrix, Camera camera, Brick brick) {
        if (!brick.hasPopulatedPixels) return;
        GlStateManager._bindTexture(glTexId);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ALIGNMENT, 4);
        GlStateManager._texSubImage2D(GlConst.GL_TEXTURE_2D, 0,0,0,brick.dim * brick.texRes, brick.dim * brick.texRes, GlConst.GL_RGBA, GlConst.GL_UNSIGNED_BYTE, brick.imgPtr);

        RenderSystem.setShaderTexture(0, glTexId);
        RenderSystem.setShader(RenderType.getProgram());
        RenderSystem.enableDepthTest(); // Is it THIS simple? https://github.com/Goby56/wakes/issues/46

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
        Vec3f pos = new Vec3f(brick.pos.add(camera.getPos().negate()).add(0, WakeNode.WATER_OFFSET, 0));

        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        buffer.vertex(matrix, pos.getX(), pos.getY(), pos.getZ())
                .color(1f, 1f, 1f, 1f)
                .texture(0, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(0f, 1f, 0f);
        buffer.vertex(matrix, pos.getX(), pos.getY(), pos.getZ() + brick.dim)
                .color(1f, 1f, 1f, 1f)
                .texture(0, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(0f, 1f, 0f);
        buffer.vertex(matrix, pos.getX() + brick.dim, pos.getY(), pos.getZ() + brick.dim)
                .color(1f, 1f, 1f, 1f)
                .texture(1, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(0f, 1f, 0f);
        buffer.vertex(matrix, pos.getX() + brick.dim, pos.getY(), pos.getZ())
                .color(1f, 1f, 1f, 1f)
                .texture(1, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(0f, 1f, 0f);

        Tessellator.getInstance().draw();
    }
}
