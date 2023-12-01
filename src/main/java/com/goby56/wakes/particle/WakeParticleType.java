package com.goby56.wakes.particle;

import com.goby56.wakes.utils.WakeNode;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DefaultParticleType;

public class WakeParticleType extends DefaultParticleType {
    public WakeNode node;

    protected WakeParticleType(boolean alwaysShow) {
        super(alwaysShow);
    }

    public WakeParticleType withNode(WakeNode node) {
        this.node = node;
        return this;
    }
}
