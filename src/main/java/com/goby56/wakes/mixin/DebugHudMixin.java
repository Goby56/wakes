package com.goby56.wakes.mixin;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.render.WakeRenderer;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.utils.WakesDebugInfo;
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
            info.getReturnValue().add(String.format("[Wakes] Rendering %d quads for %d wake nodes", WakesDebugInfo.quadsRendered, WakeHandler.getInstance().getTotal()));
            info.getReturnValue().add(String.format("[Wakes] Node logic: %.2fms/t", 10e-6 * WakesDebugInfo.nodeLogicTime));
            info.getReturnValue().add(String.format("[Wakes] Mesh gen: %.2fms/t", 10e-6 * WakesDebugInfo.meshGenerationTime));
            info.getReturnValue().add(String.format("[Wakes] Texturing: %.2fms/t", 10e-6 * WakesDebugInfo.texturingTime));
            info.getReturnValue().add(String.format("[Wakes] Rendering: %.3fms/f", 10e-6 * WakesDebugInfo.wakeRenderingTime.stream().reduce(0.0, Double::sum) / WakesDebugInfo.wakeRenderingTime.size()));
        }
    }
}