package com.goby56.wakes.particle.custom;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.particle.ModParticles;
import com.goby56.wakes.particle.WithOwnerParticleType;
import com.goby56.wakes.render.WakeTextureAtlas;
import com.goby56.wakes.simulation.SimulationNode;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.utils.WakesUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.BiomeColors;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class SplashPlaneParticle extends Particle {
    public Entity owner;
    float yaw;
    float prevYaw;

    Vec3 direction = Vec3.ZERO;

    private SimulationNode simulationNode = new SimulationNode.SplashPlaneSimulation();

    public WakeTextureAtlas.DrawContext drawContext;

    public float lerpedYaw = 0;

    protected SplashPlaneParticle(ClientLevel world, double x, double y, double z) {
        super(world, x, y, z);
        WakeHandler.getInstance().ifPresent(wakeHandler -> {
            this.drawContext = wakeHandler.getTextureAtlas().claimSubTexture();
            wakeHandler.registerSplashPlane(this);
        });
    }

    @Override
    public void remove() {
        if (this.owner instanceof ProducesWake wakeOwner) {
            wakeOwner.wakes$setSplashPlane(null);
        }
        this.owner = null;
        if (this.drawContext != null) this.drawContext.invalidate();
        super.remove();
    }

    @Override
    public void tick() {
        if (WakesConfig.disableMod || !WakesUtils.getEffectRuleFromSource(this.owner).renderPlanes) {
            this.remove();
        }
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.prevYaw = this.yaw;

        if (this.owner instanceof ProducesWake wakeOwner) {
            if (this.owner.isRemoved() || !wakeOwner.wakes$onFluidSurface() || wakeOwner.wakes$getHorizontalVelocity() < 1e-2) {
                this.remove();
            } else {
                this.aliveTick(wakeOwner);
            }
        } else {
            this.remove();
        }
    }

    private void aliveTick(ProducesWake wakeProducer) {
        Vec3 vel = this.owner.getDeltaMovement();
        if (this.owner instanceof AbstractBoat) {
            this.yaw = -this.owner.getYRot();
        } else {
            this.yaw = 90f - (float) (180f / Math.PI * Math.atan2(vel.z, vel.x));
        }
        this.direction = Vec3.directionFromRotation(0, -this.yaw);
        Vec3 planeOffset = direction.scale(this.owner.getBbWidth() + WakesConfig.splashPlaneOffset);
        Vec3 planePos = this.owner.position().add(planeOffset);
        this.setPos(planePos.x, wakeProducer.wakes$wakeHeight(), planePos.z);

        if (vel.length() / WakesConfig.maxSplashPlaneVelocity > 0.3f && WakesConfig.spawnParticles) {
            Random random = new Random();
            Vec3 particleOffset = new Vec3(-direction.z, 0, direction.x).scale(random.nextDouble() * this.owner.getBbWidth() / 4);
            Vec3 particlePos = this.owner.position().add(direction.scale(this.owner.getBbWidth() - 0.3));
            Vec3 particleVelocity = Vec3.directionFromRotation((float) (45 * random.nextDouble()), (float) (-this.yaw + 30 * (random.nextDouble() - 0.5f))).scale(1.5 * vel.length());
            this.level.addParticle(ModParticles.SPLASH_CLOUD.withOwner(this.owner), particlePos.x + particleOffset.x, this.y, particlePos.z + particleOffset.z, particleVelocity.x, particleVelocity.y, particleVelocity.z);
            this.level.addParticle(ModParticles.SPLASH_CLOUD.withOwner(this.owner), particlePos.x - particleOffset.x, this.y, particlePos.z - particleOffset.z, particleVelocity.x, particleVelocity.y, particleVelocity.z);
        }

        // If resolution changed, release the old atlas slot and claim a new one sized correctly.
        if (this.simulationNode.res != WakeHandler.resolution.res) {
            if (this.drawContext != null) this.drawContext.invalidate();
            WakeHandler.getInstance().ifPresent(wh -> this.drawContext = wh.getTextureAtlas().claimSubTexture());
            this.simulationNode = new SimulationNode.SplashPlaneSimulation();
        }

        this.simulationNode.tick((float) wakeProducer.wakes$getHorizontalVelocity(), null, null, null, null);
        populatePixels();
    }

    public void populatePixels() {
        if (this.drawContext == null) return;
        int res = WakeHandler.resolution.res;
        int fluidColor = BiomeColors.getAverageWaterColor(level, this.owner.blockPosition());
        float opacity = WakesConfig.wakeOpacity * 0.9f;
        for (int r = 0; r < res; r++) {
            for (int c = 0; c < res; c++) {
                int color = simulationNode.getPixelColor(c, r, fluidColor, opacity);
                this.drawContext.draw(c, r, color);
            }
        }
    }


    public void updateYaw(float tickDelta) {
        float diff = this.yaw - this.prevYaw;
        if (diff > 180f) {
            diff -= 360;
        } else if (diff < -180f) {
            diff += 360;
        }

        this.lerpedYaw = (this.prevYaw + diff * tickDelta) % 360f;
    }

    public void translateMatrix(Camera camera, PoseStack matrices) {
        Vec3 cameraPos = camera.position();
        float tickDelta = camera.getCameraEntityPartialTicks(net.minecraft.client.Minecraft.getInstance().getDeltaTracker());
        float x = (float) (Mth.lerp(tickDelta, this.xo, this.x) - cameraPos.x());
        float y = (float) (Mth.lerp(tickDelta, this.yo, this.y) - cameraPos.y());
        float z = (float) (Mth.lerp(tickDelta, this.zo, this.z) - cameraPos.z());

        matrices.translate(x, y, z);
    }

    public Vec3 getPos() {
        return new Vec3(x, y, z);
    }

    @Override
    public ParticleRenderType getGroup() {
        return ParticleRenderType.NO_RENDER;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleProvider<SimpleParticleType> {

        public Factory(SpriteSet spriteSet) {
        }

        @Override
        public @Nullable Particle createParticle(SimpleParticleType parameters, ClientLevel world, double x, double y, double z, double velX, double velY, double velZ, RandomSource random) {
            SplashPlaneParticle splashPlane = new SplashPlaneParticle(world, x, y, z);
            if (parameters instanceof WithOwnerParticleType type) {
                splashPlane.owner = type.owner;
                splashPlane.yaw = splashPlane.prevYaw = type.owner.getYRot();
                ((ProducesWake) splashPlane.owner).wakes$setSplashPlane(splashPlane);
            }
            return splashPlane;
        }
    }
}
