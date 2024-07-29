package com.goby56.wakes.render.debug;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.simulation.WakeHandler;
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
            for (var quad : wakeHandler.getVisible(context.frustum())) {
                Box box = new Box(quad.x, quad.y - 0.1f, quad.z, quad.x + quad.w, quad.y - 0.2f, quad.z + quad.h);
                var col = Color.getHSBColor(new Random(quad.hashCode()).nextFloat(), 1f, 1f).getRGBColorComponents(null);
                DebugRenderer.drawBox(context.matrixStack(), context.consumers(),
                        box.offset(context.camera().getPos().negate()),
                        col[0], col[1], col[2], 0.5f);
            }
        }
    }
}
