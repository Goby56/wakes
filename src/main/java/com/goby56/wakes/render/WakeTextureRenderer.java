package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.render.enums.BlendingFunction;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import java.util.ArrayList;

public class WakeTextureRenderer implements WorldRenderEvents.AfterTranslucent {

    @Override
    public void afterTranslucent(WorldRenderContext context) {
        if (WakesClient.CONFIG_INSTANCE.disableMod) {
            return;
        }
        WakeHandler wakeHandler = WakeHandler.getInstance();
        if (wakeHandler == null || wakeHandler.resetScheduled) return;

        ArrayList<WakeNode> nodes = wakeHandler.getVisible(context.frustum());
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        context.lightmapTextureManager().enable();

        BlendingFunction.applyBlendFunc();

        DynamicWakeTexture wakeTexture = DynamicWakeTexture.getInstance();

        int waterCol;
        float r, g, b, a;
        float x, y, z;
        int light;
        long fullTime;
        long t1;
        long memoryTime = 0;
        long t2;
        long renderingTime = 0;
        long t3;

        t1 = System.nanoTime();

        for (WakeNode node : nodes) {
            if (node.isDead()) continue;
            if (wakeHandler.resolution.res != node.res) continue;
            Vec3d screenSpace = node.getPos().add(context.camera().getPos().negate());
            x = (float) screenSpace.x;
            y = (float) screenSpace.y;
            z = (float) screenSpace.z;
            float distance = (float) Math.sqrt(screenSpace.lengthSquared());

            t2 = System.nanoTime();

            waterCol = BiomeColors.getWaterColor(context.world(), node.blockPos());
            light = WorldRenderer.getLightmapCoordinates(context.world(), node.blockPos());
            a = (float) ((-Math.pow(node.t, 2) + 1) * Math.pow(WakesClient.CONFIG_INSTANCE.wakeOpacity, WakesClient.CONFIG_INSTANCE.blendFunc.canVaryOpacity ? 1 : 0));

            wakeTexture.populatePixels(node, distance, waterCol, a);

            memoryTime += System.nanoTime() - t2;

            t3 = System.nanoTime();

            // TODO IMPLEMENT NODE TEXTURE RENDER CLUMPING (RENDER MULTIPLE NODES IN ONE QUAD/PASS)
            wakeTexture.render(matrix, x, y, z, 1f, 1f, 1f, 1f, light);

            renderingTime += System.nanoTime() - t3;
        }

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();

        fullTime = System.nanoTime() - t1;
        // if (!nodes.isEmpty() && !MinecraftClient.getInstance().isPaused()) {
        //     System.out.printf("Full time: %d, Memory time: %.2f, Rendering time: %.2f\n", fullTime, memoryTime / (float) fullTime, renderingTime / (float) fullTime);
        // }
    }
}
