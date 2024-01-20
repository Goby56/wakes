package com.goby56.wakes.mixin;

import com.goby56.wakes.duck.ProducesWake;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.entity.passive.TameableEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FollowOwnerGoal.class)
public class TameableTeleportMixin {
    @Shadow private TameableEntity tameable;

    @Inject(at = @At("TAIL"), method = "tryTeleportTo")
    private void onTeleport(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            ((ProducesWake) tameable).setRecentlyTeleported(true);
        }
    }
}
