package com.goby56.wakes.render.debug;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.simulation.QuadTree;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.debug.ChunkBorderDebugRenderer;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

public class WakeDebugRenderer implements WorldRenderEvents.DebugRender {

    @Override
    public void beforeDebugRender(WorldRenderContext context) {
        //if (WakesClient.CONFIG_INSTANCE.drawDebugBoxes) {
        //    WakeHandler wakeHandler = WakeHandler.getInstance();
        //    int maxTreeDepth = wakeHandler.getMaxDepth();
        //    for (QuadTree.DebugBB debugBox : wakeHandler.getBBs()) {
        //        float depthFactor = (float) debugBox.depth() / maxTreeDepth;
        //        var col = Color.getHSBColor(depthFactor, 1f, 1f).getRGBColorComponents(null);
        //        DebugRenderer.drawBox(context.matrixStack(), context.consumers(),
        //                debugBox.bb().offset(context.camera().getPos().negate().add(0, depthFactor * 0.04, 0)),
        //                col[0], col[1], col[2], 0.5f * depthFactor * depthFactor);
        //    }
        //    ArrayList<WakeNode> nodes = wakeHandler.getVisible(context.frustum());
        //    for (WakeNode node : nodes) {
        //        Vec3d pos = node.getPos().add(context.camera().getPos().negate());
        //        Box box = new Box(pos.x, pos.y - 0.1, pos.z, pos.x + 1, pos.y - 0.2, pos.z + 1);
        //        DebugRenderer.drawBox(context.matrixStack(), context.consumers(), box, 1f, 1f, 1f, 0.6f);
        //    }
        //}
    }
}
