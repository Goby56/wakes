package com.goby56.wakes.utils;

import net.minecraft.util.math.Box;

public interface Position<T> {
    int x();
    int z();

    Box toBox();

    void updateAdjacency(T node);
    void setNorth(T north);
    void setEast(T east);
    void setSouth(T south);
    void setWest(T west);
}
