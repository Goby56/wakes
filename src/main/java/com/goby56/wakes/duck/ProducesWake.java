package com.goby56.wakes.duck;

import com.goby56.wakes.particle.custom.SplashPlaneParticle;
import net.minecraft.world.phys.Vec3;

public interface ProducesWake {
    boolean wakes$onFluidSurface();
    Float wakes$wakeHeight();
    void wakes$setWakeHeight(float h);
    Vec3 wakes$getPrevPos();
    void wakes$setPrevPos(Vec3 pos);
    Vec3 wakes$getNumericalVelocity();
    double wakes$getHorizontalVelocity();
    void wakes$setSplashPlane(SplashPlaneParticle particle);

    void wakes$setRecentlyTeleported(boolean b);

    SplashPlaneParticle wakes$getSplashPlane();

}
