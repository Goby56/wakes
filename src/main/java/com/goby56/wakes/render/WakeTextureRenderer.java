package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import java.util.ArrayList;

public class WakeTextureRenderer implements WorldRenderEvents.AfterTranslucent {
    public static int nodesRendered = 0;

    @Override
    public void afterTranslucent(WorldRenderContext context) {
        if (WakesClient.CONFIG_INSTANCE.disableMod) {
            return;
        }
        WakeHandler wakeHandler = WakeHandler.getInstance();
        if (wakeHandler == null || wakeHandler.resolutionResetScheduled) return;

        ArrayList<WakeNode> nodes = wakeHandler.getVisible(context.frustum());
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        context.lightmapTextureManager().enable();

        float x, y, z;

        int n = 0;
        for (WakeNode node : nodes) {
            if (node.isDead()) continue;
            if (WakesClient.CONFIG_INSTANCE.wakeResolution.res != WakeNode.res) continue;
            Vec3d screenSpace = node.getPos().add(context.camera().getPos().negate());
            x = (float) screenSpace.x;
            y = (float) screenSpace.y;
            z = (float) screenSpace.z;
            float distance = (float) Math.sqrt(screenSpace.lengthSquared());
            node.distanceFromCamera = distance;
            if (node.tex == null) {
                continue;
            }

            // DynamicWakeTexture tex = null;
            //
            // if (WakesClient.CONFIG_INSTANCE.storeTexturesPerNode && node.tex != null) {
            //     tex = node.tex;
            // }
            // else {
            //     int waterCol = BiomeColors.getWaterColor(context.world(), node.blockPos());
            //     float a = (float) ((-Math.pow(node.t, 2) + 1) * WakesClient.CONFIG_INSTANCE.wakeOpacity);
            //     tex
            //     wakeTexture.populatePixels(node, distance, waterCol, a);
            // }

            int light = WorldRenderer.getLightmapCoordinates(context.world(), node.blockPos());
            node.tex.render(matrix, x, y, z, light);
            // TODO IMPLEMENT NODE TEXTURE RENDER CLUMPING (RENDER MULTIPLE NODES IN ONE QUAD/PASS)

            n++;
        }

        RenderSystem.enableCull();

        WakeTextureRenderer.nodesRendered = n;

    }
}
