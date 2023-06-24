package com.goby56.wakes.utils;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.joml.Vector2i;

import java.util.Objects;

public class WakeNode implements Position<WakeNode>, Age, Highlightable {
    public final int[][] values = new int[16][16];

    public final int x;
    public final int z;
    public float height;

    public WakeNode NORTH = null;
    public WakeNode EAST = null;
    public WakeNode SOUTH = null;
    public WakeNode WEST = null;

    private final int maxAge;
    private int age = 0;
    private boolean dead = false;

    public boolean highlighted = false;

    public WakeNode(Vec3d position, int maxAge) {
        this.x = (int) position.getX();
        this.z = (int) position.getZ();
        this.height = (float) position.getY();
        this.maxAge = maxAge;
    }

    @Override
    public void tick() {
        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }
        if (this.age < 3) {
            this.highlighted = false;
        }
        // TODO WAVE CALCULATIONS HERE
        // GET NEARBY NODES?
        // DO MATH
        // n*log(n)?
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
    public void setHighlight(boolean value) {
        this.highlighted = value;
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
    public void revive() {
        this.age = 0;
    }

    @Override
    public void markDead() {
        this.dead = true;
    }

    @Override
    public boolean isDead() {
        return this.dead;
    }

    public Vec3d getPos() {
        return new Vec3d(this.x, this.height, this.z);
    }
}
