package com.goby56.wakes.utils;

import com.goby56.wakes.WakesClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.joml.Vector2i;

import java.util.Objects;
import java.util.Random;

public class WakeNode implements Position<WakeNode>, Age, Highlightable {
    public double[][] values = new double[16][16];
    public int sources = 0;
    public int MAX_QUERY_RANGE = 10;

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
        this.x = (int) Math.floor(position.x);
        this.z = (int) Math.floor(position.z);
        this.height = (float) position.getY();
        this.maxAge = maxAge;
        Random r = new Random();
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                this.values[i][j] = r.nextInt(0xFFFFFF);
            }
        }
    }

    @Override
    public void tick() {
        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }
        if (this.age < 10) {
            this.highlighted = false;
        }

//        for (int i = 0; i < 16; i++) {
//            for (int j = 0; j < 16; j++) {
//                this.image.setColor(i, j, 1);
//            }
//        }
//        MinecraftClient.getInstance().getTextureManager().registerTexture(this.textureID, new NativeImageBackedTexture(this.image));
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
