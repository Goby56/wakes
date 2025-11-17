package com.goby56.wakes.mixin;

import com.goby56.wakes.duck.LightmapAccess;
import com.goby56.wakes.render.LightmapInfo;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public abstract class LightmapTextureManagerMixin implements LightmapAccess {
    @Shadow private float blockLightRedFlicker;
    @Shadow @Final private Minecraft minecraft;

    @Unique
    private int currentTick;

    @Shadow protected abstract float calculateDarknessScale(LivingEntity entity, float factor, float tickProgress);

    @Shadow @Final private GameRenderer renderer;
    @Unique
    private LightmapInfo info;

    @Inject(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalInt;)Lcom/mojang/blaze3d/systems/RenderPass;"))
    private void wakes$onUpdate(float tickProgress, CallbackInfo ci, @Local ClientLevel world, @Local(ordinal = 1) Vector3f skyColor) {
        float f = world.getSkyDarken(1.0F);
        float g;
        if (world.getSkyFlashTime() > 0) {
            g = 1.0F;
        } else {
            g = f * 0.95F + 0.05F;
        }

        float k = this.minecraft.player.getWaterVision();

        float l;
        if (this.minecraft.player.hasEffect(MobEffects.NIGHT_VISION)) {
            l = GameRenderer.getNightVisionScale(this.minecraft.player, tickProgress);
        } else if (k > 0.0F && this.minecraft.player.hasEffect(MobEffects.CONDUIT_POWER)) {
            l = k;
        } else {
            l = 0.0F;
        }

        float h = this.minecraft.options.darknessEffectScale().get().floatValue();
        float i = this.minecraft.player.getEffectBlendFactor(MobEffects.DARKNESS, tickProgress) * h;
        float j = this.calculateDarknessScale(this.minecraft.player, i, tickProgress) * h;

        float o = this.minecraft.options.gamma().get().floatValue();


        info = new LightmapInfo(world.dimensionType().ambientLight(), g, this.blockLightRedFlicker + 1.5f,
                world.effects().constantAmbientLight(), l, j, this.renderer.getDarkenWorldAmount(tickProgress), Math.max(0.0F, o - i), skyColor, currentTick++);
    }

    @Override
    public LightmapInfo wakes$getLightmapInfo() {
        return info;
    }
}
