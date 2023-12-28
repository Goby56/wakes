package com.goby56.wakes.particle;

import net.minecraft.entity.Entity;
import net.minecraft.particle.DefaultParticleType;

public class WithOwnerParticleType extends DefaultParticleType {
    public Entity owner;

    protected WithOwnerParticleType(boolean alwaysShow) {
        super(alwaysShow);
    }

    public WithOwnerParticleType withOwner(Entity owner) {
        this.owner = owner;
        return this;
    }
}
