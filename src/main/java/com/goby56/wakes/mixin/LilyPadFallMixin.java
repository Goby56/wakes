package com.goby56.wakes.mixin;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.config.enums.EffectSpawningRule;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.simulation.WakeNode;
import com.goby56.wakes.utils.WakesUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class LilyPadFallMixin {

    @Inject(at = @At("TAIL"), method = "onLandedUpon")
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance, CallbackInfo ci) {
        if (!world.getBlockState(pos.up()).isOf(Blocks.LILY_PAD)) return;
        if (WakesConfig.disableMod) return;
        EffectSpawningRule rule = WakesUtils.getEffectRuleFromSource(entity);
        ProducesWake wakeProducer = (ProducesWake) entity;
        if (rule.simulateWakes) {
            wakeProducer.wakes$setWakeHeight(pos.getY() + WakeNode.WATER_OFFSET);
            WakesUtils.placeFallSplash(entity);
        }
    }

}
