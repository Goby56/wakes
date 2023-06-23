package com.goby56.wakes.utils;

import net.minecraft.util.math.Vec3i;
import org.joml.Vector2i;

public class WakeNode implements Position {
    public final int[][] values = new int[16][16];

    public Vec3i position;

    public WakeNode(Vec3i position) {
        this.position = position;
    }

    @Override
    public int x() {
        return position.getX();
    }

    @Override
    public int z() {
        return position.getZ();
    }
}
