package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.render.enums.RenderType;
import com.goby56.wakes.render.enums.WakeColor;
import com.goby56.wakes.simulation.WakeNode;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;

public class DynamicWakeTexture {

    public int res;
    public int glTexId;
    public long imgPtr;

    public DynamicWakeTexture(int resolution) {
        this.res = resolution;
        this.glTexId = TextureUtil.generateTextureId();
        this.imgPtr = MemoryUtil.nmemAlloc((long) resolution * resolution * 4);

        GlStateManager._bindTexture(this.glTexId);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0f);

        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_FILTER, GL12.GL_NEAREST);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAG_FILTER, GL12.GL_NEAREST);

        GlStateManager._texImage2D(GlConst.GL_TEXTURE_2D, 0, GlConst.GL_RGBA, resolution, resolution, 0, GlConst.GL_RGBA, GlConst.GL_UNSIGNED_BYTE, null);
    }

    public static DynamicWakeTexture fromDist(float distance) {
        int res = WakesClient.CONFIG_INSTANCE.wakeResolution.res;
        return new DynamicWakeTexture((int) Math.ceil(res * Math.exp(-distance / res)));
    }

    public void render(Matrix4f matrix, float x, float y, float z, int light) {
        GlStateManager._bindTexture(this.glTexId);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ALIGNMENT, 4);
        GlStateManager._texSubImage2D(GlConst.GL_TEXTURE_2D, 0, 0, 0, this.res, this.res, GlConst.GL_RGBA, GlConst.GL_UNSIGNED_BYTE, this.imgPtr);

        RenderSystem.setShaderTexture(0, this.glTexId);
        RenderSystem.setShader(RenderType.getProgram());
        RenderSystem.enableDepthTest(); // Is it THIS simple? https://github.com/Goby56/wakes/issues/46

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

        y = MinecraftClient.isFabulousGraphicsOrBetter() ? y - 1e-2f : y;
        // Render below water because changing opacity when using fabulous settings makes it completely transparent
        // Temporary fix as it only moves the problem to when looking from underneath
        // Will not give completely white foam parts on wakes
        // TODO FIX ^
        buffer.vertex(matrix, x, y, z).color(1f, 1f, 1f, 1f).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0f, 1f, 0f).next();
        buffer.vertex(matrix, x, y, z + 1).color(1f, 1f, 1f, 1f).texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0f, 1f, 0f).next();
        buffer.vertex(matrix, x + 1, y, z + 1).color(1f, 1f, 1f, 1f).texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0f, 1f, 0f).next();
        buffer.vertex(matrix, x + 1, y, z).color(1f, 1f, 1f, 1f).texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0f, 1f, 0f).next();

        Tessellator.getInstance().draw();
    }
}
