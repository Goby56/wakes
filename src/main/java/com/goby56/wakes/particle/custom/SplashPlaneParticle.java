package com.goby56.wakes.particle.custom;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.particle.ModParticles;
import com.goby56.wakes.particle.WithOwnerParticleType;
import com.goby56.wakes.render.SplashPlaneRenderer;
import com.goby56.wakes.utils.WakesUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.Nullable;

public class SplashPlaneParticle extends Particle {
    Entity owner;
    float yaw;
    float prevYaw;
    int ticksSinceSplash = 0;

    protected SplashPlaneParticle(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    public void markDead() {
        if (this.owner instanceof ProducesWake wakeOwner) {
            wakeOwner.setSplashPlane(null);
        }
        this.owner = null;
        super.markDead();
    }

    @Override
    public void tick() {
        if (WakesClient.CONFIG_INSTANCE.disableMod || !WakesUtils.getEffectRuleFromSource(this.owner).renderPlanes) {
            this.markDead();
        }
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        this.prevYaw = this.yaw;

        if (this.owner instanceof ProducesWake wakeOwner) {
            if (!wakeOwner.onWaterSurface() || wakeOwner.getHorizontalVelocity() < 1e-2) {
                this.markDead();
            } else {
                this.aliveTick(wakeOwner);
            }
        } else {
            this.markDead();
        }
    }

    private void aliveTick(ProducesWake wakeProducer) {
        this.ticksSinceSplash++;

        Vec3d vel = wakeProducer.getNumericalVelocity();
        // TODO FIX GLITCHY PLANES AT YAW = 90
        this.yaw = 90 - (float) (180 / Math.PI * Math.atan2(vel.z, vel.x));
        Vec3d normVel = vel.normalize();
        Vec3d planePos = this.owner.getPos().add(normVel.multiply(this.owner.getWidth() + WakesClient.CONFIG_INSTANCE.splashPlaneOffset));
        this.setPos(planePos.x, wakeProducer.producingHeight(), planePos.z);

        // TODO ADD DISTANCING OFFSET BETWEEN PLANES
        int t = (int) Math.floor(WakesClient.CONFIG_INSTANCE.maxSplashPlaneVelocity / vel.horizontalLength());
        if (this.ticksSinceSplash > t && WakesClient.CONFIG_INSTANCE.spawnParticles) {
            this.ticksSinceSplash = 0;
            Vec3d particlePos = planePos.subtract(new Vec3d(normVel.x, 0f, normVel.z).multiply(this.owner.getWidth() / 2f));
            Vec3d particleOffset = new Vec3d(-normVel.z, 0f, normVel.x).multiply(this.owner.getWidth() / 2f);
            Vec3d pos = particlePos.add(particleOffset);
            world.addParticle(ModParticles.SPLASH_CLOUD, pos.x, wakeProducer.producingHeight(), pos.z, vel.x, vel.y ,vel.z);
            pos = particlePos.add(particleOffset.multiply(-1f));
            world.addParticle(ModParticles.SPLASH_CLOUD, pos.x, wakeProducer.producingHeight(), pos.z, vel.x, vel.y ,vel.z);
        }
    }

    @Override
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        if (this.dead) return;
        MatrixStack modelMatrix = getMatrixStackFromCamera(camera, tickDelta);
        int light = this.getBrightness(tickDelta);

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
            if (parameters instanceof WithOwnerParticleType type) {
                wake.owner = type.owner;
                wake.yaw = wake.prevYaw = type.owner.getYaw();
                ((ProducesWake) wake.owner).setSplashPlane(wake);
            }
            return wake;
        }
    }
}
