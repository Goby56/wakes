package com.goby56.wakes.particle.custom;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.particle.ModParticles;
import com.goby56.wakes.particle.WithOwnerParticleType;
import com.goby56.wakes.render.SplashPlaneRenderer;
import com.goby56.wakes.utils.WakesUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.Nullable;

public class SplashPlaneParticle extends Particle {
    Entity owner;
    float yaw;
    float prevYaw;

    Vec3d direction = Vec3d.ZERO;


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
            if (this.owner.isRemoved() || !wakeOwner.onWaterSurface() || wakeOwner.getHorizontalVelocity() < 1e-2) {
                this.markDead();
            } else {
                this.aliveTick(wakeOwner);
            }
        } else {
            this.markDead();
        }
    }

    private void aliveTick(ProducesWake wakeProducer) {
        if (this.owner instanceof BoatEntity) {
            this.yaw = -this.owner.getYaw();
        } else {
            Vec3d vel = wakeProducer.getNumericalVelocity();
            this.yaw = 90f - (float) (180f / Math.PI * Math.atan2(vel.z, vel.x));
        }
        this.direction = Vec3d.fromPolar(0, -this.yaw);
        Vec3d planeOffset = direction.multiply(this.owner.getWidth() + WakesClient.CONFIG_INSTANCE.splashPlaneOffset);
        Vec3d planePos = this.owner.getPos().add(planeOffset);
        this.setPos(planePos.x, wakeProducer.producingWaterLevel(), planePos.z);
    }

    @Override
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        if (this.dead) return;
        if (MinecraftClient.getInstance().options.getPerspective().isFirstPerson() &&
                !WakesClient.CONFIG_INSTANCE.firstPersonSplashPlane &&
                this.owner instanceof ClientPlayerEntity) {
            return;
        }

        MatrixStack modelMatrix = getMatrixStackFromCamera(camera, tickDelta);
        int light = this.getBrightness(tickDelta);

        float diff = this.yaw - this.prevYaw;
        if (diff > 180f) {
            diff -= 360;
        } else if (diff < -180f) {
            diff += 360;
        }

        float yawLerp = (this.prevYaw + diff * tickDelta) % 360f;

        modelMatrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawLerp + 180f));
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

    public Vec3d getPos() {
        return new Vec3d(x, y, z);
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
            SplashPlaneParticle splashPlane = new SplashPlaneParticle(world, x, y, z);
            if (parameters instanceof WithOwnerParticleType type) {
                splashPlane.owner = type.owner;
                splashPlane.yaw = splashPlane.prevYaw = type.owner.getYaw();
                ((ProducesWake) splashPlane.owner).setSplashPlane(splashPlane);
                if (type.owner instanceof BoatEntity) {
                    world.addParticle(ModParticles.SPLASH_CLOUD.withOwner(type.owner), x, y, z, 1, 0 ,0);
                    world.addParticle(ModParticles.SPLASH_CLOUD.withOwner(type.owner), x, y, z, -1, 0 ,0);
                }
            }
            return splashPlane;
        }
    }
}
