package com.goby56.wakes.render.debug;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.simulation.QuadTree;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Random;

public class WakeDebugRenderer implements WorldRenderEvents.DebugRender {

    @Override
    public void beforeDebugRender(WorldRenderContext context) {
        if (WakesClient.CONFIG_INSTANCE.drawDebugBoxes) {
            WakeHandler wakeHandler = WakeHandler.getInstance();
            for (QuadTree.DebugBB debugBox : wakeHandler.getBBs()) {
                System.out.println(debugBox);
                if (debugBox.depth() < 4) {
                    continue;
                }
                Random r = new Random(debugBox.depth());
                DebugRenderer.drawBox(context.matrixStack(), context.consumers(), debugBox.bb(), r.nextFloat(), r.nextFloat(), r.nextFloat(), 0.4f);
            }
            ArrayList<WakeNode> nodes = wakeHandler.getVisible(context.frustum());
            for (WakeNode node : nodes) {
                Vec3d pos = node.getPos().add(context.camera().getPos().negate());
                Box box = new Box(pos.x, pos.y - 0.1, pos.z, pos.x + 1, pos.y - 0.2, pos.z + 1);
                DebugRenderer.drawBox(context.matrixStack(), context.consumers(), box, 1f, 0f, 1f, 0.4f);
            }
        }
    }

    public static class TreeBoundingBox {
        public Box bb;
        public int depth;

        public TreeBoundingBox(Box bb, int depth) {
           this.bb = bb;
           this.depth = depth;
        }
    }
}
