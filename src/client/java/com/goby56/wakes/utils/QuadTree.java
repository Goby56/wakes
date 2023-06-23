package com.goby56.wakes.utils;


import org.joml.Vector2i;

import java.util.ArrayList;

public class QuadTree<T extends Position> {
    private static final int CAPACITY = 4;

    private QuadTree<T> NE;
    private QuadTree<T> NW;
    private QuadTree<T> SW;
    private QuadTree<T> SE;

    private final AABB bounds;
    private final int depth;
    private final ArrayList<T> nodes = new ArrayList<>(CAPACITY);

    public QuadTree(int x, int z, int width, int depth) {

        this.bounds = new AABB(x, z, width);
        this.depth = depth;
    }

    public boolean insert(T node) {
        if (!this.bounds.contains(node.x(), node.z())) {
            return false;
        }

        if (this.nodes.size() < CAPACITY) {
            this.nodes.add(node);
            System.out.printf("inserting node (%d, %d) at depth %d\n", node.x(), node.z(), this.depth);
            return true;
        }

        if (this.NE == null) {
            this.subdivide();
        }

        if (this.NE.insert(node)) return true;
        if (this.NW.insert(node)) return true;
        if (this.SW.insert(node)) return true;
        if (this.SE.insert(node)) return true;

        return false;
    }

    public void query(AABB range, ArrayList<T> output) {
//        System.out.println("Looking in range: " + range);
        if (!this.bounds.intersects(range)) {
            return;
        }
        for (T node : this.nodes) {
            if (range.contains(node.x(), node.z())) {
                output.add(node);
            }
        }
        if (this.NE == null) {
            return;
        }
        this.NE.query(range, output);
        this.NW.query(range, output);
        this.SW.query(range, output);
        this.SE.query(range, output);
    }

    private void subdivide() {
        int x = this.bounds.x;
        int z = this.bounds.z;
        int w = this.bounds.width >> 1;
        this.NE = new QuadTree<>(x + w, z - w, w, depth + 1);
        this.NW = new QuadTree<>(x - w, z - w, w, depth + 1);
        this.SW = new QuadTree<>(x - w, z + w, w, depth + 1);
        this.SE = new QuadTree<>(x + w, z + w, w, depth + 1);
    }

    public record AABB(int x, int z, int width) {

        public boolean contains(int x, int z) {
            return this.x - this.width <= x && x < this.x + this.width &&
                    this.z - this.width <= z && z < this.z + this.width;
        }

        public boolean intersects(AABB other) {
            return !(this.x - this.width > other.x + other.width ||
                    this.x + this.width < other.x - other.width ||
                    this.z - this.width > other.z + other.width ||
                    this.z + this.width < other.z - other.width);
        }
    }
}
