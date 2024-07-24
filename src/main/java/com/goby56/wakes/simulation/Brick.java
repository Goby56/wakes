package com.goby56.wakes.simulation;

import net.minecraft.util.math.Vec3d;

public class Brick {
    public int[] bitMask = new int[32];
    private final WakeNode[][] nodes;
    public final int capacity;
    public final int dim;

    public int occupied = 0;

    private final int x;
    private final int z;

    public Brick(int x, int z) {
        this.dim = 32;
        this.capacity = dim * dim;
        this.nodes = new WakeNode[dim][dim];
        this.x = x;
        this.z = z;
    }

    public boolean tick() {
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                if (nodes[i][j] == null) continue;
                nodes[i][j].tick();
            }
        }
        return occupied != 0;
    }

    public Vec3d getPos() {
        return new Vec3d(this.x, 0, this.z);
    }

    public WakeNode get(int x, int z) {
        return nodes[x][z];
    }

    public void insert(WakeNode node) {
       this.set(Math.floorMod(node.x(), dim), Math.floorMod(node.z(), dim), node);
    }

    protected void set(int x, int z, WakeNode node) {
        boolean wasNull = nodes[x][z] == null;
        nodes[x][z] = node;
        if (node == null) {
            bitMask[x] &= ~(1 << z);
            if (!wasNull) this.occupied--;
        } else {
            bitMask[x] |= (1 << z);
            if (wasNull) this.occupied++;
        }
    }

    public void clear(int x, int z) {
        this.set(x, z, null);
    }

}