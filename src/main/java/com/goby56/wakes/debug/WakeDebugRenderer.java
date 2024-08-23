package com.goby56.wakes.debug;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.simulation.Brick;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import kroppeb.stareval.function.Type;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class WakeDebugRenderer implements WorldRenderEvents.DebugRender {

    @Override
    public void beforeDebugRender(WorldRenderContext context) {
        WakeHandler wakeHandler = WakeHandler.getInstance();
        Map<Integer, Integer> nodeHeights = new HashMap<>();
        Map<Integer, Integer> brickHeights = new HashMap<>();
        if (WakesClient.CONFIG_INSTANCE.drawDebugBoxes) {
            for (var node : wakeHandler.getVisible(context.frustum(), WakeNode.class)) {
                nodeHeights.merge((int) node.height, 1, Integer::sum);
                Box box = new Box(node.x, node.height - 0.1f, node.z, node.x + 1, node.height - 0.2f, node.z + 1);
                DebugRenderer.drawBox(context.matrixStack(), context.consumers(),
                        box.offset(context.camera().getPos().negate()),
                        1, 0, 1, 0.5f);
            }
            for (var brick : wakeHandler.getVisible(context.frustum(), Brick.class)) {
                brickHeights.merge((int) brick.pos.y, 1, Integer::sum);
                Vec3d pos = brick.pos;
                Box box = new Box(pos.x, pos.y - 0.2f, pos.z, pos.x + brick.dim, pos.y - 0.3f, pos.z + brick.dim);
                var col = Color.getHSBColor(new Random(pos.hashCode()).nextFloat(), 1f, 1f).getRGBColorComponents(null);
                DebugRenderer.drawBox(context.matrixStack(), context.consumers(),
                        box.offset(context.camera().getPos().negate()),
                        col[0], col[1], col[2], 0.5f);
            }
        }
    }
}
