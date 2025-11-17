package com.goby56.wakes.debug;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.simulation.Brick;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.Random;

public class WakeDebugRenderer implements WorldRenderEvents.DebugRender {
    public static final ResourceLocation DEBUG_TEXTURE_LAYER = ResourceLocation.fromNamespaceAndPath(WakesClient.MOD_ID, "debug-texture-layer");

    @Override
    public void beforeDebugRender(WorldRenderContext context) {
        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null) return;
        if (WakesConfig.drawDebugBoxes) {
            Camera camera = context.gameRenderer().getMainCamera();
            for (var node : wakeHandler.getVisible(WakeNode.class)) {
                DebugRenderer.renderFilledBox(context.matrices(), context.consumers(),
                        node.toBox().move(camera.getPosition().reverse()),
                        1, 0, 1, 0.5f);
            }
            for (var brick : wakeHandler.getVisible(Brick.class)) {
                Vec3 pos = brick.pos;
                AABB box = new AABB(pos.x, pos.y - (1 - WakeNode.WATER_OFFSET), pos.z, pos.x + brick.dim, pos.y, pos.z + brick.dim);
                var col = Color.getHSBColor(new Random(pos.hashCode()).nextFloat(), 1f, 1f).getRGBColorComponents(null);
                DebugRenderer.renderFilledBox(context.matrices(), context.consumers(),
                        box.move(camera.getPosition().reverse()),
                        col[0], col[1], col[2], 0.5f);
            }
        }

    }

    public static void registerDebugTextureRenderer() {
       // HudLayerRegistrationCallback.EVENT.register(layeredDrawerWrapper -> {
       //     layeredDrawerWrapper.attachLayerBefore(IdentifiedLayer.DEBUG, WakeDebugRenderer.DEBUG_TEXTURE_LAYER, WakeDebugRenderer::renderOnHUD);
        //});
    }

    public static void renderOnHUD(GuiGraphics context, DeltaTracker tickCounter) {
        if (WakesConfig.showDebugInfo && Minecraft.getInstance().getDebugOverlay().showDebugScreen()) {
          //  LightmapWrapper.render(context.getMatrices().peek().getPositionMatrix());
        }
    }

}
