package com.goby56.wakes.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2i;

import java.util.Objects;

public class WakeNode implements Position<WakeNode>, Age<WakeNode> {
    private final WakeHandler wakeHandler = WakeHandler.getInstance();

    public float[][][] u = new float[3][18][18];
    public final float c = 1f; // blocks per second
    public final float initialStrength = 1f;
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
        this.u[0][sz+1][sx+1] = this.initialStrength;
        this.floodLevel = 8;
    }

    public WakeNode(int x, int z, float height, int floodLevel) {
        this.x = x;
        this.z = z;
        this.height = height;
        this.floodLevel = floodLevel;
    }

    @Override
    public void preTick() {
        this.t += 1/20f;
        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }
        this.u[2] = this.u[1];
        this.u[1] = this.u[0];
    }

    @Override
    public void tick() {
        for (int i = 2; i > 0; i--) {
            if (this.NORTH != null) this.u[i][0] = this.NORTH.u[i][16];
            if (this.SOUTH != null) this.u[i][17] = this.SOUTH.u[i][1];
            for (int z = 1; z < 17; z++) {
                if (this.EAST == null && this.WEST == null) break;
                if (this.EAST != null) this.u[i][z][17] = this.EAST.u[i][z][1];
                if (this.WEST != null) this.u[i][z][0] = this.WEST.u[i][z][16];
            }
        }
        for (int z = 1; z < 17; z++) {
            for (int x = 1; x < 17; x++) {
                this.u[0][z][x] = alpha * (u[1][z][x-1] + u[1][z][x+1] + u[1][z-1][x] + u[1][z+1][x] - 4*u[1][z][x]) + 2*u[1][z][x] - u[2][z][x];
                this.u[0][z][x] *= 0.95;
            }
        }

        if (this.floodLevel > 0) {
            if (this.NORTH == null) {
                wakeHandler.insert(new WakeNode(this.x, this.z - 1, this.height, this.floodLevel - 1));
            }
            if (this.SOUTH == null) {
                wakeHandler.insert(new WakeNode(this.x, this.z + 1, this.height, this.floodLevel - 1));
            }
            if (this.EAST == null) {
                wakeHandler.insert(new WakeNode(this.x - 1, this.z, this.height, this.floodLevel - 1));
            }
            if (this.WEST == null) {
                wakeHandler.insert(new WakeNode(this.x + 1, this.z, this.height, this.floodLevel - 1));
            }
            this.floodLevel = 0;
            // TODO IF BLOCK IS BROKEN (AND WATER APPEARS IN ITS STEAD) RETRY FLOOD FILL
        }
    }

    @Override
    public void updateAdjacency(WakeNode node) {
        if (node.x == this.x && node.z == this.z - 1) {
            this.NORTH = node;
            node.setSouth(this);
            return;
        }
        if (node.x == this.x + 1 && node.z == this.z) {
            this.EAST = node;
            node.setWest(this);
            return;
        }
        if (node.x == this.x && node.z == this.z + 1) {
            this.SOUTH = node;
            node.setNorth(this);
            return;
        }
        if (node.x == this.x - 1 && node.z == this.z) {
            node.setEast(this);
            this.WEST = node;
        }
    }

    @Override
    public void setNorth(WakeNode north) {
        this.NORTH = north;
    }

    @Override
    public void setEast(WakeNode east) {
        this.EAST = east;
    }

    @Override
    public void setSouth(WakeNode south) {
        this.SOUTH = south;
    }

    @Override
    public void setWest(WakeNode west) {
        this.WEST = west;
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
            int sx = node.subPos.x;
            int sz = node.subPos.y;
            this.u[0][sz+1][sx+1] = this.initialStrength;
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
