package com.goby56.wakes.render.debug;

import com.goby56.wakes.particle.custom.WakeParticle;
import com.goby56.wakes.utils.WakeData;
import com.goby56.wakes.utils.WakeNode;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedList;

public class WakeDebugRenderer {
    public static void drawWakeNodes(LinkedList<WakeParticle.Node> nodes, Camera camera) {
        Vec3d[] vertices = new Vec3d[nodes.size()];
        int i = 0;
        for (WakeParticle.Node node : nodes) {
            vertices[i] = node.position;
            i++;
        }
        DebugUtils.drawLines(vertices, camera.getPos());
    }

    public static void drawWakeNodes(Camera camera) {
        ArrayList<WakeNode> nodes = WakeData.getInstance().getNearby(camera.getPos());
        if (nodes.size() == 0) return;
        Vec3d[] vertices = new Vec3d[nodes.size()];
        int i = 0;
        for (WakeNode node : nodes) {
            vertices[i] = new Vec3d(node.position.getX(), node.position.getY(), node.position.getZ());
//            Vec3d pos = new Vec3d(node.position.getX(), node.position.getY(), node.position.getZ());
//            vertices[i] = pos;
//            vertices[i+1] = pos.add(new Vec3d(1, 0, 0));
//            vertices[i+2] = pos.add(new Vec3d(0, 0, 1));
//            vertices[i+3] = pos.add(new Vec3d(1, 0, 1));
            i++;
        }
        DebugUtils.drawLines(vertices, camera.getPos());
    }
}
