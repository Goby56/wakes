package com.goby56.wakes.debug;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.render.LightmapWrapper;
import com.goby56.wakes.simulation.Brick;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.Random;

public class WakeDebugRenderer implements WorldRenderEvents.DebugRender {
    public static final Identifier DEBUG_TEXTURE_LAYER = Identifier.of(WakesClient.MOD_ID, "debug-texture-layer");

    @Override
    public void beforeDebugRender(WorldRenderContext context) {
        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null) return;
        if (WakesConfig.drawDebugBoxes) {
            for (var node : wakeHandler.getVisible(context.frustum(), WakeNode.class)) {
                DebugRenderer.drawBox(context.matrixStack(), context.consumers(),
                        node.toBox().offset(context.camera().getPos().negate()),
                        1, 0, 1, 0.5f);
            }
            for (var brick : wakeHandler.getVisible(context.frustum(), Brick.class)) {
                Vec3d pos = brick.pos;
                Box box = new Box(pos.x, pos.y - (1 - WakeNode.WATER_OFFSET), pos.z, pos.x + brick.dim, pos.y, pos.z + brick.dim);
                var col = Color.getHSBColor(new Random(pos.hashCode()).nextFloat(), 1f, 1f).getRGBColorComponents(null);
                DebugRenderer.drawBox(context.matrixStack(), context.consumers(),
                        box.offset(context.camera().getPos().negate()),
                        col[0], col[1], col[2], 0.5f);
            }
        }
    }

    public static void registerDebugTextureRenderer() {
       // HudLayerRegistrationCallback.EVENT.register(layeredDrawerWrapper -> {
       //     layeredDrawerWrapper.attachLayerBefore(IdentifiedLayer.DEBUG, WakeDebugRenderer.DEBUG_TEXTURE_LAYER, WakeDebugRenderer::renderOnHUD);
        //});
    }

    public static void renderOnHUD(DrawContext context, RenderTickCounter tickCounter) {
        if (WakesConfig.showDebugInfo && MinecraftClient.getInstance().getDebugHud().shouldShowDebugHud()) {
          //  LightmapWrapper.render(context.getMatrices().peek().getPositionMatrix());
        }
    }

}
