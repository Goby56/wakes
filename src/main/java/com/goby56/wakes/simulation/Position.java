package com.goby56.wakes.simulation;

import net.minecraft.util.math.Box;

public interface Position<T> {
    int x();
    int z();

    boolean inValidPos();

    Box toBox();

    void updateAdjacency(T node);
}
