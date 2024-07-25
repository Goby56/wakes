package com.goby56.wakes.simulation;

public interface Age<T> {
    boolean isDead();
    void markDead();
    void revive(T newNode);
    boolean tick();
}
