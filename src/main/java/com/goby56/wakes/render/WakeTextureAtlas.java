package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.simulation.WakeChunk;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.opengl.GlTexture;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.util.function.Supplier;

public class WakeTextureAtlas {
    public static final int CHUNKS_PER_ROW = 16;

    public final int nodeResolution;
    public final int chunkResolution;
    public final int resolution;

    protected final NativeImage nativeImage;
    public final BetterDynamicTexture dynamicTexture;

    private final boolean[] occupiedSubTextures = new boolean[CAPACITY];

    private static final int CAPACITY = CHUNKS_PER_ROW * CHUNKS_PER_ROW - 1; // Include error texture

    public WakeTextureAtlas(int wakeNodeRes) {
        nodeResolution = wakeNodeRes;
        chunkResolution = wakeNodeRes * WakeChunk.WIDTH;
        resolution = chunkResolution * CHUNKS_PER_ROW;

        nativeImage = new NativeImage(resolution, resolution, false);
        Supplier<String> name = () -> String.format("%s %dx%x texture atlas (%d%d wakes)",
                WakesClient.MOD_ID, resolution, resolution, nodeResolution, nodeResolution);
        dynamicTexture = new BetterDynamicTexture(name, nativeImage);
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

        public final UVPair uv;
        public final float uvOffset;
        public final int nodeResolution;

        public DrawContext(int subTextureIndex, WakeTextureAtlas atlas) {
            this.subTextureIndex = subTextureIndex;
            this.active = true;
            this.atlas = atlas;

            int r = subTextureIndex / CHUNKS_PER_ROW;
            int c = subTextureIndex % CHUNKS_PER_ROW;
            this.uv = new UVPair(c / (float) CHUNKS_PER_ROW, r / (float) CHUNKS_PER_ROW);
            this.uvOffset = atlas.chunkResolution / (float) atlas.resolution;
            this.nodeResolution = atlas.nodeResolution;
        }

        public void invalidate() {
            this.atlas.vacateSubTexture(subTextureIndex);
            this.active = false;
        }

        public void draw(int x, int y, int color) {
            if (!active) {
                return;
                //throw new IllegalAccessException("Wake texture atlas draw context has been invalidated and cannot be drawn to");
            }
            int r = subTextureIndex / CHUNKS_PER_ROW;
            int c = subTextureIndex % CHUNKS_PER_ROW;
            int globX = x + c * CHUNKS_PER_ROW;
            int globY = y + r * CHUNKS_PER_ROW;
            this.atlas.nativeImage.setPixel(globX, globY, color);
        }
    }
}
