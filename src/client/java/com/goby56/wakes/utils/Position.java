package com.goby56.wakes.utils;

import net.minecraft.util.math.Box;

public interface Position<T> {
    int x();
    int z();

    boolean inValidPos();

    Box toBox();

    void updateAdjacency(T node);
}
