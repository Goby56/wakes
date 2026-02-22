package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.enums.Resolution;
import com.goby56.wakes.simulation.WakeChunk;
import com.goby56.wakes.simulation.WakeHandler;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.opengl.GlTexture;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.util.Arrays;
import java.util.function.Supplier;

public class WakeTextureAtlas {
    public static final int CHUNKS_PER_ROW = 16;

    public final int resolution;

    public int nodeResolution;
    public int chunkResolution;
    public int effectiveResolution;
    public float chunkUVOffset;

    protected final NativeImage nativeImage;
    public final BetterDynamicTexture dynamicTexture;

    private final boolean[] occupiedSubTextures = new boolean[CAPACITY];

    private static final int CAPACITY = CHUNKS_PER_ROW * CHUNKS_PER_ROW - 1; // Include error texture

    public WakeTextureAtlas() {
        resolution = Resolution.getHighest().res * WakeChunk.WIDTH * CHUNKS_PER_ROW;

        nativeImage = new NativeImage(resolution, resolution, false);
        Supplier<String> name = () -> String.format("%s %dx%x texture atlas",
                WakesClient.MOD_ID, resolution, resolution);
        dynamicTexture = new BetterDynamicTexture(name, nativeImage);
    }

    public void setResolution(int wakeNodeRes) {
        nodeResolution = wakeNodeRes;
        chunkResolution = wakeNodeRes * WakeChunk.WIDTH;
        effectiveResolution = chunkResolution * CHUNKS_PER_ROW;
        chunkUVOffset = chunkResolution / (float) resolution;

        Arrays.fill(occupiedSubTextures, false);
    }

    public DrawContext claimSubTexture() {
        for (int i = 0; i < CAPACITY; i++) {
            if (!occupiedSubTextures[i]) {
                occupiedSubTextures[i] = true;
                return new DrawContext(i, this);
            }
        }
        return null;
    }

    protected void vacateSubTexture(int subTextureIndex) {
        occupiedSubTextures[subTextureIndex] = false;
    }

    public static class DrawContext {
        private boolean active;
        private final int subTextureIndex;
        private final WakeTextureAtlas atlas;

        public final int row;
        public final int column;

        public DrawContext(int subTextureIndex, WakeTextureAtlas atlas) {
            this.subTextureIndex = subTextureIndex;
            this.active = true;
            this.atlas = atlas;

            this.row = subTextureIndex / CHUNKS_PER_ROW;
            this.column = subTextureIndex % CHUNKS_PER_ROW;
        }

        public void invalidate() {
            this.atlas.vacateSubTexture(subTextureIndex);
            this.active = false;
        }

        public float getUVOffset() {
            return atlas.chunkUVOffset;
        }

        public UVPair getUV() {
            float uvOffset = atlas.chunkUVOffset;
            return new UVPair(column * uvOffset, row * uvOffset);
        }

        public void draw(int x, int y, int color) {
            if (!active) {
                return;
                //throw new IllegalAccessException("Wake texture atlas draw context has been invalidated and cannot be drawn to");
            }
            int globX = x + column * atlas.chunkResolution;
            int globY = y + row * atlas.chunkResolution;
            this.atlas.nativeImage.setPixel(globX, globY, color);
            this.atlas.dynamicTexture.dirty = true;
        }
    }
}
