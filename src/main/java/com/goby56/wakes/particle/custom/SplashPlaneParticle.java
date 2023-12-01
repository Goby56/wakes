package com.goby56.wakes.particle.custom;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.particle.SplashPlaneParticleType;
import com.goby56.wakes.render.SplashPlaneRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.Nullable;

public class SplashPlaneParticle extends Particle {
    RenderLayer wakeLayer;

    Entity owner;
    float yaw;
    float prevYaw;

    protected SplashPlaneParticle(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
        this.setMaxAge(60);
//        this.setBoundingBoxSpacing(3, 0);
        Identifier wakeTexture = new Identifier(WakesClient.MOD_ID, "textures/entity/wake_texture.png");
        this.wakeLayer = RenderLayer.getEntityTranslucent(wakeTexture);
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        this.prevYaw = this.yaw;


        if (this.owner == null) {
            this.markDead();
            return;
        }

        if (this.owner instanceof ProducesWake wakeOwner) {
            if (!wakeOwner.onWaterSurface() || (this.owner instanceof PlayerEntity player && player.isSpectator())
                || this.owner.getVelocity().horizontalLength() < 1e-2) {
                wakeOwner.setWakeParticle(null);
                this.owner = null;
            } else {
                Vec3d vel = this.owner.getVelocity();
                this.yaw = 90 - (float) (180 / Math.PI * Math.atan2(vel.z, vel.x));
                Vec3d ownerPos = this.owner.getPos().add(vel.normalize().multiply(this.owner.getWidth()));
                this.setPos(ownerPos.x, wakeOwner.producingHeight(), ownerPos.z);
            }
        } else {
            this.markDead();
        }
    }


    @Override
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        MatrixStack modelMatrix = getMatrixStackFromCamera(camera, tickDelta);
        int light = this.getBrightness(tickDelta);
        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer modelConsumer = immediate.getBuffer(wakeLayer);

        float yawLerp = MathHelper.lerp(tickDelta, this.prevYaw, this.yaw);

        modelMatrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawLerp + 180));
        SplashPlaneRenderer.render(this.owner, yawLerp, tickDelta, modelMatrix, light);
    }

    private MatrixStack getMatrixStackFromCamera(Camera camera, float tickDelta) {
        // Think it moves the matrix context smoothly to the camera
        // https://github.com/Ladysnake/Effective/blob/main/src/main/java/ladysnake/effective/particle/SplashParticle.java
        Vec3d cameraPos = camera.getPos();
        float x = (float) (MathHelper.lerp(tickDelta, this.prevPosX, this.x) - cameraPos.getX());
        float y = (float) (MathHelper.lerp(tickDelta, this.prevPosY, this.y) - cameraPos.getY());
        float z = (float) (MathHelper.lerp(tickDelta, this.prevPosZ, this.z) - cameraPos.getZ());

        MatrixStack matrixStack = new MatrixStack();
        matrixStack.translate(x, y, z);
        return matrixStack;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.CUSTOM;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {

        public Factory(SpriteProvider spriteSet) {
        }

        @Nullable
        @Override
        public Particle createParticle(DefaultParticleType parameters, ClientWorld world, double x, double y, double z, double velX, double velY, double velZ) {
            SplashPlaneParticle wake = new SplashPlaneParticle(world, x, y, z);
            if (parameters instanceof SplashPlaneParticleType type) {
                wake.owner = type.owner;
                wake.yaw = wake.prevYaw = type.owner.getYaw();
                ((ProducesWake) wake.owner).setWakeParticle(wake);
            }
            return wake;
        }
    }
}
