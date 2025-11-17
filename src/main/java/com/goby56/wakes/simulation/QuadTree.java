package com.goby56.wakes.simulation;

import com.goby56.wakes.render.FrustumManager;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class QuadTree {
    public static final int BRICK_WIDTH = 4;
    private static final int MAX_DEPTH = (int) (26 - Math.log(BRICK_WIDTH) / Math.log(2));
    private static final int ROOT_X = (int) - Math.pow(2, 25);
    private static final int ROOT_Z = (int) - Math.pow(2, 25);
    private static final int ROOT_WIDTH = (int) Math.pow(2, 26);

    private final QuadTree ROOT;
    private List<QuadTree> children;

    private final DecentralizedBounds bounds;
    private final int depth;
    private Brick brick;
    public final float yLevel;

    public QuadTree(float y) {
        this(ROOT_X, y, ROOT_Z, ROOT_WIDTH, 0, null);
    }

    private QuadTree(int x, float y, int z, int width, int depth, QuadTree root) {
        this.bounds = new DecentralizedBounds(x, y, z, width);
        this.depth = depth;
        this.ROOT = root == null ? this : root;
        this.yLevel = y;
    }

    private boolean hasLeaf() {
        return depth == MAX_DEPTH && brick != null;
    }

    private void initLeaf() {
        if (depth >= MAX_DEPTH) {
            this.brick = new Brick(bounds.x, this.yLevel, bounds.z, bounds.width);
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

    public boolean tick(WakeHandler wakeHandler) {
        if (hasLeaf()) {
            return brick.tick(wakeHandler);
        }
        if (children == null) return false;
        int aliveChildren = 0;
        for (var tree : children) {
            if (tree.tick(wakeHandler)) aliveChildren++;
        }
        if (aliveChildren == 0) this.prune();
        return aliveChildren > 0;
    }

    public boolean insert(WakeNode node) {
        if (!this.bounds.contains(node.x, node.z)) {
            return false;
        }

        if (depth == MAX_DEPTH) {
            if (brick == null) {
                initLeaf();
            }
            brick.insert(node);
            return true;
        }

        if (children == null) this.subdivide();
        for (var tree : children) {
           if (tree.insert(node)) return true;
        }
        return false;
    }

    public void recolorWakes() {
        if (hasLeaf()) {
            brick.populatePixels();
        }
        if (children == null) return;
        for (var tree : children) {
            tree.recolorWakes();
        }
    }

    public <T> void query(ArrayList<T> output, Class<T> type) {
        if (!FrustumManager.isVisible(this.bounds.toBox((int) yLevel))) {
            return;
        }
        if (hasLeaf() && brick.occupied > 0) {
            if (type.equals(Brick.class)) {
                output.add(type.cast(brick));
            }
            if (type.equals(WakeNode.class)) {
                ArrayList<WakeNode> nodes = new ArrayList<>();
                brick.query(nodes);
                for (var node : nodes) {
                    output.add(type.cast(node));
                }
            }
            return;
        }
        if (children == null) return;
        for (var tree : children) {
            tree.query(output, type);
        }
    }

    private void subdivide() {
        if (depth == MAX_DEPTH) return;
        int x = this.bounds.x;
        int z = this.bounds.z;
        int w = this.bounds.width >> 1;
        children = new ArrayList<>();
        children.add(0, new QuadTree(x, yLevel, z, w, depth + 1, this.ROOT)); // NW
        children.add(1, new QuadTree(x + w, yLevel, z, w, depth + 1, this.ROOT)); // NE
        children.add(2, new QuadTree(x, yLevel, z + w, w, depth + 1, this.ROOT)); // SW
        children.add(3, new QuadTree(x + w, yLevel, z + w, w, depth + 1, this.ROOT)); // SE
    }

    public void prune() {
        if (children != null) {
            for (var tree : children) {
                tree.prune();
                if (tree.hasLeaf()) tree.brick.deallocTexture();
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

        public AABB toBox(int y) {
            return new AABB(this.x, y - 0.5, this.z,
                    this.x + this.width, y + 0.5, this.z + this.width);
        }
    }
}
