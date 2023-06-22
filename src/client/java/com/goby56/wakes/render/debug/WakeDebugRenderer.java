package com.goby56.wakes.render.debug;

import com.goby56.wakes.particle.custom.WakeParticle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

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
}
