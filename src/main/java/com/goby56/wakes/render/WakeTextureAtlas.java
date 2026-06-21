package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.enums.Resolution;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.resources.Identifier;

import java.util.Arrays;
import java.util.function.Supplier;

public class WakeTextureAtlas {
    public static final int ATLAS_WIDTH = 64; // wake nodes

    public static final Identifier ATLAS_ID = Identifier.fromNamespaceAndPath(WakesClient.MOD_ID, "wake_atlas");

    public final int resolution;

    public int nodeResolution;
    public int effectiveResolution;
    public float uvOffset;

    protected final NativeImage nativeImage;
    public final BetterDynamicTexture dynamicTexture;

    private final boolean[] occupiedSubTextures = new boolean[CAPACITY];

    public static final int CAPACITY = ATLAS_WIDTH * ATLAS_WIDTH - 1; // Include error texture

    public WakeTextureAtlas() {
        resolution = Resolution.getHighest().res * ATLAS_WIDTH;

        nativeImage = new NativeImage(resolution, resolution, false);
        Supplier<String> name = () -> String.format("%s %dx%x texture atlas",
                WakesClient.MOD_ID, resolution, resolution);
        dynamicTexture = new BetterDynamicTexture(name, nativeImage);
        // Register with the texture manager so RenderType can bind it by Identifier
        // (this makes wakes render through the game's buffer system -> works under shaders/Iris)
        Minecraft.getInstance().getTextureManager().register(ATLAS_ID, dynamicTexture);
    }

    public void setResolution(int wakeNodeRes) {
        nodeResolution = wakeNodeRes;
        effectiveResolution = nodeResolution * ATLAS_WIDTH;
        uvOffset = (float) nodeResolution / resolution;

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

    public int occupiedCount() {
        int count = 0;
        for (boolean b : occupiedSubTextures) if (b) count++;
        return count;
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

            this.row = subTextureIndex / ATLAS_WIDTH;
            this.column = subTextureIndex % ATLAS_WIDTH;
        }

        public void invalidate() {
            if (!active) return;
            this.atlas.vacateSubTexture(subTextureIndex);
            this.active = false;
        }

        public float getUVOffset() {
            return atlas.uvOffset;
        }

        public UVPair getUV() {
            return new UVPair(column * atlas.uvOffset, row * atlas.uvOffset);
        }

        public void draw(int x, int y, int color) {
            if (!active) {
                return;
                //throw new IllegalAccessException("Wake texture atlas draw context has been invalidated and cannot be drawn to");
            }
            int globX = x + column * atlas.nodeResolution;
            int globY = y + row * atlas.nodeResolution;
            this.atlas.nativeImage.setPixel(globX, globY, color);
            this.atlas.dynamicTexture.dirty = true;
        }
    }
}
