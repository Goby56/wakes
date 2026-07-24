package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.GpuFormat;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

public class BetterDynamicTexture extends AbstractTexture {
    private NativeImage pixels;

    public BetterDynamicTexture(Supplier<String> supplier, NativeImage nativeImage) {
        this.pixels = nativeImage;
        this.createTexture(supplier);
    }

    private void createTexture(Supplier<String> supplier) {
        GpuDevice gpuDevice = RenderSystem.getDevice();
        this.texture = gpuDevice.createTexture(supplier, 5, GpuFormat.RGBA8_UNORM, this.pixels.getWidth(), this.pixels.getHeight(), 1, 1);
        this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
        this.textureView = gpuDevice.createTextureView(this.texture);
    }

    // uploads just the given sub-rectangle instead of the whole atlas, since only a few
    // node-sized regions actually change per frame outside of a real simulation tick
    public void uploadRegion(NativeImage region, int x, int y) {
        if (this.texture != null) {
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, region, 0, 0, x, y);
        } else {
            WakesClient.LOGGER.warn("Trying to upload disposed texture {}", this.getTexture().getLabel());
        }
    }

    // uploads the top `rows` rows (full width) of `source` directly from its own backing buffer,
    // with no copy: a row-major image's top rows are already a contiguous prefix of its memory,
    // so this is just a bounded view into the same bytes the full-atlas upload would already send
    public void uploadTopRows(NativeImage source, int rows) {
        if (this.texture != null) {
            ByteBuffer buf = source.getPixelBytes();
            buf.rewind();
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, buf, 0, 0, 0, 0, source.getWidth(), rows);
        } else {
            WakesClient.LOGGER.warn("Trying to upload disposed texture {}", this.getTexture().getLabel());
        }
    }

    public void close() {
        this.pixels.close();
        super.close();
    }
}
