package com.goby56.wakes.utils;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.joml.Vector2i;

import java.util.Objects;

public class WakeNode implements Position, Age, Tickable {
    public final int[][] values = new int[16][16];

    public final int x;
    public final int z;
    public float height;

    private final int maxAge;
    private int age = 0;
    private boolean dead = false;

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
        // TODO WAVE CALCULATIONS HERE
        // GET NEARBY NODES?
        // DO MATH
        // n*log(n)?
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
