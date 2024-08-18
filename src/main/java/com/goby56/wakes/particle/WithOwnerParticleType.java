package com.goby56.wakes.particle;

import net.minecraft.entity.Entity;
import net.minecraft.particle.SimpleParticleType;

public class WithOwnerParticleType extends SimpleParticleType {
    public Entity owner;

    protected WithOwnerParticleType(boolean alwaysShow) {
        super(alwaysShow);
    }

    public WithOwnerParticleType withOwner(Entity owner) {
        this.owner = owner;
        return this;
    }
}
