package com.goby56.wakes.simulation;

import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.*;

public class QuadTree {
    private static final int MAX_DEPTH = 20;
    private static final int ROOT_X = (int) - Math.pow(2, 25);
    private static final int ROOT_Z = (int) - Math.pow(2, 25);
    private static final int ROOT_WIDTH = (int) Math.pow(2, 26);

    private final QuadTree ROOT;
    private final QuadTree PARENT;
    private List<QuadTree> children;

    private final DecentralizedBounds bounds;
    private final int depth;
    private Brick brick;

    public QuadTree() {
        this(ROOT_X, ROOT_Z, ROOT_WIDTH, 0, null, null);
    }

    private QuadTree(int x, int z, int width, int depth, QuadTree root, QuadTree parent) {
        this.bounds = new DecentralizedBounds(x, z, width);
        this.depth = depth;
        if (depth >= MAX_DEPTH) {
            assert bounds.width() == 32;
            this.brick = new Brick(x, z);
        }
        this.ROOT = root == null ? this : root;
        this.PARENT = parent;
    }

    public boolean tick(World world) {
        if (brick != null) {
            return brick.tick(world);
        }
        if (children == null) return false;
        int aliveChildren = 0;
        for (var tree : children) {
            if (tree.tick(world)) aliveChildren++;
        }
        if (aliveChildren == 0) this.prune();
        return aliveChildren > 0;
    }

    public boolean insert(WakeNode node) {
        if (!this.bounds.contains(node.x(), node.z())) {
            return false;
        }

        if (this.brick != null) {
            brick.insert(node);
            return true;
        }

        if (children == null) this.subdivide();
        for (var tree : children) {
           if (tree.insert(node)) return true;
        }
        return false;
    }

    public void query(Frustum frustum, int y, ArrayList<Brick> output) {
        if (!frustum.isVisible(this.bounds.toBox(y))) {
            return;
        }
        if (brick != null) {
            output.add(brick);
            return;
        }
        if (children == null) return;
        for (var tree : children) {
            tree.query(frustum, y, output);
        }
    }

    private void subdivide() {
        if (brick != null) return;
        int x = this.bounds.x;
        int z = this.bounds.z;
        int w = this.bounds.width >> 1;
        children = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            children.add(i, new QuadTree());
        }
        children.add(0, new QuadTree(x, z, w, depth + 1, this.ROOT, this)); // NW
        children.add(1, new QuadTree(x + w, z, w, depth + 1, this.ROOT, this)); // NE
        children.add(2, new QuadTree(x, z + w, w, depth + 1, this.ROOT, this)); // SW
        children.add(3, new QuadTree(x + w, z + w, w, depth + 1, this.ROOT, this)); // SE
    }

    public int count() {
        if (brick != null) {
           return brick.occupied;
        }
        if (children == null) return 0;
        return children.stream().reduce(0, (tot, tree) -> tot + tree.count(), Integer::sum);
    }

    public void prune() {
        if (children != null) {
            for (var tree : children) {
                tree.prune();
            }
            children.set(0, null);
            children.set(1, null);
            children.set(2, null);
            children.set(3, null);
        }
        children = null;
    }

    public void getBrickBBs(ArrayList<DebugBB> bbs, int height) {
        if (brick != null) {
            bbs.add(new DebugBB(bounds.toDebugBox(height), depth));
        }
        if (children == null) return;
        for (var tree : children) {
            tree.getBrickBBs(bbs, height);
        }
    }

    public record DebugBB(Box bb, int depth) {
    }

    public record DecentralizedBounds(int x, int z, int width) {
        public boolean contains(int x, int z) {
            return this.x <= x && x <= this.x + this.width &&
                    this.z <= z && z <= this.z + this.width;
        }

        public boolean intersects(DecentralizedBounds other) {
            return !(this.x > other.x + other.width ||
                    this.x + this.width < other.x ||
                    this.z > other.z + other.width ||
                    this.z + this.width < other.z);
        }

        public Box toBox(int y) {
            return new Box(this.x, y - 0.5, this.z,
                    this.x + this.width, y + 0.5, this.z + this.width);
        }

        public Box toDebugBox(int y) {
            return new Box(this.x + 0.1, y - 1.2, this.z + 0.1,
                    this.x + this.width - 0.1, y - 1.23, this.z + this.width - 0.1);
        }

    }
}
