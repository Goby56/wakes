package com.goby56.wakes.mixin;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.render.WakeTextureRenderer;
import com.goby56.wakes.simulation.WakeHandler;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugHud.class)
public class DebugHudMixin {

    @Inject(at = @At("RETURN"), method = "getLeftText")
    protected void getLeftText(CallbackInfoReturnable<List<String>> info) {
        if (WakesClient.CONFIG_INSTANCE.drawDebugBoxes) {
            info.getReturnValue().add(String.format("[Wakes] Rendering %d/%d wake nodes", WakeTextureRenderer.nodesRendered, WakeHandler.getInstance().getTotal()));
            info.getReturnValue().add(String.format("[Wakes] Max tree depth: %d", WakeHandler.getInstance().getMaxDepth()));
        }
    }
}
