package com.goby56.wakes.particle.custom;

import com.goby56.wakes.particle.WithOwnerParticleType;
import com.goby56.wakes.utils.WakesUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class SplashCloudParticle extends SpriteBillboardParticle {

    public SplashCloudParticle(ClientWorld world, double x, double y, double z, SpriteProvider sprites, double velocityX, double velocityY, double velocityZ) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);

        this.velocityX += 0.001f * (world.random.nextGaussian() - 0.5f);
        this.velocityY += 0.01f * Math.abs(world.random.nextGaussian() - 0.5f);
        this.velocityZ += 0.001f * (world.random.nextGaussian() - 0.5f);

        this.prevPosX = x;
        this.prevPosY = y;
        this.prevPosZ = z;

        this.maxAge = 15;
        this.scale *= 2;
        this.setSprite(sprites.getSprite(world.random));
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }
        this.alpha = 1f - (float) this.age / this.maxAge;

        Vec3d pos = new Vec3d(this.x, this.y, this.z);
        BlockState currBlock = this.world.getBlockState(WakesUtils.vecToBlockPos(pos));

        if (currBlock.isAir()) {
            this.velocityY -= 0.02f;
        } else if (currBlock.isOf(Blocks.WATER)) {
            this.velocityY += 0.01f;
        }
        this.velocityX *= 0.99f;
        this.velocityY *= 0.5f;
        this.velocityZ *= 0.99f;
        this.move(velocityX, velocityY, velocityZ);
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
            return new SplashCloudParticle(world, x, y, z, this.sprites, velocityX, velocityY, velocityZ);
        }
    }
}
