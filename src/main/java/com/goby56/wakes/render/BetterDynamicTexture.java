package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.util.function.Supplier;

public class BetterDynamicTexture extends AbstractTexture {
    private NativeImage pixels;

    public BetterDynamicTexture(Supplier<String> supplier, NativeImage nativeImage) {
        this.pixels = nativeImage;
        this.createTexture(supplier);
    }

    private void createTexture(Supplier<String> supplier) {
        GpuDevice gpuDevice = RenderSystem.getDevice();
        this.texture = gpuDevice.createTexture(supplier, 5, TextureFormat.RGBA8, this.pixels.getWidth(), this.pixels.getHeight(), 1, 1);
        this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
        this.textureView = gpuDevice.createTextureView(this.texture);
    }

    public void upload() {
        if (this.texture != null) {
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, this.pixels);
        } else {
            WakesClient.LOGGER.warn("Trying to upload disposed texture {}", this.getTexture().getLabel());
        }
    }

    public void close() {
        this.pixels.close();
        super.close();
    }
}
