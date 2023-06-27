package com.goby56.wakes.render.debug;

import com.goby56.wakes.particle.custom.WakeParticle;
import com.goby56.wakes.utils.WakeHandler;
import com.goby56.wakes.utils.WakeNode;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.LinkedList;

public class WakeDebugRenderer implements WorldRenderEvents.DebugRender {
    public static void drawWakeNodes(LinkedList<WakeParticle.Node> nodes, Camera camera) {
        Vec3d[] vertices = new Vec3d[nodes.size()];
        int i = 0;
        for (WakeParticle.Node node : nodes) {
            vertices[i] = node.position;
            i++;
        }
        DebugUtils.drawLines(vertices, camera.getPos());
    }

    @Override
    public void beforeDebugRender(WorldRenderContext context) {
        ArrayList<WakeNode> nodes = WakeHandler.getInstance().getVisible(context.frustum());
        for (WakeNode node : nodes) {
            Vec3d pos = node.getPos().add(context.camera().getPos().negate());
//            Box box = new Box(pos.x, pos.y + node.debugOffset, pos.z, pos.x + 1, pos.y + node.debugOffset + 0.1, pos.z + 1);
//            DebugRenderer.drawBox(context.matrixStack(), context.consumers(), box, 1f, 0f, 1f, 0.4f);
        }
    }
}
