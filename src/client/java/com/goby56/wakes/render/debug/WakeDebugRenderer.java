package com.goby56.wakes.render.debug;

import com.goby56.wakes.WakesClient;
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

    @Override
    public void beforeDebugRender(WorldRenderContext context) {
        if (!WakesClient.CONFIG_INSTANCE.drawDebugBoxes) {
            return;
        }
        ArrayList<WakeNode> nodes = WakeHandler.getInstance().getVisible(context.frustum());
        for (WakeNode node : nodes) {
            Vec3d pos = node.getPos().add(context.camera().getPos().negate());
            Box box = new Box(pos.x, pos.y - 0.1, pos.z, pos.x + 1, pos.y - 0.2, pos.z + 1);
            DebugRenderer.drawBox(box, 1f, 0f, 1f, 0.4f);
        }
    }
}
