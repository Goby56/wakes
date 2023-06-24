package com.goby56.wakes.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

public class WakeHandler {
    private static WakeHandler INSTANCE;

    private final ArrayList<QuadTree<WakeNode>> trees;
    private final int minY;
    private final int maxY;

    private WakeHandler() {
        this.minY = MinecraftClient.getInstance().world.getBottomY();
        this.maxY = MinecraftClient.getInstance().world.getTopY();
        int worldHeight = this.maxY - this.minY;
        this.trees = new ArrayList<>(worldHeight);
        for (int i = 0; i < worldHeight; i++) {
            this.trees.add(null);
        }
    }

    public static WakeHandler getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new WakeHandler();
        }
        return INSTANCE;
    }

    public void tick() {
        for (QuadTree<WakeNode> tree : this.trees) {
            if (tree == null) {
                continue;
            }
            tree.tick();
        }
    }

    public void insert(WakeNode node) {
        int i = this.getArrayIndex((int) node.height);

        if (this.trees.get(i) == null) {
            this.trees.add(i, new QuadTree<>(0, 0, 30000000, 0) );
        }
        this.trees.get(i).insert(node);
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
