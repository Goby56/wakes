package com.goby56.wakes.simulation;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.render.WakeQuad;
import com.goby56.wakes.render.WakeRenderer;
import com.goby56.wakes.render.WakeTexture;
import com.goby56.wakes.utils.WakesDebugInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class Brick {
    public int[] bitMask = new int[32];
    private final WakeNode[][] nodes;
    public final int capacity;
    public final int dim;

    public int occupied = 0;


    public final Vec3d pos;
    public final ArrayList<WakeQuad> quads;

    public Brick NORTH;
    public Brick EAST;
    public Brick SOUTH;
    public Brick WEST;

    public Brick(int x, float y, int z) {
        this.dim = 32;
        this.capacity = dim * dim;
        this.nodes = new WakeNode[dim][dim];
        this.pos = new Vec3d(x, y, z);
        this.quads = new ArrayList<>();
    }


    public boolean tick() {
        quads.clear();
        long tNode = System.nanoTime();
        for (int z = 0; z < dim; z++) {
            for (int x = 0; x < dim; x++) {
                if (this.get(x, z) == null) continue;

                if (!this.get(x, z).tick()) {
                    this.clear(x, z);
                }
            }
        }
        WakesDebugInfo.nodeLogicTime += (System.nanoTime() - tNode);
        if (occupied != 0) {
            long tMesh = System.nanoTime();
            generateMesh();
            WakesDebugInfo.meshGenerationTime += (System.nanoTime() - tMesh);
        }
        return occupied != 0;
    }

    public void query(Frustum frustum, ArrayList<WakeQuad> output) {
        for (WakeQuad quad : quads) {
            Box b = quad.toBox();
            if (frustum.isVisible(b)) output.add(quad);
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
                return WEST.nodes[Math.floorMod(z, dim)][x];
            } else if (x >= dim && EAST != null) {
                return EAST.nodes[Math.floorMod(z, dim)][x];
            }
        }
        return null;
    }

    public void insert(WakeNode node) {
        int x = Math.floorMod(node.x, dim), z = Math.floorMod(node.z, dim);
        // MinecraftClient.getInstance().player.sendMessage(Text.of(String.format("Placing Node(%d, %f, %d) at (%d, %d) in brick (%f, %f, %f)",
        //        node.x, node.height, node.z, x, z, pos.x, pos.y, pos.z)));
        for (WakeNode neighbor : getAdjacentNodes(x, z)) {
            neighbor.updateAdjacency(node);
        }
        this.set(x, z, node);
    }

    protected void set(int x, int z, WakeNode node) {
        boolean wasNull = nodes[z][x] == null;
        nodes[z][x] = node;
        if (node == null) {
            bitMask[x] &= ~(1 << (dim - z - 1));
            if (!wasNull) this.occupied--;
        } else {
            bitMask[x] |= (1 << (dim - z - 1));
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

    public void generateMesh() {
        var ints = bitMask;
        for (int i = 0; i < dim; i++) {
            int j = 0;
            while (j < dim) {
                j += Integer.numberOfTrailingZeros(ints[i] >>> j);
                if (j >= dim) continue;

                int h = Integer.numberOfTrailingZeros(~(ints[i] >>> j));

                int hm = (h == 32) ? ~0 : (1 << h) - 1;
                int mask = hm << j;

                int w = 1;
                while (i + w < dim) {
                    int nextH = (ints[i + w] >>> j) & hm;
                    if (nextH != hm) {
                        break;
                    }
                    ints[i + w] &= ~mask;
                    w++;
                }
                quads.add(new WakeQuad((int) (i + pos.x), (float) pos.y, (int) (dim - j - h + pos.z), w, h, getFromArea(i, dim - j - h, w, h)));
                j += h;
            }
        }
    }

    private WakeNode[][] getFromArea(int x, int z, int w, int h) {
        WakeNode[][] nodes = new WakeNode[h][w];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                WakeNode node = this.get(x + j, z + i);
                assert node != null;
                nodes[i][j] = node;
            }
        }
        return nodes;
    }
}