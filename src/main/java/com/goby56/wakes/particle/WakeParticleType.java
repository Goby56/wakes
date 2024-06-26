package com.goby56.wakes.particle;

import net.minecraft.entity.Entity;
import net.minecraft.particle.SimpleParticleType;

public class WakeParticleType extends SimpleParticleType {
    public Entity owner;

    protected WakeParticleType(boolean alwaysShow) {
        super(alwaysShow);
    }

    public WakeParticleType withOwner(Entity owner) {
        this.owner = owner;
        return this;
    }
}
