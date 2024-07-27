package com.goby56.wakes.simulation;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.render.WakeQuad;
import com.goby56.wakes.render.WakeRenderer;
import com.goby56.wakes.render.WakeTexture;
import com.goby56.wakes.utils.WakesDebugInfo;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;

public class Brick {
    public int[] bitMask = new int[32];
    private final WakeNode[][] nodes;
    public final int capacity;
    public final int dim;

    public int occupied = 0;

    private final int x;
    private final int z;

    public final ArrayList<WakeQuad> quads;


    public Brick(int x, int z) {
        this.dim = 32;
        this.capacity = dim * dim;
        this.nodes = new WakeNode[dim][dim];
        this.x = x;
        this.z = z;
        this.quads = new ArrayList<>();
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
                quads.add(new WakeQuad(i, dim - j - h, w, h, getFromArea(i, dim - j - h, w, h)));
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

    public boolean tick(World world) {
        quads.clear();
        for (int z = 0; z < dim; z++) {
            for (int x = 0; x < dim; x++) {
                if (this.get(x, z) == null) continue;

                long tNode = System.nanoTime();
                if (!this.get(x, z).tick()) {
                    this.clear(x, z);
                }
                WakesDebugInfo.nodeLogicTime += (System.nanoTime() - tNode);
            }
        }
        if (occupied != 0) {
            long tMesh = System.nanoTime();
            generateMesh();
            WakesDebugInfo.meshGenerationTime += (System.nanoTime() - tMesh);

            long tTexture = System.nanoTime();
            for (var quad : quads) {
                WakeTexture tex = WakeRenderer.wakeTextures.get(WakesClient.CONFIG_INSTANCE.wakeResolution);
                quad.populatePixels(tex, world);
            }
            WakesDebugInfo.texturingTime += (System.nanoTime() - tTexture);


        }
        return occupied != 0;
    }

    public Vec3d getPos() {
        return new Vec3d(this.x, 0, this.z);
    }

    public WakeNode get(int x, int z) {
        return nodes[z][x];
    }

    public void insert(WakeNode node) {
       this.set(Math.floorMod(node.x(), dim), Math.floorMod(node.z(), dim), node);
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

}