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
        int n = 0;
        if (WakesClient.CONFIG_INSTANCE.drawDebugBoxes) {
            for (var brick : wakeHandler.getVisible(context.frustum())) {
                for (var quad : brick.quads) {
                    Vec3d brickPos = brick.getPos();
                    float y = quad.nodes[0][0].height;
                    System.out.printf();
                    Box box = new Box(brickPos.x + quad.x, y, brickPos.z + quad.z, brickPos.x + quad.x + quad.w, y + 0.1f, brickPos.z + quad.z + quad.h);
                    var col = Color.getHSBColor(new Random(quad.hashCode()).nextFloat(), 1f, 1f).getRGBColorComponents(null);
                    DebugRenderer.drawBox(context.matrixStack(), context.consumers(),
                            box.offset(context.camera().getPos().negate()),
                            col[0], col[1], col[2], 0.5f);
                    n++;
                }
            }
        }
        WakesDebugInfo.quadsRendered = n;
    }
}
