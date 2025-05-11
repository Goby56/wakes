package com.goby56.wakes.simulation;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.debug.WakesDebugInfo;
import com.goby56.wakes.utils.WakesUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class Brick {
    private final WakeNode[][] nodes;
    public final int capacity;
    public final int dim;

    public int occupied = 0;

    public final Vec3d pos;

    public Brick NORTH;
    public Brick EAST;
    public Brick SOUTH;
    public Brick WEST;

    public IntBuffer pixels;
    public int texRes;
    public boolean hasPopulatedPixels = false;

    public Brick(int x, float y, int z, int width) {
        this.dim = width;
        this.capacity = dim * dim;
        this.nodes = new WakeNode[dim][dim];
        this.pos = new Vec3d(x, y, z);

        initTexture(WakeHandler.resolution.res);
    }

    public void initTexture(int res) {
        if (pixels == null) {
            this.pixels = BufferUtils.createIntBuffer(dim*dim*res*res);
        }
        this.texRes = res;
        this.hasPopulatedPixels = false;
    }

    public void deallocTexture() {
        this.pixels.clear();
    }

    public boolean tick(WakeHandler wakeHandler) {
        long tNode = System.nanoTime();
        for (int z = 0; z < dim; z++) {
            for (int x = 0; x < dim; x++) {
                if (this.get(x, z) == null) continue;

                if (!this.get(x, z).tick(wakeHandler)) {
                    this.clear(x, z);
                }
            }
        }
        WakesDebugInfo.nodeLogicTime += (System.nanoTime() - tNode);
        long tTexturing = System.nanoTime();
        populatePixels();
        WakesDebugInfo.texturingTime += (System.nanoTime() - tTexturing);
        WakesDebugInfo.nodeCount += occupied;
        return occupied != 0;
    }

    public void query(Frustum frustum, ArrayList<WakeNode> output) {
        for (int z = 0; z < dim; z++) {
            for (int x = 0; x < dim; x++) {
                var node = this.get(x, z);
                if (node == null) continue;
                Box b = node.toBox();
                if (frustum.isVisible(b)) output.add(node);
            }
        }
    }

    public WakeNode get(int x, int z) {
        if (x >= 0 && x < dim) {
            if (z < 0 && NORTH != null) {
                return NORTH.nodes[Math.floorMod(z, dim)][x];
            } else if (z >= dim && SOUTH != null) {
                return SOUTH.nodes[Math.floorMod(z, dim)][x];
            } else if (z >= 0 && z < dim){
                return nodes[z][x];
            }
        }
        if (z >= 0 && z < dim) {
            if (x < 0 && WEST != null) {
                return WEST.nodes[z][Math.floorMod(x, dim)];
            } else if (x >= dim && EAST != null) {
                return EAST.nodes[z][Math.floorMod(x, dim)];
            }
        }
        return null;
    }

    public void insert(WakeNode node) {
        int x = Math.floorMod(node.x, dim), z = Math.floorMod(node.z, dim);
        if (nodes[z][x] != null) {
            nodes[z][x].revive(node);
            return;
        }
        this.set(x, z, node);
        for (WakeNode neighbor : getAdjacentNodes(x, z)) {
            neighbor.updateAdjacency(node);
        }
    }

    protected void set(int x, int z, WakeNode node) {
        boolean wasNull = nodes[z][x] == null;
        nodes[z][x] = node;
        if (node == null) {
            if (!wasNull) this.occupied--;
        } else {
            if (wasNull) this.occupied++;
        }
    }

    public void clear(int x, int z) {
        this.set(x, z, null);
    }

    private List<WakeNode> getAdjacentNodes(int x, int z) {
        return Stream.of(
                this.get(x, z + 1),
                this.get(x + 1, z),
                this.get(x, z - 1),
                this.get(x - 1, z)).filter(Objects::nonNull).toList();
    }

    public void updateAdjacency(Brick brick) {
        if (brick.pos.x == this.pos.x && brick.pos.z == this.pos.z - dim) {
            this.NORTH = brick;
            brick.SOUTH = this;
            return;
        }
        if (brick.pos.x == this.pos.x + dim && brick.pos.z == this.pos.z) {
            this.EAST = brick;
            brick.WEST = this;
            return;
        }
        if (brick.pos.x == this.pos.x && brick.pos.z == this.pos.z + dim) {
            this.SOUTH = brick;
            brick.NORTH = this;
            return;
        }
        if (brick.pos.x == this.pos.x - dim && brick.pos.z == this.pos.z) {
            this.WEST = brick;
            brick.EAST = this;
        }
    }

    public void populatePixels() {
        World world = MinecraftClient.getInstance().world;
        for (int z = 0; z < dim; z++) {
            for (int x = 0; x < dim; x++) {
                WakeNode node = this.get(x, z);
                int lightCol = LightmapTextureManager.MAX_LIGHT_COORDINATE;
                int fluidColor = 0;
                float opacity = 0;
                if (node != null) {
                    fluidColor = BiomeColors.getWaterColor(world, node.blockPos());
                    lightCol = WakesUtils.getLightColor(world, node.blockPos());
                    // TODO LERP LIGHT FROM SURROUNDING BLOCKS
                    opacity = (float) ((-Math.pow(node.t, 2) + 1) * WakesConfig.wakeOpacity);
                }

                // TODO MASS SET PIXELS TO NO COLOR IF NODE DOESNT EXIST (NEED TO REORDER PIXELS STORED?)
                int nodeOffset = texRes * ((z * dim * texRes) + x);
                for (int r = 0; r < texRes; r++) {
                    for (int c = 0; c < texRes; c++) {
                        int color = 0;
                        if (node != null) {
                            // TODO USE SHADERS TO COLOR THE WAKES?
                            color = node.simulationNode.getPixelColor(c, r, fluidColor, lightCol, opacity);
                        }
                        int pixelOffset = (( r * dim * texRes) + c);
                        this.pixels.put(nodeOffset + pixelOffset, color);
                    }
                }
            }
        }
        hasPopulatedPixels = true;
    }
}
