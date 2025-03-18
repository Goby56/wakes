package com.goby56.wakes.render;

import com.goby56.wakes.simulation.QuadTree;
import com.goby56.wakes.simulation.WakeHandler;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;

public class WakeTexture {
    public int res;
    public int glTexId;
    public final boolean isUsingBricks;
    private final int resolutionScaling;

    public WakeTexture(int res, boolean useBricks) {
        this.res = res;
        this.glTexId = TextureUtil.generateTextureId();
        this.isUsingBricks = useBricks;
        this.resolutionScaling = useBricks ? QuadTree.BRICK_WIDTH : 1;

        GlStateManager._bindTexture(glTexId);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0f);

        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_FILTER, GL12.GL_NEAREST);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAG_FILTER, GL12.GL_NEAREST);

        GlStateManager._texImage2D(GlConst.GL_TEXTURE_2D, 0, GlConst.GL_RGBA, resolutionScaling * res, resolutionScaling * res, 0, GlConst.GL_RGBA, GlConst.GL_UNSIGNED_BYTE, null);
    }

    public void loadTexture(long imgPtr, int glFormat) {
        GlStateManager._bindTexture(glTexId);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ALIGNMENT, 4);

        int dim = resolutionScaling * WakeHandler.resolution.res;
        GlStateManager._texSubImage2D(GlConst.GL_TEXTURE_2D, 0,0,0,dim, dim, glFormat, GlConst.GL_UNSIGNED_BYTE, imgPtr);

        RenderSystem.setShaderTexture(0, glTexId);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.enableDepthTest(); // Is it THIS simple? https://github.com/Goby56/wakes/issues/46
        RenderSystem.disableCull();
    }
}
