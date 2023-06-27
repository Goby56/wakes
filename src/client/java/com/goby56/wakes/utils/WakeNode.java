package com.goby56.wakes.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2i;

import java.util.Objects;

public class WakeNode implements Position<WakeNode>, Age<WakeNode> {
    private final WakeHandler wakeHandler = WakeHandler.getInstance();

    public float[][][] u = new float[3][18][18];
    public final float c = 0.85f; // blocks per second kind of
    public final float initialStrength = 200f;
    private final float alpha = (float) Math.pow(c * 16f/20f, 2);
    public float t = 0;
    public int floodLevel;

    public final int x;
    public final int z;
    public float height;
    public Vector2i subPos;

    public WakeNode NORTH = null;
    public WakeNode EAST = null;
    public WakeNode SOUTH = null;
    public WakeNode WEST = null;

    // TODO MAKE DISAPPEARANCE DEPENDENT ON WAVE VALUES INSTEAD OF AGE/TIME
    private final int maxAge = 60;
    private int age = 4;
    private boolean dead = false;

    public WakeNode(Vec3d position) {
        this.x = (int) Math.floor(position.x);
        this.z = (int) Math.floor(position.z);
        this.height = (float) position.getY();
        int sx = (int) Math.floor(16 * (position.x - this.x));
        int sz = (int) Math.floor(16 * (position.z - this.z));
        this.subPos = new Vector2i(sx, sz);
        for (int z = -1; z < 2; z++) {
            for (int x = -1; x < 2; x++) {
                this.u[0][sz+1+z][sx+1+x] = this.initialStrength;
            }
        }
        this.floodLevel = 6;
    }

    public WakeNode(int x, int z, float height, int floodLevel) {
        this.x = x;
        this.z = z;
        this.height = height;
        this.floodLevel = floodLevel;
    }

    @Override
    public void tick() {
        this.t += 1/20f;
        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }

        for (int i = 2; i >= 1; i--) {
            if (this.NORTH != null) this.u[i][0] = this.NORTH.u[i][16];
            if (this.SOUTH != null) this.u[i][17] = this.SOUTH.u[i][1];
            for (int z = 0; z < 18; z++) {
                if (this.EAST == null && this.WEST == null) break;
                if (this.EAST != null) this.u[i][z][17] = this.EAST.u[i][z][1];
                if (this.WEST != null) this.u[i][z][0] = this.WEST.u[i][z][16];
            }
        }

        for (int z = 1; z < 17; z++) {
            for (int x = 1; x < 17; x++) {
                this.u[2][z][x] = this.u[1][z][x];
                this.u[1][z][x] = this.u[0][z][x];
            }
        }
//        this.u[2] = this.u[1].;
//        this.u[1] = this.u[0].clone();
//        this.u[0] = new float[18][18];

        double largest = 0;
        for (int x = 1; x < 17; x++) {
            for (int z = 1; z < 17; z++) {
//                this.u[0][z][x] = (float) (alpha * (0.5*u[1][z-1][x] + 0.25*u[1][z-1][x+1] + 0.5*u[1][z][x+1]
//                                                 + 0.25*u[1][z+1][x+1] + 0.5*u[1][z+1][x] + 0.25*u[1][z+1][x-1]
//                                                 + 0.5*u[1][z][x-1] + 0.25*u[1][z-1][x-1])
//                                                 + 2*u[1][z][x] - u[2][z][x]);
                this.u[0][x][z] = alpha * (u[1][x-1][z] + u[1][x+1][z] + u[1][x][z-1] + u[1][x][z+1] - 4*u[1][x][z]) + 2*u[1][x][z] - u[2][x][z];
            }
        }


        if (this.floodLevel > 0 && this.t > 0.5) {
            if (this.NORTH == null) {
                wakeHandler.insert(new WakeNode(this.x, this.z - 1, this.height, this.floodLevel - 1));
            } else {
                this.NORTH.updateFloodLevel(this.floodLevel - 1);
            }
            if (this.EAST == null) {
                wakeHandler.insert(new WakeNode(this.x + 1, this.z, this.height, this.floodLevel - 1));
            } else {
                this.EAST.updateFloodLevel(this.floodLevel - 1);
            }
            if (this.SOUTH == null) {
                wakeHandler.insert(new WakeNode(this.x, this.z + 1, this.height, this.floodLevel - 1));
            } else {
                this.SOUTH.updateFloodLevel(this.floodLevel - 1);
            }
            if (this.WEST == null) {
                wakeHandler.insert(new WakeNode(this.x - 1, this.z, this.height, this.floodLevel - 1));
            } else {
                this.WEST.updateFloodLevel(this.floodLevel - 1);
            }
            this.floodLevel = 0;
            // TODO IF BLOCK IS BROKEN (AND WATER APPEARS IN ITS STEAD) RETRY FLOOD FILL
        }
    }

    @Override
    public void updateAdjacency(WakeNode node) {
        if (node.x == this.x && node.z == this.z - 1) {
            this.NORTH = node;
            node.SOUTH = this;
            return;
        }
        if (node.x == this.x + 1 && node.z == this.z) {
            this.EAST = node;
            node.WEST = this;
            return;
        }
        if (node.x == this.x && node.z == this.z + 1) {
            this.SOUTH = node;
            node.NORTH = this;
            return;
        }
        if (node.x == this.x - 1 && node.z == this.z) {
            this.WEST = node;
            node.EAST = this;
        }
    }

    public void updateFloodLevel(int newLevel) {
        this.age = 0;
        if (newLevel > this.floodLevel) {
            this.floodLevel = newLevel;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WakeNode wakeNode = (WakeNode) o;
        return x == wakeNode.x && z == wakeNode.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    @Override
    public Box toBox() {
        return new Box(this.x, Math.floor(this.height), this.z, this.x + 1, Math.ceil(this.height), this.z + 1);
    }

    @Override
    public int x() {
        return this.x;
    }

    @Override
    public int z() {
        return this.z;
    }

    @Override
    public void revive(WakeNode node) {
        this.age = 0;
        if (node.subPos != null) {
            this.floodLevel = 8;
            int sx = node.subPos.x;
            int sz = node.subPos.y;
            for (int z = -1; z < 2; z++) {
                for (int x = -1; x < 2; x++) {
                    this.u[0][sz+1+z][sx+1+x] = this.initialStrength;
                }
            }
            this.subPos = node.subPos;
        }
    }

    @Override
    public void markDead() {
        this.dead = true;
    }

    @Override
    public boolean isDead() {
        return this.dead;
    }

    @Override
    public boolean inValidPos() {
        FluidState fluidState = MinecraftClient.getInstance().world.getFluidState(new BlockPos(this.x, (int) this.height, this.z));
        return fluidState.isIn(FluidTags.WATER);
    }

    public Vec3d getPos() {
        return new Vec3d(this.x, this.height, this.z);
    }
}
