package com.goby56.wakes.particle.custom;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.particle.ModParticles;
import com.goby56.wakes.particle.WithOwnerParticleType;
import com.goby56.wakes.simulation.SimulationNode;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.utils.WakesUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.particle.*;
import net.minecraft.client.Camera;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.util.Random;

public class SplashPlaneParticle extends Particle {
    public Entity owner;
    float yaw;
    float prevYaw;

    Vec3 direction = Vec3.ZERO;

    private final SimulationNode simulationNode = new SimulationNode.SplashPlaneSimulation();

    public long imgPtr = -1;
    public int texRes;
    public boolean hasPopulatedPixels = false;

    public boolean isRenderReady = false;
    public float lerpedYaw = 0;


    protected SplashPlaneParticle(ClientLevel world, double x, double y, double z) {
        super(world, x, y, z);
        initTexture(WakeHandler.resolution.res);
        WakeHandler.getInstance().ifPresent(wakeHandler -> wakeHandler.registerSplashPlane(this));
    }

    @Override
    public void remove() {
        if (this.owner instanceof ProducesWake wakeOwner) {
            wakeOwner.wakes$setSplashPlane(null);
        }
        this.owner = null;
        this.deallocTexture();
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
        // Vec3d vel = wakeProducer.wakes$getNumericalVelocity(); // UNCOMMENT IF WEIRD SPLASH BEHAVIOR
        Vec3 vel = this.owner.getDeltaMovement();
        if (this.owner instanceof Boat) {
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
            this.level.addParticle(ModParticles.SPLASH_CLOUD, particlePos.x + particleOffset.x, this.y, particlePos.z + particleOffset.z, particleVelocity.x, particleVelocity.y, particleVelocity.z);
            this.level.addParticle(ModParticles.SPLASH_CLOUD, particlePos.x - particleOffset.x, this.y, particlePos.z - particleOffset.z, particleVelocity.x, particleVelocity.y, particleVelocity.z);
        }

        this.simulationNode.tick((float) wakeProducer.wakes$getHorizontalVelocity(), null, null, null, null);
        populatePixels();
    }

    public void initTexture(int res) {
        long size = 4L * res * res;
        if (imgPtr == -1) {
            imgPtr = MemoryUtil.nmemAlloc(size);
        } else {
            imgPtr = MemoryUtil.nmemRealloc(imgPtr, size);
        }

        this.texRes = res;
        this.hasPopulatedPixels = false;
    }

    public void deallocTexture() {
        MemoryUtil.nmemFree(imgPtr);
    }

    public void populatePixels() {
        int fluidColor = BiomeColors.getAverageWaterColor(level, this.owner.blockPosition());
        int lightCol = WakesUtils.getLightColor(level, this.owner.blockPosition());
        float opacity = WakesConfig.wakeOpacity * 0.9f;
        int res = WakeHandler.resolution.res;
        for (int r = 0; r < res; r++) {
            for (int c = 0; c < res; c++) {
                long pixelOffset = 4L * (((long) r * res) + c);
                MemoryUtil.memPutInt(imgPtr + pixelOffset, simulationNode.getPixelColor(c, r, fluidColor, lightCol, opacity));
            }
        }
        this.hasPopulatedPixels = true;
    }

    @Override
    public void render(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        this.isRenderReady = false;
        if (this.removed) return;
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson() &&
                !WakesConfig.firstPersonSplashPlane &&
                this.owner instanceof LocalPlayer) {
            return;
        }

        float diff = this.yaw - this.prevYaw;
        if (diff > 180f) {
            diff -= 360;
        } else if (diff < -180f) {
            diff += 360;
        }

        this.lerpedYaw = (this.prevYaw + diff * tickDelta) % 360f;
        this.isRenderReady = true;
    }

    public void translateMatrix(WorldRenderContext context, PoseStack matrices) {
        Vec3 cameraPos = context.camera().getPosition();
        float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(true);
        float x = (float) (Mth.lerp(tickDelta, this.xo, this.x) - cameraPos.x());
        float y = (float) (Mth.lerp(tickDelta, this.yo, this.y) - cameraPos.y());
        float z = (float) (Mth.lerp(tickDelta, this.zo, this.z) - cameraPos.z());

        matrices.translate(x, y, z);
    }

    public Vec3 getPos() {
        return new Vec3(x, y, z);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleProvider<SimpleParticleType> {

        public Factory(SpriteSet spriteSet) {
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientLevel world, double x, double y, double z, double velX, double velY, double velZ) {
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
