package com.goby56.wakes.simulation;

import java.util.concurrent.ConcurrentLinkedQueue;

public class QueueSet<T> extends ConcurrentLinkedQueue<T> {
    @Override
    public boolean add(T t) {
        if (contains(t)) return false;
        return super.add(t);
    }
}
