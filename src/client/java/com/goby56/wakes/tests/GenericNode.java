package com.goby56.wakes.tests;

import com.goby56.wakes.utils.Age;
import com.goby56.wakes.utils.Position;
import com.goby56.wakes.utils.WakeNode;
import net.minecraft.util.math.Box;

import java.util.Objects;

public class GenericNode implements Position<GenericNode>, Age<GenericNode> {

    private final int x;
    private final int z;
    private final float height;
    private final int floodLevel;

    public GenericNode(int x, int z, float height, int floodLevel) {
        this.x = x;
        this.z = z;
        this.height = height;
        this.floodLevel = floodLevel;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenericNode genericNode = (GenericNode) o;
        return x == genericNode.x && z == genericNode.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    @Override
    public boolean isDead() {
        return false;
    }

    @Override
    public void markDead() {

    }

    @Override
    public void revive(GenericNode newNode) {

    }

    @Override
    public void preTick() {

    }

    @Override
    public void tick() {

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
    public boolean inValidPos() {
        return true;
    }

    @Override
    public Box toBox() {
        return null;
    }

    @Override
    public void updateAdjacency(GenericNode node) {

    }
}
