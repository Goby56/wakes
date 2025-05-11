package com.goby56.wakes.render;

import com.goby56.wakes.simulation.QuadTree;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.texture.NativeImage;

import java.nio.IntBuffer;

public class WakeTexture {
    public int res;
    public final GpuTexture texture;
    public final boolean isUsingBricks;


    public WakeTexture(int res, boolean useBricks) {
        this.res = useBricks ? QuadTree.BRICK_WIDTH * res : res;
        this.texture = RenderSystem.getDevice().createTexture(String.format("Wake texture %dx%d", this.res, this.res), TextureFormat.RGBA8, this.res, this.res, 1);
        this.isUsingBricks = useBricks;
    }

    public void loadTexture(IntBuffer pixels) {
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(
                this.texture, pixels, NativeImage.Format.RGBA,
                0, 0, 0, this.res, this.res);
    }
}
