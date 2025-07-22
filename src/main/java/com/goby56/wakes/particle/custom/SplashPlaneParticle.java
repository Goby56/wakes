package com.goby56.wakes.particle.custom;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.particle.ModParticles;
import com.goby56.wakes.particle.WithOwnerParticleType;
import com.goby56.wakes.render.SplashPlaneRenderer;
import com.goby56.wakes.simulation.SimulationNode;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.utils.WakesUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.particle.*;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.util.Random;

public class SplashPlaneParticle extends Particle {
    public Entity owner;
    float yaw;
    float prevYaw;

    Vec3d direction = Vec3d.ZERO;

    private final SimulationNode simulationNode = new SimulationNode.SplashPlaneSimulation();

    public long imgPtr = -1;
    public int texRes;
    public boolean hasPopulatedPixels = false;

    public boolean isRenderReady = false;
    public float lerpedYaw = 0;


    protected SplashPlaneParticle(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
        initTexture(WakeHandler.resolution.res);
        WakeHandler.getInstance().ifPresent(wakeHandler -> wakeHandler.registerSplashPlane(this));
    }

    @Override
    public void markDead() {
        if (this.owner instanceof ProducesWake wakeOwner) {
            wakeOwner.wakes$setSplashPlane(null);
        }
        this.owner = null;
        this.deallocTexture();
        super.markDead();
    }

    @Override
    public void tick() {
        if (WakesConfig.disableMod || !WakesUtils.getEffectRuleFromSource(this.owner).renderPlanes) {
            this.markDead();
        }
        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;
        this.prevYaw = this.yaw;

        if (this.owner instanceof ProducesWake wakeOwner) {
            if (this.owner.isRemoved() || !wakeOwner.wakes$onFluidSurface() || wakeOwner.wakes$getHorizontalVelocity() < 1e-2) {
                this.markDead();
            } else {
                this.aliveTick(wakeOwner);
            }
        } else {
            this.markDead();
        }
    }

    private void aliveTick(ProducesWake wakeProducer) {
        // Vec3d vel = wakeProducer.wakes$getNumericalVelocity(); // UNCOMMENT IF WEIRD SPLASH BEHAVIOR
        Vec3d vel = this.owner.getVelocity();
        if (this.owner instanceof BoatEntity) {
            this.yaw = -this.owner.getYaw();
        } else {
            this.yaw = 90f - (float) (180f / Math.PI * Math.atan2(vel.z, vel.x));
        }
        this.direction = Vec3d.fromPolar(0, -this.yaw);
        Vec3d planeOffset = direction.multiply(this.owner.getWidth() + WakesConfig.splashPlaneOffset);
        Vec3d planePos = this.owner.getPos().add(planeOffset);
        this.setPos(planePos.x, wakeProducer.wakes$wakeHeight(), planePos.z);

        if (vel.length() / WakesConfig.maxSplashPlaneVelocity > 0.3f && WakesConfig.spawnParticles) {
            Random random = new Random();
            Vec3d particleOffset = new Vec3d(-direction.z, 0, direction.x).multiply(random.nextDouble() * this.owner.getWidth() / 4);
            Vec3d particlePos = this.owner.getPos().add(direction.multiply(this.owner.getWidth() - 0.3));
            Vec3d particleVelocity = Vec3d.fromPolar((float) (45 * random.nextDouble()), (float) (-this.yaw + 30 * (random.nextDouble() - 0.5f))).multiply(1.5 * vel.length());
            this.world.addParticleClient(ModParticles.SPLASH_CLOUD, particlePos.x + particleOffset.x, this.y, particlePos.z + particleOffset.z, particleVelocity.x, particleVelocity.y, particleVelocity.z);
            this.world.addParticleClient(ModParticles.SPLASH_CLOUD, particlePos.x - particleOffset.x, this.y, particlePos.z - particleOffset.z, particleVelocity.x, particleVelocity.y, particleVelocity.z);
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
        int fluidColor = BiomeColors.getWaterColor(world, this.owner.getBlockPos());
        int lightCol = WakesUtils.getLightColor(world, this.owner.getBlockPos());
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
        if (this.dead) return;
        if (MinecraftClient.getInstance().options.getPerspective().isFirstPerson() &&
                !WakesConfig.firstPersonSplashPlane &&
                this.owner instanceof ClientPlayerEntity) {
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

    public void translateMatrix(WorldRenderContext context, MatrixStack matrices) {
        Vec3d cameraPos = context.camera().getPos();
        float tickDelta = context.tickCounter().getTickProgress(true);
        float x = (float) (MathHelper.lerp(tickDelta, this.lastX, this.x) - cameraPos.getX());
        float y = (float) (MathHelper.lerp(tickDelta, this.lastY, this.y) - cameraPos.getY());
        float z = (float) (MathHelper.lerp(tickDelta, this.lastZ, this.z) - cameraPos.getZ());

        matrices.translate(x, y, z);
    }

    public Vec3d getPos() {
        return new Vec3d(x, y, z);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.CUSTOM;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<SimpleParticleType> {

        public Factory(SpriteProvider spriteSet) {
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientWorld world, double x, double y, double z, double velX, double velY, double velZ) {
            SplashPlaneParticle splashPlane = new SplashPlaneParticle(world, x, y, z);
            if (parameters instanceof WithOwnerParticleType type) {
                splashPlane.owner = type.owner;
                splashPlane.yaw = splashPlane.prevYaw = type.owner.getYaw();
                ((ProducesWake) splashPlane.owner).wakes$setSplashPlane(splashPlane);
            }
            return splashPlane;
        }
    }
}
