package com.goby56.wakes.simulation;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.debug.WakesDebugInfo;
import com.goby56.wakes.render.FrustumManager;
import com.goby56.wakes.utils.WakesUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.system.MemoryUtil;

import java.util.*;
import java.util.stream.Stream;

public class WakeChunk {
    private final WakeHandler wakeHandler;

    public static final int WIDTH = 16;
    private final WakeNode[][] nodes;
    public final int capacity;

    public int occupied = 0;

    public final Vec3 pos;
    public final WakeChunkPos chunkPos;
    public final AABB boundingBox;

    public final Map<WakeChunkPos.Direction, WakeChunk> neighbors;

    public long imgPtr = -1;
    public int texRes;
    public boolean hasPopulatedPixels = false;


    public WakeChunk(WakeChunkPos chunkPos, WakeHandler wakeHandler) {
        this.capacity = WIDTH * WIDTH;
        this.nodes = new WakeNode[WIDTH][WIDTH];
        this.chunkPos = chunkPos;
        this.pos = new Vec3(chunkPos.cx() * WIDTH, chunkPos.y(), chunkPos.cz() * WIDTH);
        this.boundingBox = new AABB(pos.x, pos.y, pos.z, pos.x + WIDTH, pos.y + 1, pos.z + WIDTH);
        this.neighbors = new HashMap<>();
        this.wakeHandler = wakeHandler;

        initTexture(WakeHandler.resolution.res);
    }

    public void initTexture(int res) {
        long size = 4L * WIDTH * WIDTH * res * res;
        if (imgPtr == -1) {
            this.imgPtr = MemoryUtil.nmemAlloc(size);
        } else {
            this.imgPtr = MemoryUtil.nmemRealloc(imgPtr, size);
        }
        this.texRes = res;
        this.hasPopulatedPixels = false;
    }

    public void deallocTexture() {
        MemoryUtil.nmemFree(imgPtr);
    }

    public boolean tick() {
        long tNode = System.nanoTime();
        for (int z = 0; z < WIDTH; z++) {
            for (int x = 0; x < WIDTH; x++) {
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

    public void query(ArrayList<WakeNode> output) {
        for (int z = 0; z < WIDTH; z++) {
            for (int x = 0; x < WIDTH; x++) {
                var node = this.get(x, z);
                if (node == null) continue;
                AABB b = node.toBox();
                if (FrustumManager.isVisible(b)) output.add(node);
            }
        }
    }

    public WakeNode get(int x, int z) {
        if (x >= 0 && x < WIDTH) {
            if (z < 0) {
                return getNeighbor(WakeChunkPos.Direction.NORTH).map(
                        wakeChunk -> wakeChunk.nodes[Math.floorMod(z, WIDTH)][x]).orElse(null);
            } else if (z >= WIDTH) {
                return getNeighbor(WakeChunkPos.Direction.SOUTH).map(
                        wakeChunk -> wakeChunk.nodes[Math.floorMod(z, WIDTH)][x]).orElse(null);
            } else {
                return nodes[z][x];
            }
        }
        if (z >= 0 && z < WIDTH) {
            if (x < 0) {
                return getNeighbor(WakeChunkPos.Direction.WEST).map(
                        wakeChunk -> wakeChunk.nodes[z][Math.floorMod(x, WIDTH)]).orElse(null);
            } else {
                return getNeighbor(WakeChunkPos.Direction.EAST).map(
                        wakeChunk -> wakeChunk.nodes[z][Math.floorMod(x, WIDTH)]).orElse(null);
            }
        }
        return null;
    }

    private Optional<WakeChunk> getNeighbor(WakeChunkPos.Direction direction) {
        if (neighbors.get(direction) == null) {
            neighbors.put(direction, wakeHandler.getChunk(chunkPos.offset(direction)));
        }
        return Optional.ofNullable(neighbors.get(direction));
    }

    public void insert(WakeNode node) {
        int x = Math.floorMod(node.x, WIDTH), z = Math.floorMod(node.z, WIDTH);
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

    public void populatePixels() {
        Level world = Minecraft.getInstance().level;
        for (int z = 0; z < WIDTH; z++) {
            for (int x = 0; x < WIDTH; x++) {
                WakeNode node = this.get(x, z);
                int lightCol = LightTexture.FULL_BRIGHT;
                int fluidColor = 0;
                float opacity = 0;
                if (node != null) {
                    fluidColor = BiomeColors.getAverageWaterColor(world, node.blockPos());
                    lightCol = WakesUtils.getLightColor(world, node.blockPos());
                    // TODO LERP LIGHT FROM SURROUNDING BLOCKS
                    opacity = (float) ((-Math.pow(node.t, 2) + 1) * WakesConfig.wakeOpacity);
                }

                // TODO MASS SET PIXELS TO NO COLOR IF NODE DOESNT EXIST (NEED TO REORDER PIXELS STORED?)
                long nodeOffset = texRes * 4L * (((long) z * WIDTH * texRes) + (long) x);
                for (int r = 0; r < texRes; r++) {
                    for (int c = 0; c < texRes; c++) {
                        int color = 0;
                        if (node != null) {
                            // TODO USE SHADERS TO COLOR THE WAKES?
                            color = node.simulationNode.getPixelColor(c, r, fluidColor, lightCol, opacity);
                        }
                        long pixelOffset = 4L * (((long) r * WIDTH * texRes) + c);
                        MemoryUtil.memPutInt(imgPtr + nodeOffset + pixelOffset, color);
                    }
                }
            }
        }
        hasPopulatedPixels = true;
    }
}
