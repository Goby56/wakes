package com.goby56.wakes.mixin;

import com.goby56.wakes.duck.ProducesWake;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FollowOwnerGoal.class)
public class TameableTeleportMixin {

    @Inject(at = @At("TAIL"), method = "tryTeleportTo")
    private void onTeleport(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            ((ProducesWake) this).setRecentlyTeleported(true);
        }
    }
}
