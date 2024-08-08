package com.goby56.wakes.simulation;

import com.goby56.wakes.render.WakeQuad;
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
    private final float yLevel;

    public QuadTree(float y) {
        this(ROOT_X, y, ROOT_Z, ROOT_WIDTH, 0, null, null);
    }

    private QuadTree(int x, float y, int z, int width, int depth, QuadTree root, QuadTree parent) {
        this.bounds = new DecentralizedBounds(x, y, z, width);
        this.depth = depth;
        this.ROOT = root == null ? this : root;
        this.PARENT = parent;
        this.yLevel = y;
        if (depth >= MAX_DEPTH) {
            assert bounds.width() == 32;
            this.brick = new Brick(x, y, z);
            this.ROOT.updateAdjacency(this);
        }
    }

    protected void updateAdjacency(QuadTree leaf) {
        if (this == leaf) return;
        if (!this.bounds.neighbors(leaf.bounds) && !this.bounds.intersects(leaf.bounds)) {
            return;
        }
        if (brick != null) {
            brick.updateAdjacency(leaf.brick);
            return;
        }
        if (children != null) {
            for (var tree : children) {
               tree.updateAdjacency(leaf);
            }
        }
    }

    public boolean tick() {
        if (brick != null) {
            return brick.tick();
        }
        if (children == null) return false;
        int aliveChildren = 0;
        for (var tree : children) {
            if (tree.tick()) aliveChildren++;
        }
        if (aliveChildren == 0) this.prune();
        return aliveChildren > 0;
    }

    public boolean insert(WakeNode node) {
        // TODO FIX WAKES ARE INSERTED MULTIPLE TIMES BUT IN DIFFERENT BRICKS (LEAVES GAPS) COULD BE ISSUE WITH MESHER
        if (!this.bounds.contains(node.x, node.z)) {
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

    public void query(Frustum frustum, ArrayList<WakeQuad> output) {
        if (!frustum.isVisible(this.bounds.toBox((int) yLevel))) {
            return;
        }
        if (brick != null) {
            brick.query(frustum, output);
            return;
        }
        if (children == null) return;
        for (var tree : children) {
            tree.query(frustum, output);
        }
    }

    private void subdivide() {
        if (brick != null) return;
        int x = this.bounds.x;
        int z = this.bounds.z;
        int w = this.bounds.width >> 1;
        children = new ArrayList<>();
        children.add(0, new QuadTree(x, yLevel, z, w, depth + 1, this.ROOT, this)); // NW
        children.add(1, new QuadTree(x + w, yLevel, z, w, depth + 1, this.ROOT, this)); // NE
        children.add(2, new QuadTree(x, yLevel, z + w, w, depth + 1, this.ROOT, this)); // SW
        children.add(3, new QuadTree(x + w, yLevel, z + w, w, depth + 1, this.ROOT, this)); // SE
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

    public record DecentralizedBounds(int x, float y, int z, int width) {
        public boolean contains(int x, int z) {
            return this.x <= x && x < this.x + this.width &&
                    this.z <= z && z < this.z + this.width;
        }

        public boolean intersects(DecentralizedBounds other) {
            return !(this.x > other.x + other.width ||
                    this.x + this.width < other.x ||
                    this.z > other.z + other.width ||
                    this.z + this.width < other.z);
        }

        public boolean neighbors(DecentralizedBounds other) {
            return !(this.x == other.x + other.width ||
                    this.x + this.width == other.x ||
                    this.z == other.z + other.width ||
                    this.z + this.width == other.z);
        }

        public Box toBox(int y) {
            return new Box(this.x, y - 0.5, this.z,
                    this.x + this.width, y + 0.5, this.z + this.width);
        }
    }
}
