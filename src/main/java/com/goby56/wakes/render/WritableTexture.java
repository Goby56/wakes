package com.goby56.wakes.render;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.texture.NativeImage;

import java.nio.IntBuffer;

public class WritableTexture {
    private final GpuTexture texture; // TODO SWITCH TO BUFFER

    public final int width;
    public final int height;

    public WritableTexture(int width, int height, String id) {
        this.width = width;
        this.height = height;
        this.texture = RenderSystem.getDevice().createTexture(id, TextureFormat.RGBA8, width, height, 1);
        this.texture.setTextureFilter(FilterMode.NEAREST, FilterMode.NEAREST, false);
    }

    public void updateTexture(IntBuffer imgPtr) {
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, imgPtr, NativeImage.Format.RGBA, 0, 0, 0, this.width, this.height);
    }

    public IntBuffer getPixels() {
        GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "Texture output buffer",
                BufferType.PIXEL_PACK, BufferUsage.STATIC_READ,
                width * height * texture.getFormat().pixelSize() );
        RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(this.texture, gpuBuffer, 0, () -> {<CALLBACK HERE>}, 0);
        GpuBuffer.ReadView readView = RenderSystem.getDevice().createCommandEncoder().readBuffer(gpuBuffer);
        readView.data().asIntBuffer() << ACTUAL VALUE WOULD BE RETRIEVED LIKE THIS
    }


    public void release() {
        this.texture.close();
    }
}
