package com.goby56.wakes.render;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.texture.NativeImage;

import java.nio.IntBuffer;

public class WritableTexture {
    private final IntBuffer pixelBuffer;
    private final int width;
    private final int height;

    private final GpuTexture texture;

    public WritableTexture(int width, int height, String name) {
        this.width = width;
        this.height = height;
        this.pixelBuffer = IntBuffer.allocate(width * height);
        this.texture = RenderSystem.getDevice().createTexture(name, TextureFormat.RGBA8, width, height, 1);
    }

    public int getPixel(int x, int y) {
        this.pixelBuffer.get(y*this.width + x);
    }

    public void setPixel(int x, int y, int color) {
        this.pixelBuffer.put(y*this.width + x, color);
    }

    public void putPixels(IntBuffer pixels) {
        this.pixelBuffer.put(0, pixels, 0, this.width*this.height);
    }

    public void readFromTexture(GpuTexture texture) {
        GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> String.format("Temp. buffer (%s -> %s)", texture.getLabel(), this.texture.getLabel()),
                BufferType.PIXEL_PACK, BufferUsage.STATIC_READ,
                width * height * texture.getFormat().pixelSize() );
        RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(this.texture, buffer, 0, () -> readFromBuffer(buffer), 0);
    }

    public void readFromBuffer(GpuBuffer buffer) {
        GpuBuffer.ReadView readView = RenderSystem.getDevice().createCommandEncoder().readBuffer(buffer);
        this.putPixels(readView.data().asIntBuffer());
    }

    public IntBuffer getPixels() {
        return this.pixelBuffer;
    }

    public void upload() {
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(
                this.texture, this.pixelBuffer, NativeImage.Format.RGBA,
                1, 0, 0, this.width, this.height);
    }

    public void release() {
        this.texture.close();
    }
}
