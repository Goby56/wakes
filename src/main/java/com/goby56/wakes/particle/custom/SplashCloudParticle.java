package com.goby56.wakes.particle.custom;

import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.particle.WithOwnerParticleType;
import com.goby56.wakes.simulation.WakeNode;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class SplashCloudParticle extends SpriteBillboardParticle {
    Entity owner;
    final double offset;
    final boolean isFromPaddles;

    public SplashCloudParticle(ClientWorld world, double x, double y, double z, SpriteProvider sprites, double velocityX, double velocityY, double velocityZ) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);

        this.prevPosX = x;
        this.prevPosY = y;
        this.prevPosZ = z;

        this.maxAge = WakeNode.maxAge;
        this.setSprite(sprites.getSprite(world.random));

        this.offset = velocityX;
        this.isFromPaddles = velocityX == 0;
        this.scale = isFromPaddles ? scale * 2 : 0.3f;
    }

    @Override
    public void tick() {
        if (this.owner == null || (this.isFromPaddles && this.age > maxAge)) {
            this.markDead();
            return;
        }
        this.alpha = 1f - (float) this.age / this.maxAge;
        if (this.isFromPaddles) {
            this.age++;
            return;
        }
        
        if (this.owner instanceof ProducesWake wake) {
            SplashPlaneParticle splashPlane = wake.getSplashPlane();
            if (splashPlane == null) {
                this.markDead();
                return;
            }

            this.prevPosX = this.x;
            this.prevPosY = this.y;
            this.prevPosZ = this.z;

            Vec3d particleOffset = new Vec3d(-splashPlane.direction.z, 0f, splashPlane.direction.x).multiply(this.offset * this.owner.getWidth() / 2f);
            Vec3d pos = splashPlane.getPos().add(particleOffset).add(splashPlane.direction.multiply(-0.2f));
            this.setPos(pos.x, pos.y, pos.z);
        }
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider spriteSet) {
            this.sprites = spriteSet;
        }

        @Nullable
        @Override
        public Particle createParticle(DefaultParticleType parameters, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            SplashCloudParticle cloud = new SplashCloudParticle(world, x, y, z, this.sprites, velocityX, velocityY, velocityZ);
            if (parameters instanceof WithOwnerParticleType type) {
                cloud.owner = type.owner;
            }
            return cloud;
        }
    }
}
