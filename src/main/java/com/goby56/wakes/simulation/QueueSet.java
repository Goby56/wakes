package com.goby56.wakes.simulation;

import java.util.Iterator;
import java.util.LinkedHashSet;

public class QueueSet<T> {
    private final LinkedHashSet<T> set = new LinkedHashSet<>();

    public boolean add(T t) {
        return set.add(t);
    }

    public T peek() {
        Iterator<T> it = set.iterator();
        return it.hasNext() ? it.next() : null;
    }

    public T poll() {
        Iterator<T> it = set.iterator();
        if (!it.hasNext()) return null;
        T t = it.next();
        it.remove();
        return t;
    }

    public void clear() {
        set.clear();
    }
}
