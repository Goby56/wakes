package com.goby56.wakes.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.joml.Vector2i;

import java.util.ArrayList;

public class WakeData {
    private static WakeData INSTANCE;

    private final ArrayList<QuadTree<WakeNode>> trees;
    private final int minY;
    private final int maxY;

    private WakeData() {
        this.minY = MinecraftClient.getInstance().world.getBottomY();
        this.maxY = MinecraftClient.getInstance().world.getTopY();
        int worldHeight = this.maxY - this.minY;
        System.out.printf("creating wake data for %d trees\n", worldHeight);
        this.trees = new ArrayList<>(worldHeight);
        // Fucking hate Java
        for (int i = 0; i < worldHeight; i++) {
            this.trees.add(null);
        }
    }

    public static WakeData getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new WakeData();
        }
        return INSTANCE;
    }

    public void insert(WakeNode node) {
        int i = this.getArrayIndex(node.position.getY());

        if (this.trees.get(i) == null) {
            this.trees.add(i, new QuadTree<>(0, 0, 30000000, 0) );
        }
        this.trees.get(i).insert(node);
    }

    public ArrayList<WakeNode> getNearby(Vec3d pos) {
        ArrayList<WakeNode> foundNodes = new ArrayList<>();
        QuadTree.AABB range = new QuadTree.AABB((int) pos.getX(), (int) pos.getZ(), 10);
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
