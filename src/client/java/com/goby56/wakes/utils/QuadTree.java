package com.goby56.wakes.utils;

import java.util.ArrayList;
import java.util.Stack;

public class QuadTree<T extends Position & Age & Highlightable> {
    private static final int CAPACITY = 64;

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

    public void tick() {
        Stack<Integer> indicesToDelete = new Stack<>();
        int i = 0;
        for (T node : nodes) {
            if (!node.isDead()) {
                node.tick();
            } else {
                indicesToDelete.add(i);
            }
            i++;
        }
        for (i = 0; i < indicesToDelete.size(); i++) {
            this.nodes.remove((int) indicesToDelete.pop());
        }

        if (this.NE == null) {
            return;
        }

        this.NE.tick();
        this.NW.tick();
        this.SW.tick();
        this.SE.tick();
    }

    private void tryAdd(T node) {
        ArrayList<T> nodesNearby = new ArrayList<>();
        this.query(new AABB(node.x(), node.z(), 1), nodesNearby);

        boolean nodeAlreadyExists = false;
        for (T otherNode : nodesNearby) {
            otherNode.setHighlight(true); // For debugging
            if (node.equals(otherNode)) {
                nodeAlreadyExists = true;
                otherNode.revive();
            }
        }

        if (!nodeAlreadyExists) {
            this.nodes.add(node);
            nodesNearby.forEach(node::updateAdjacency);
        }
    }

    public boolean insert(T node) {
        if (!this.bounds.contains(node.x(), node.z())) {
            return false;
        }

        if (this.nodes.size() < CAPACITY) {
            this.tryAdd(node);
            return true;
        }

        if (this.NE == null) {
            this.subdivide();
        }

        if (this.NE.insert(node)) return true;
        if (this.NW.insert(node)) return true;
        if (this.SW.insert(node)) return true;
        return this.SE.insert(node);
    }

    public void query(AABB range, ArrayList<T> output) {
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
        // TODO FIX IF WIDTH BECOMES SMALLER THAN 1
        this.NE = new QuadTree<>(x + w, z - w, w, depth + 1);
        this.NW = new QuadTree<>(x - w, z - w, w, depth + 1);
        this.SW = new QuadTree<>(x - w, z + w, w, depth + 1);
        this.SE = new QuadTree<>(x + w, z + w, w, depth + 1);
    }

    public record AABB(int x, int z, int width) {

        public boolean contains(int x, int z) {
            return this.x - this.width <= x && x <= this.x + this.width &&
                    this.z - this.width <= z && z <= this.z + this.width;
        }

        public boolean intersects(AABB other) {
            return !(this.x - this.width > other.x + other.width ||
                    this.x + this.width < other.x - other.width ||
                    this.z - this.width > other.z + other.width ||
                    this.z + this.width < other.z - other.width);
        }
    }
}
