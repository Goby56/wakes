package com.goby56.wakes.render.debug;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.utils.WakesTimers;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.util.math.Box;

import java.awt.*;
import java.util.Random;

public class WakeDebugRenderer implements WorldRenderEvents.DebugRender {

    @Override
    public void beforeDebugRender(WorldRenderContext context) {
        WakeHandler wakeHandler = WakeHandler.getInstance();
        if (WakesClient.CONFIG_INSTANCE.drawDebugBoxes) {
            for (var brick : wakeHandler.getVisible(context.frustum())) {
                if (WakesClient.CONFIG_INSTANCE.wakeResolution.res != WakeNode.res) continue;
                for (var quad : brick.quads) {
                    float y = quad.nodes[0][0].height;
                    Box box = new Box(quad.x, y, quad.z, quad.x + quad.w, y + 0.1f, quad.z + quad.h);
                    var col = Color.getHSBColor(new Random(quad.hashCode()).nextFloat(), 1f, 1f).getRGBColorComponents(null);
                    DebugRenderer.drawBox(context.matrixStack(), context.consumers(),
                            box.offset(context.camera().getPos().negate()),
                            col[0], col[1], col[2], 0.5f);
                }
            }
        }
    }
}
