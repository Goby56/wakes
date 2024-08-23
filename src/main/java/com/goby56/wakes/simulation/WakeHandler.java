package com.goby56.wakes.simulation;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.enums.Resolution;
import com.goby56.wakes.debug.WakesDebugInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Queue;

public class WakeHandler {
    private static WakeHandler INSTANCE;
    public World world;

    private QuadTree[] trees;
    private QueueSet<WakeNode>[] toBeInserted;
    public boolean resolutionResetScheduled = false;
    private final int minY;
    private final int maxY;

    private WakeHandler(ClientWorld world) {
        this.world = world;
        WakeNode.calculateWaveDevelopmentFactors();
        this.minY = world.getBottomY();
        this.maxY = world.getTopY();
        int worldHeight = this.maxY - this.minY;
        this.trees = new QuadTree[worldHeight];
        this.toBeInserted = new QueueSet[worldHeight];
        for (int i = 0; i < worldHeight; i++) {
            toBeInserted[i] = new QueueSet<>();
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
            Queue<WakeNode> pendingNodes = this.toBeInserted[i];
            if (this.resolutionResetScheduled) {
                if (pendingNodes != null) pendingNodes.clear();
                continue;
            }
            QuadTree tree = this.trees[i];
            if (tree != null) {
                tree.tick();
                long tInsertion = System.nanoTime();
                while (pendingNodes.peek() != null) {
                    tree.insert(pendingNodes.poll());
                }
                WakesDebugInfo.insertionTime = System.nanoTime() - tInsertion;
            }
        }
        if (this.resolutionResetScheduled) {
            this.changeResolution();
        }
    }

    public void insert(WakeNode node) {
        if (this.resolutionResetScheduled) return;
        int i = this.getArrayIndex(node.y);
        if (i < 0) return;

        if (this.trees[i] == null) {
            this.trees[i] = new QuadTree(node.y);
        }

        if (node.validPos()) {
            this.toBeInserted[i].add(node);
        }
    }

    public <T> ArrayList<T> getVisible(Frustum frustum, Class<T> type) {
        ArrayList<T> visibleQuads = new ArrayList<>();
        for (int i = 0; i < this.maxY - this.minY; i++) {
            if (this.trees[i] != null) {
                this.trees[i].query(frustum, visibleQuads, type);
            }
        }
        return visibleQuads;
    }

    private int getArrayIndex(int y) {
        if (y < this.minY || y > this.maxY) {
            return -1;
        }
        return y - this.minY;
    }

    private int getYLevel(int i) {
        if (i < 0 || i > this.maxY - this.minY) {
            throw new IndexOutOfBoundsException();
        }
        return i + this.minY;
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
            QuadTree tree = this.trees[i];
            if (tree != null) {
                tree.prune();
            }
            toBeInserted[i].clear();
        }
    }

    public static class WorldNotFoundException extends Exception {
        public WorldNotFoundException() {
            super("WakeHandler singleton was accessed too early! Player needs to be in a world.");
        }
    }
}
