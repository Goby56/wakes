package com.goby56.wakes.utils;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class WakeHandler {
    public final int MAX_QUERY_RANGE = 10;

    private static WakeHandler INSTANCE;

    private final ArrayList<QuadTree<WakeNode>> trees;
    private final ArrayList<Set<WakeNode>> toBeInserted;
    private final int minY;
    private final int maxY;

    public int glTexId = -1;
    public long imagePointer = -1;

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

        // Initialize texture
        this.glTexId = TextureUtil.generateTextureId();

        GlStateManager._bindTexture(this.glTexId);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0f);

        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_FILTER, GL12.GL_NEAREST);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAG_FILTER, GL12.GL_NEAREST);

        GlStateManager._texImage2D(GlConst.GL_TEXTURE_2D, 0, GlConst.GL_RGBA, 16, 16, 0, GlConst.GL_RGBA, GlConst.GL_UNSIGNED_BYTE, null);

        this.imagePointer = MemoryUtil.nmemAlloc(16 * 16 * 4);
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

    public ArrayList<WakeNode> getVisible(Frustum frustum) {
        ArrayList<WakeNode> foundNodes = new ArrayList<>();
        for (int i = 0; i < this.maxY - this.minY; i++) {
            if (this.trees.get(i) != null) {
                this.trees.get(i).query(frustum, i + this.minY, foundNodes);
            }
        }
        return foundNodes;
    }

    public ArrayList<WakeNode> getNearby(int x, int y, int z) {
        ArrayList<WakeNode> foundNodes = new ArrayList<>();
        int i = this.getArrayIndex(y);
        if (this.trees.get(i) == null) {
            return foundNodes;
        }
        QuadTree.Circle range = new QuadTree.Circle(x, z, this.MAX_QUERY_RANGE);
        this.trees.get(i).query(range, foundNodes);
        return foundNodes;
    }

    private int getArrayIndex(int height) {
        if (height < this.minY || height > this.maxY) {
            return -1;
        }
        return height + Math.abs(this.minY);
    }
}
