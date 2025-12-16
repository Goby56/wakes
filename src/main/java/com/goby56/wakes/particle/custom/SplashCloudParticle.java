package com.goby56.wakes.particle.custom;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.particle.WithOwnerParticleType;
import com.goby56.wakes.simulation.WakeNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SplashCloudParticle extends SingleQuadParticle {
    Entity owner;
    final double offset;
    final boolean isFromPaddles;

    public SplashCloudParticle(ClientLevel world, Entity owner, double x, double y, double z, SpriteSet sprites, double velocityX, double velocityY, double velocityZ) {
        super(world, x, y, z, velocityX, velocityY, velocityZ, sprites.first());
        this.owner = owner;
        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;

        this.xo = x;
        this.yo = y;
        this.zo = z;

        this.isFromPaddles = velocityX == 0;

        this.lifetime = this.isFromPaddles ? (int) (WakeNode.maxAge * 1.5) : WakeNode.maxAge / 3;

        this.setSprite(sprites.get(world.random));

        this.offset = velocityX;
        this.quadSize = isFromPaddles ? quadSize * 2 : 0.3f;

        this.alpha = getAlpha();
    }

    public float getAlpha() {
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson() &&
                !WakesConfig.firstPersonEffects && this.owner instanceof LocalPlayer) {
            return 0;
        }
        return 1f - (float) this.age / this.lifetime;
    }

    @Override
    public void tick() {
        this.age++;
        if (this.age > this.lifetime) {
            this.remove();
            return;
        }

        this.alpha = getAlpha();

        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (level.getFluidState(new BlockPos((int) this.x, (int) this.y, (int) this.z)).is(Fluids.WATER)) {
            this.yd = 0.1;
            this.xd *= 0.92;
            this.yd *= 0.92;
            this.zd *= 0.92;
        } else {
            this.yd -= 0.05;

            this.xd *= 0.95;
            this.yd *= 0.95;
            this.zd *= 0.95;
        }

        this.x += xd;
        this.y += yd;
        this.z += zd;
        this.setPos(this.x, this.y, this.z);

    }

    @Override
    protected @NotNull Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Factory(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        @Override
        public @Nullable Particle createParticle(SimpleParticleType parameters, ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, RandomSource random) {
            if (parameters instanceof WithOwnerParticleType type) {
                return new SplashCloudParticle(world, type.owner, x, y, z, this.sprites, velocityX, velocityY, velocityZ);
            }
            return null;
        }
    }
}
