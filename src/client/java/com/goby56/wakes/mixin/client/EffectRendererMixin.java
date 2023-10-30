package com.goby56.wakes.mixin.client;

import com.goby56.wakes.render.FoamOutlineRenderer;
import com.goby56.wakes.render.SplashPlaneRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
@Debug(export = true)
public abstract class EffectRendererMixin {

    @Inject(at = @At("HEAD"), method = "render")
    private <T extends Entity> void renderWaterFX(T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
//        FoamOutlineRenderer.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
//        SplashPlaneRenderer.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}
