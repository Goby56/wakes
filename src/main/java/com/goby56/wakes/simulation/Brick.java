package com.goby56.wakes.simulation;

import net.minecraft.util.math.Vec2f;

import java.util.*;

public class Brick<T extends Position<T> & Age<T>> implements Iterable<T> {
    private final ArrayList<T> nodes;
    public final int capacity;
    private final int dim;

    public int occupied = 0;

    public Brick(int width) {
        this.dim = width;
        this.capacity = dim * dim;
        this.nodes = new ArrayList<>(capacity);
    }

    public boolean tick() {
        return true;
    }

    public T get_global(int x, int z) {
        return this.get(x % this.dim, z % this.dim);
    }

    public T get(int x, int z) {
        return this.get(dim * z + x);
    }

    protected T get(int i) {
        return this.nodes.get(i);
    }

    public void insert(T node) {
       this.set(node.x() % this.dim, node.z() % this.dim, node);
    }
    protected void set(int x, int z, T node) {
        this.set(dim * z + x, node);
    }

    protected void set(int i, T node) {
        this.nodes.set(i, node);
        if (node != null) this.occupied++;
    }

    public void clear(int x, int z) {
        this.clear(dim * z + x);
    }

    public void clear(int i) {
        this.nodes.set(i, null);
        this.occupied--;
    }

    public void clear() {
        for (int i = 0; i < this.nodes.size(); i++) {
            this.clear(i);
        }
        this.occupied = 0;
    }

    public boolean isEmpty() {
        return nodes.stream().allMatch(Objects::isNull);
    }

    @Override
    public Iterator<T> iterator() {
        return new BrickIterator<>(this);
    }
}

class BrickIterator<T extends Position<T> & Age<T>> implements Iterator<T> {
    Brick<T> brick;
    int index;
    int found;

    public BrickIterator(Brick<T> brick) {
        this.brick = brick;
        this.index = 0;
        this.found = 0;
    }

    @Override
    public boolean hasNext() {
        return found < brick.occupied;
    }

    @Override
    public T next() {
        if (!this.hasNext()) throw new NoSuchElementException("Brick has no more nodes");
        T next = brick.get(index++);
        if (next == null) {
            return this.next();
        } else {
            found++;
            return next;
        }
    }
}
