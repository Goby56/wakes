package com.goby56.wakes.debug;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.simulation.Brick;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.Random;

public class WakeDebugRenderer implements WorldRenderEvents.DebugRender {

    @Override
    public void beforeDebugRender(WorldRenderContext context) {
        WakeHandler wakeHandler = WakeHandler.getInstance();
        if (WakesClient.CONFIG_INSTANCE.drawDebugBoxes) {
            for (var node : wakeHandler.getVisible(context.frustum(), WakeNode.class)) {
                DebugRenderer.drawBox(node.toBox().offset(context.camera().getPos().negate()),
                        1, 0, 1, 0.5f);
            }
            for (var brick : wakeHandler.getVisible(context.frustum(), Brick.class)) {
                Vec3d pos = brick.pos;
                Box box = new Box(pos.x, pos.y - (1 - WakeNode.WATER_OFFSET), pos.z, pos.x + brick.dim, pos.y, pos.z + brick.dim);
                var col = Color.getHSBColor(new Random(pos.hashCode()).nextFloat(), 1f, 1f).getRGBColorComponents(null);
                DebugRenderer.drawBox(box.offset(context.camera().getPos().negate()),
                        col[0], col[1], col[2], 0.5f);
            }
        }
    }
}
