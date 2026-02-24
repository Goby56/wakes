package com.goby56.wakes.mixin;

import com.goby56.wakes.duck.LightmapAccess;
import com.goby56.wakes.render.LightmapInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EndFlashState;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
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
    @Shadow @Final private GameRenderer renderer;
    @Shadow protected abstract float calculateDarknessScale(LivingEntity entity, float factor, float tickProgress);

    @Unique
    private int currentTick;

    @Unique
    private LightmapInfo info;

    @Inject(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalInt;)Lcom/mojang/blaze3d/systems/RenderPass;"))
    private void wakes$onUpdate(float tickProgress, CallbackInfo ci) {
        ClientLevel world = this.minecraft.level;
        if (world == null || this.minecraft.player == null) {
            return;
        }

        EnvironmentAttributeProbe probe = this.minecraft.gameRenderer.getMainCamera().attributeProbe();
        int skyLightColorRgb = ((Integer) probe.getValue(EnvironmentAttributes.SKY_LIGHT_COLOR, tickProgress)).intValue();
        float skyFactor = ((Float) probe.getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, tickProgress)).floatValue();

        EndFlashState endFlashState = world.endFlashState();
        Vector3f ambientColor;
        if (endFlashState != null) {
            ambientColor = new Vector3f(0.99f, 1.12f, 1.0f);
            if (!this.minecraft.options.hideLightningFlash().get()) {
                float intensity = endFlashState.getIntensity(tickProgress);
                if (this.minecraft.gui.getBossOverlay().shouldCreateWorldFog()) {
                    skyFactor += intensity / 3.0f;
                } else {
                    skyFactor += intensity;
                }
            }
        } else {
            ambientColor = new Vector3f(1.0f, 1.0f, 1.0f);
        }

        float darknessScaleSetting = this.minecraft.options.darknessEffectScale().get().floatValue();
        float darknessBlend = this.minecraft.player.getEffectBlendFactor(MobEffects.DARKNESS, tickProgress) * darknessScaleSetting;
        float darknessScale = this.calculateDarknessScale(this.minecraft.player, darknessBlend, tickProgress) * darknessScaleSetting;

        float waterVision = this.minecraft.player.getWaterVision();
        float nightVisionFactor;
        if (this.minecraft.player.hasEffect(MobEffects.NIGHT_VISION)) {
            nightVisionFactor = GameRenderer.getNightVisionScale(this.minecraft.player, tickProgress);
        } else if (waterVision > 0.0f && this.minecraft.player.hasEffect(MobEffects.CONDUIT_POWER)) {
            nightVisionFactor = waterVision;
        } else {
            nightVisionFactor = 0.0f;
        }

        float blockFactor = this.blockLightRedFlicker + 1.5f;
        float brightnessFactor = Math.max(0.0f, this.minecraft.options.gamma().get().floatValue() - darknessBlend);

        this.info = new LightmapInfo(
                world.dimensionType().ambientLight(),
                skyFactor,
                blockFactor,
                nightVisionFactor,
                darknessScale,
                this.renderer.getDarkenWorldAmount(tickProgress),
                brightnessFactor,
                ARGB.vector3fFromRGB24(skyLightColorRgb),
                ambientColor,
                currentTick++
        );
    }

    @Override
    public LightmapInfo wakes$getLightmapInfo() {
        return info;
    }
}
