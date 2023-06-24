package com.goby56.wakes.utils;

public interface Position<T> {
    int x();
    int z();

    void updateAdjacency(T node);
    void setNorth(T north);
    void setEast(T east);
    void setSouth(T south);
    void setWest(T west);
}
