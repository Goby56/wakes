package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;

public class SplashPlaneTexture {
    private final GpuTexture texture;
    private final GpuTextureView textureView;
    public final int resolution;

    public SplashPlaneTexture(int resolution) {
        this.resolution = resolution;

        this.texture = RenderSystem.getDevice().createTexture(() -> WakesClient.MOD_ID + " splash plane texture",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8, resolution, resolution, 1, 1);
        this.textureView = RenderSystem.getDevice().createTextureView(texture);
    }

    public void loadTexture(NativeImage image) {
        if (image == null) {
            return;
        }
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, image);
    }

    public GpuTextureView getTextureView() {
        return this.textureView;
    }
}
