package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.config.enums.Resolution;
import com.goby56.wakes.simulation.Brick;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import com.goby56.wakes.debug.WakesDebugInfo;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.joml.Matrix4f;

import java.util.*;

public class WakeRenderer implements WorldRenderEvents.AfterTranslucent {
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
        if (WakesConfig.disableMod) {
            WakesDebugInfo.quadsRendered = 0;
            return;
        }

        if (wakeTextures == null) initTextures();

        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null || WakeHandler.resolutionResetScheduled) return;

        ArrayList<Brick> bricks = wakeHandler.getVisible(context.frustum(), Brick.class);

        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        context.lightmapTextureManager().enable();

        Resolution resolution = WakesConfig.wakeResolution;
        if (resolution.res != WakeNode.res) return;
        int n = 0;
        long tRendering = System.nanoTime();
        for (var brick : bricks) {
            wakeTextures.get(resolution).render(matrix, context.camera(), brick);
            n++;
        }
        WakesDebugInfo.renderingTime.add(System.nanoTime() - tRendering);
        WakesDebugInfo.quadsRendered = n;
    }
}
