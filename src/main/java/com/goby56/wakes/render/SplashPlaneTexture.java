package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.opengl.GlTexture;

public class SplashPlaneTexture {
    public GpuTexture texture;
    private final GpuTextureView textureView;
    public final int resolution;

    public SplashPlaneTexture(int resolution) {
        this.resolution = resolution;

        this.texture = RenderSystem.getDevice().createTexture(() -> WakesClient.MOD_ID + " splash plane texture",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
                TextureFormat.RGBA8, resolution, resolution, 1, 1);
        // texture.setTextureFilter(FilterMode.NEAREST, false);
        this.textureView = RenderSystem.getDevice().createTextureView(texture);
    }

    public void loadTexture(long imgPtr, int glFormat) {
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ALIGNMENT, 4);

        GlStateManager._bindTexture(((GlTexture) texture).glId());
        GlStateManager._texSubImage2D(GlConst.GL_TEXTURE_2D, 0,0,0,resolution, resolution, glFormat, GlConst.GL_UNSIGNED_BYTE, imgPtr);

        RenderSystem.outputColorTextureOverride = textureView;
    }
}
