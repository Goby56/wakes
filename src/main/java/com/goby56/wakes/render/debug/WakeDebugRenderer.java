package com.goby56.wakes.render.debug;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import com.goby56.wakes.utils.WakesDebugInfo;
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
                Box box = new Box(node.x, node.height - 0.1f, node.z, node.x + 1, node.height - 0.2f, node.z + 1);
                var col = Color.getHSBColor(new Random(node.hashCode()).nextFloat(), 1f, 1f).getRGBColorComponents(null);
                DebugRenderer.drawBox(context.matrixStack(), context.consumers(),
                        box.offset(context.camera().getPos().negate()),
                        col[0], col[1], col[2], 0.5f);
            }
        }
    }
}
