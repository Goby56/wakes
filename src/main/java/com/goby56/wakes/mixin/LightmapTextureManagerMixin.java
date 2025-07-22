package com.goby56.wakes.mixin;

import com.goby56.wakes.duck.LightmapAccess;
import com.goby56.wakes.render.LightmapInfo;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin implements LightmapAccess {
    @Shadow private float flickerIntensity;
    @Shadow @Final private MinecraftClient client;

    @Unique
    private int currentTick;

    @Shadow protected abstract float getDarkness(LivingEntity entity, float factor, float tickProgress);

    @Shadow @Final private GameRenderer renderer;
    @Unique
    private LightmapInfo info;

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalInt;)Lcom/mojang/blaze3d/systems/RenderPass;"))
    private void wakes$onUpdate(float tickProgress, CallbackInfo ci, @Local ClientWorld world, @Local Vector3f skyColor) {
        float f = world.getSkyBrightness(1.0F);
        float g;
        if (world.getLightningTicksLeft() > 0) {
            g = 1.0F;
        } else {
            g = f * 0.95F + 0.05F;
        }

        float k = this.client.player.getUnderwaterVisibility();

        float l;
        if (this.client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            l = GameRenderer.getNightVisionStrength(this.client.player, tickProgress);
        } else if (k > 0.0F && this.client.player.hasStatusEffect(StatusEffects.CONDUIT_POWER)) {
            l = k;
        } else {
            l = 0.0F;
        }

        float h = this.client.options.getDarknessEffectScale().getValue().floatValue();
        float i = this.client.player.getEffectFadeFactor(StatusEffects.DARKNESS, tickProgress) * h;
        float j = this.getDarkness(this.client.player, i, tickProgress) * h;

        float o = this.client.options.getGamma().getValue().floatValue();


        info = new LightmapInfo(world.getDimension().ambientLight(), g, this.flickerIntensity + 1.5f,
                world.getDimensionEffects().shouldBrightenLighting(), l, j, this.renderer.getSkyDarkness(tickProgress), Math.max(0.0F, o - i), skyColor, currentTick++);
    }

    @Override
    public LightmapInfo wakes$getLightmapInfo() {
        return info;
    }
}
