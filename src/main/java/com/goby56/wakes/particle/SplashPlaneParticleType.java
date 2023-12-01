package com.goby56.wakes.particle;

import net.minecraft.entity.Entity;
import net.minecraft.particle.DefaultParticleType;

public class SplashPlaneParticleType extends DefaultParticleType {
    public Entity owner;

    protected SplashPlaneParticleType(boolean alwaysShow) {
        super(alwaysShow);
    }

    public SplashPlaneParticleType withOwner(Entity owner) {
        this.owner = owner;
        return this;
    }
}
