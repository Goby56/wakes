package com.goby56.wakes.mixin;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.debug.WakesDebugInfo;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugHud.class)
public abstract class DebugHudMixin {

    @Inject(at = @At("RETURN"), method = "getLeftText")
    protected void getLeftText(CallbackInfoReturnable<List<String>> info) {
        if (WakesClient.CONFIG_INSTANCE.showDebugInfo) {
            if (WakesClient.CONFIG_INSTANCE.disableMod) {
                info.getReturnValue().add("[Wakes] Mod disabled!");
            } else {
                WakesDebugInfo.show(info);
            }
        }
    }
}