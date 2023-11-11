package com.goby56.wakes.utils;

public interface Age<T> {
    boolean isDead();
    void markDead();
    void revive(T newNode);
    void tick();
}
