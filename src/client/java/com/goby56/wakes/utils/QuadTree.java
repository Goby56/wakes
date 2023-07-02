package com.goby56.wakes.utils;

import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.Stack;

public class QuadTree<T extends Position<T> & Age<T>> {
    private static final int CAPACITY = 64;

    private final QuadTree<T> ROOT;
    private QuadTree<T> NE;
    private QuadTree<T> NW;
    private QuadTree<T> SW;
    private QuadTree<T> SE;

    private final AABB bounds;
    private final int depth;
    private final ArrayList<T> nodes = new ArrayList<>(CAPACITY);

    public QuadTree(int x, int z, int width) {
        this(x, z, width, 0, null);
    }

    private QuadTree(int x, int z, int width, int depth, QuadTree<T> root) {
        this.bounds = new AABB(x, z, width);
        this.depth = depth;
        this.ROOT = root == null ? this : root;
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
        if (!node.inValidPos()) {
            return;
        }
        ArrayList<T> nodesNearby = new ArrayList<>();
        this.ROOT.query(new AABB(node.x(), node.z(), 1), nodesNearby);

        boolean nodeAlreadyExists = false;
        for (T otherNode : nodesNearby) {
            if (node.equals(otherNode)) {
                nodeAlreadyExists = true;
                otherNode.revive(node);
            }
        }

        if (!nodeAlreadyExists) {
            this.nodes.add(node);
            nodesNearby.forEach(node::updateAdjacency);
        }
    }

    public boolean insert(T node) {
        // TODO DUPLICATE INSERTIONS MAY STILL BE PRESENT. FIX
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

    public void query(Circle range, ArrayList<T> output) {
        // TODO UNIFY query(AABB) AND query(Circle) TO ONE METHOD
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

    public void query(Frustum frustum, int y, ArrayList<T> output) {
        if (!frustum.isVisible(this.bounds.toBox(y))) {
            return;
        }
        for (T node : this.nodes) {
            if (frustum.isVisible(node.toBox())) {
                output.add(node);
            }
        }
        if (this.NE == null) {
            return;
        }
        this.NE.query(frustum, y, output);
        this.NW.query(frustum, y, output);
        this.SW.query(frustum, y, output);
        this.SE.query(frustum, y, output);
    }

    private void subdivide() {
        int x = this.bounds.x;
        int z = this.bounds.z;
        int w = this.bounds.width >> 1;
        // TODO FIX IF WIDTH BECOMES SMALLER THAN 1 (POTENTIALLY)
        this.NE = new QuadTree<>(x + w, z - w, w, depth + 1, this.ROOT);
        this.NW = new QuadTree<>(x - w, z - w, w, depth + 1, this.ROOT);
        this.SW = new QuadTree<>(x - w, z + w, w, depth + 1, this.ROOT);
        this.SE = new QuadTree<>(x + w, z + w, w, depth + 1, this.ROOT);
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

        public boolean intersects(Circle other) {
            if (this.contains(other.x, other.z)) return true;
            return !(this.x - this.width > other.x + other.radius ||
                    this.x + this.width < other.x - other.radius ||
                    this.z - this.width > other.z + other.radius ||
                    this.z + this.width < other.z - other.radius);
        }

        public Box toBox(int y) {
            return new Box(this.x - this.width, y, this.z - this.width,
                           this.x + this.width, y + 1, this.z + this.width);
        }
    }

    public record Circle(int x, int z, int radius) {
        public boolean contains(int x, int z) {
            return Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.z - z, 2)) <= radius;
        }

    }
}
