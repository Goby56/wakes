package com.goby56.wakes.particle.custom;

import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.particle.WithOwnerParticleType;
import com.goby56.wakes.simulation.WakeNode;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.Fluids;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class SplashCloudParticle extends SpriteBillboardParticle {
    Entity owner;
    final double offset;
    final boolean isFromPaddles;

    public SplashCloudParticle(ClientWorld world, double x, double y, double z, SpriteProvider sprites, double velocityX, double velocityY, double velocityZ) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;

        this.prevPosX = x;
        this.prevPosY = y;
        this.prevPosZ = z;

        this.maxAge = (int) (WakeNode.maxAge * 1.5);
        this.setSprite(sprites.getSprite(world.random));

        this.offset = velocityX;
        this.isFromPaddles = velocityX == 0;
        this.scale = isFromPaddles ? scale * 2 : 0.3f;
    }

    @Override
    public void tick() {
        this.age++;
        if (this.isFromPaddles) {
            if (this.age > maxAge) {
                this.markDead();
                return;
            }
            this.alpha = 1f - (float) this.age / this.maxAge;
            return;
        } else {
            if (this.age > maxAge / 3) {
                this.markDead();
                return;
            }
            this.alpha = 1f - (float) this.age / (this.maxAge / 3f);
        }

        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;

        if (world.getFluidState(new BlockPos((int) this.x, (int) this.y, (int) this.z)).isOf(Fluids.WATER)) {
            this.velocityY = 0.1;
            this.velocityX *= 0.92;
            this.velocityY *= 0.92;
            this.velocityZ *= 0.92;
        } else {
            this.velocityY -= 0.05;

            this.velocityX *= 0.95;
            this.velocityY *= 0.95;
            this.velocityZ *= 0.95;
        }

        this.x += velocityX;
        this.y += velocityY;
        this.z += velocityZ;
        this.setPos(this.x, this.y, this.z);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider spriteSet) {
            this.sprites = spriteSet;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            SplashCloudParticle cloud = new SplashCloudParticle(world, x, y, z, this.sprites, velocityX, velocityY, velocityZ);
            if (parameters instanceof WithOwnerParticleType type) {
                cloud.owner = type.owner;
            }
            return cloud;
        }
    }
}
