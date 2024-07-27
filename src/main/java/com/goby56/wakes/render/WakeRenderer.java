package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.enums.Resolution;
import com.goby56.wakes.simulation.Brick;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.utils.WakesDebugInfo;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.joml.Matrix4f;

import java.util.*;

public class WakeRenderer implements WorldRenderEvents.AfterTranslucent {
    public static int quadsRendered = 0;
    public static Map<Resolution, WakeTexture> wakeTextures = null;

    private void initTextures() {
        wakeTextures = Map.of(
                Resolution.EIGHT, new WakeTexture(Resolution.EIGHT.res),
                Resolution.SIXTEEN, new WakeTexture(Resolution.SIXTEEN.res),
                Resolution.THIRTYTWO, new WakeTexture(Resolution.THIRTYTWO.res)
        );
    }

    @Override
    public void afterTranslucent(WorldRenderContext context) {
        if (WakesClient.CONFIG_INSTANCE.disableMod) {
            return;
        }

        if (wakeTextures == null) initTextures();

        WakeHandler wakeHandler = WakeHandler.getInstance();
        if (wakeHandler == null || wakeHandler.resolutionResetScheduled) return;

        double tRendering = System.nanoTime();

        ArrayList<Brick> bricks = wakeHandler.getVisible(context.frustum());
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        context.lightmapTextureManager().enable();

        // int n = 0;
        // for (var brick : bricks) {
        //     Resolution resolution = WakesClient.CONFIG_INSTANCE.wakeResolution;
        //     if (resolution.res != WakeNode.res) continue;

        //     for (var quad : brick.quads) {
        //         wakeTextures.get(resolution).render(matrix, context.camera(), brick, quad, wakeHandler.world);
        //         n++;
        //     }

        // }
        // RenderSystem.enableCull();

        // quadsRendered = n;

        WakesDebugInfo.wakeRenderingTime.add(System.nanoTime() - tRendering);
    }
}
