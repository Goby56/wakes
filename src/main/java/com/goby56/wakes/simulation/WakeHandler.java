package com.goby56.wakes.simulation;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.enums.Resolution;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Queue;

public class WakeHandler {
    public final int MAX_QUERY_RANGE = 10;

    private static WakeHandler INSTANCE;
    public World world;

    private final ArrayList<QuadTree<WakeNode>> trees;
    private final ArrayList<Queue<WakeNode>> toBeInserted;
    public boolean resolutionResetScheduled = false;
    private final int minY;
    private final int maxY;

    private WakeHandler(ClientWorld world) {
        this.world = world;
        WakeNode.calculateWaveDevelopmentFactors();
        this.minY = world.getBottomY();
        this.maxY = world.getTopY();
        int worldHeight = this.maxY - this.minY;
        this.trees = new ArrayList<>(worldHeight);
        this.toBeInserted = new ArrayList<>(worldHeight);
        for (int i = 0; i < worldHeight; i++) {
            this.trees.add(null);
            this.toBeInserted.add(new QueueSet<>());
        }
    }

    public static WakeHandler getInstance() {
        if (INSTANCE == null) {
            if (MinecraftClient.getInstance().world == null) {
                return null;
            }
            INSTANCE = new WakeHandler(MinecraftClient.getInstance().world);
        }
        return INSTANCE;
    }

    public void tick() {
        for (int i = 0; i < this.maxY - this.minY; i++) {
            if (this.resolutionResetScheduled) {
                this.toBeInserted.get(i).clear();
                continue;
            }
            QuadTree<WakeNode> tree = this.trees.get(i);
            if (tree != null) {
                tree.tick();


                Queue<WakeNode> pendingNodes = this.toBeInserted.get(i);
                while (pendingNodes.peek() != null) {
                    tree.insert(pendingNodes.poll());
                }
            }
        }
        if (this.resolutionResetScheduled) {
            this.changeResolution();
        }
    }

    public void insert(WakeNode node) {
        if (this.resolutionResetScheduled) return;
        int i = this.getArrayIndex((int) node.height);
        if (i < 0) return;

        if (this.trees.get(i) == null) {
            this.trees.add(i, new QuadTree<>(0, 0, 30000000));
        }

        this.toBeInserted.get(i).add(node);
    }

    public ArrayList<WakeNode> getVisible(Frustum frustum) {
        ArrayList<WakeNode> foundNodes = new ArrayList<>();
        for (int i = 0; i < this.maxY - this.minY; i++) {
            if (this.trees.get(i) != null) {
                this.trees.get(i).query(frustum, i + this.minY, foundNodes);
            }
        }
        return foundNodes;
    }

    public int getTotal() {
        // TODO SEEMS LIKE THERE ARE DUPLICATE NODES
        int n = 0;
        for (int y = 0; y < this.maxY - this.minY; y++) {
            if (this.trees.get(y) != null) {
                n += this.trees.get(y).count();
            }
        }
        return n;
    }

    public int getMaxDepth() {
        int maxDepth = 0;
        for (int y = 0; y < this.maxY - this.minY; y++) {
            QuadTree<WakeNode> t = this.trees.get(y);
            if (t != null) {
                int depth = t.getDepth();
                if (depth > maxDepth) maxDepth = depth;
            }
        }
        return maxDepth;
    }

    public ArrayList<QuadTree.DebugBB> getBBs() {
        ArrayList<QuadTree.DebugBB> boxes = new ArrayList<>();
        for (int y = 0; y < this.maxY - this.minY; y++) {
            if (this.trees.get(y) != null) {
                this.trees.get(y).getBBs(boxes, y - Math.abs(this.minY));
            }
        }
        return boxes;
    }

    private int getArrayIndex(int height) {
        if (height < this.minY || height > this.maxY) {
            return -1;
        }
        return height + Math.abs(this.minY);
    }

    public static void scheduleResolutionChange(Resolution newRes) {
        WakesClient.CONFIG_INSTANCE.wakeResolution = newRes;
        WakeHandler wakeHandler = WakeHandler.getInstance();
        if (wakeHandler == null) {
            return;
        }
        wakeHandler.resolutionResetScheduled = true;
    }

    private void changeResolution() {
        this.reset();
        WakeNode.res = WakesClient.CONFIG_INSTANCE.wakeResolution.res;
        this.resolutionResetScheduled = false;
    }

    private void reset() {
        for (int i = 0; i < this.maxY - this.minY; i++) {
            QuadTree<WakeNode> tree = this.trees.get(i);
            if (tree != null) {
                tree.prune();
            }
        }
    }

    public static class WorldNotFoundException extends Exception {
        public WorldNotFoundException() {
            super("WakeHandler singleton was accessed too early! Player needs to be in a world.");
        }
    }
}
