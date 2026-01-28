package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.simulation.WakeChunk;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.opengl.GlTexture;
import net.minecraft.client.model.geom.builders.UVPair;

public class WakeTextureAtlas {
    public static final int CHUNKS_PER_ROW = 16;

    public final int chunkResolution;
    public final int resolution;

    public GpuTexture texture;
    private final GpuTextureView textureView;

    private int chunkTexturesStored = 0;
    private static final int CAPACITY = CHUNKS_PER_ROW * CHUNKS_PER_ROW - 1;

    public WakeTextureAtlas(int wakeNodeRes) {
        chunkResolution = wakeNodeRes * WakeChunk.WIDTH;
        resolution = chunkResolution * CHUNKS_PER_ROW;

        texture = RenderSystem.getDevice().createTexture(() -> WakesClient.MOD_ID + " wake texture atlas",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
                TextureFormat.RGBA8, resolution, resolution, 1, 1);
        // texture.setTextureFilter(FilterMode.NEAREST, false);

        textureView = RenderSystem.getDevice().createTextureView(texture);
    }

    public UVPair loadTexture(long imgPtr, int glFormat) {
        if (chunkTexturesStored == CAPACITY) {
            float undefinedTextureCoord = 1f - 1 / (float) CHUNKS_PER_ROW;
            return new UVPair(undefinedTextureCoord, undefinedTextureCoord);
        }

        GlStateManager._pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ALIGNMENT, 4);

        int r = chunkTexturesStored / CHUNKS_PER_ROW;
        int c = chunkTexturesStored % CHUNKS_PER_ROW;
        int xOffset = c * chunkResolution;
        int yOffset = r * chunkResolution;

        GlStateManager._bindTexture(((GlTexture) texture).glId());
        GlStateManager._texSubImage2D(GlConst.GL_TEXTURE_2D, 0, xOffset, yOffset, chunkResolution, chunkResolution, glFormat, GlConst.GL_UNSIGNED_BYTE, imgPtr);

        RenderSystem.outputColorTextureOverride = textureView;
        chunkTexturesStored++;

        return new UVPair(c / (float) CHUNKS_PER_ROW, r / (float) CHUNKS_PER_ROW);
    }

    public GpuTextureView getTextureView() {
        return textureView;
    }

    public void markDirty() {
        this.chunkTexturesStored = 0;
    }
}
