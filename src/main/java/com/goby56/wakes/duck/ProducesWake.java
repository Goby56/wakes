package com.goby56.wakes.duck;

import net.minecraft.client.particle.Particle;
import net.minecraft.util.math.Vec3d;

public interface ProducesWake {
    boolean onWaterSurface();
    float producingHeight();
    Vec3d getPrevPos();
    void setPrevPos(Vec3d pos);
    double getHorizontalVelocity();
    double getVerticalVelocity();
    void setWakeParticle(Particle particle);

}
