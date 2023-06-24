package com.goby56.wakes.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class WakeHandler {
    private static WakeHandler INSTANCE;

    private final ArrayList<QuadTree<WakeNode>> trees;
    private final ArrayList<Set<WakeNode>> toBeInserted;
    private final int minY;
    private final int maxY;

    private WakeHandler() {
        this.minY = MinecraftClient.getInstance().world.getBottomY();
        this.maxY = MinecraftClient.getInstance().world.getTopY();
        int worldHeight = this.maxY - this.minY;
        this.trees = new ArrayList<>(worldHeight);
        this.toBeInserted = new ArrayList<>(worldHeight);
        for (int i = 0; i < worldHeight; i++) {
            this.trees.add(null);
            this.toBeInserted.add(new HashSet<>());
        }
    }

    public static WakeHandler getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new WakeHandler();
        }
        return INSTANCE;
    }

    public void tick() {
        for (int i = 0; i < this.maxY - this.minY; i++) {
            QuadTree<WakeNode> tree = this.trees.get(i);
            if (tree != null) {
                tree.tick();

                Set<WakeNode> pendingNodes = this.toBeInserted.get(i);
                for (WakeNode node : pendingNodes) {
                    tree.insert(node);
                }
                pendingNodes.clear();
            }
        }
    }

    public void insert(WakeNode node) {
        int i = this.getArrayIndex((int) node.height);

        if (this.trees.get(i) == null) {
            this.trees.add(i, new QuadTree<>(0, 0, 30000000, 0));
        }

        this.toBeInserted.get(i).add(node);
    }

    public ArrayList<WakeNode> getNearby(Vec3d pos) {
        ArrayList<WakeNode> foundNodes = new ArrayList<>();
        QuadTree.AABB range = new QuadTree.AABB((int) pos.getX(), (int) pos.getZ(), 100);
        for (int y = -10; y < 10; y++) {
            int i = this.getArrayIndex((int) (pos.getY() + y));
            if (i == -1) continue;
            if (this.trees.get(i) != null) {
                this.trees.get(i).query(range, foundNodes);
            }
        }
        return foundNodes;
    }

    private int getArrayIndex(int height) {
        if (height < this.minY || height > this.maxY) {
            return -1;
        }
        return height + Math.abs(this.minY);
    }
}
