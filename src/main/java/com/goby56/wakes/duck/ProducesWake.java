package com.goby56.wakes.duck;

import com.goby56.wakes.particle.custom.SplashPlaneParticle;
import net.minecraft.util.math.Vec3d;

public interface ProducesWake {
    boolean wakes$onFluidSurface();
    Float wakes$wakeHeight();
    void wakes$setWakeHeight(float h);
    Vec3d wakes$getPrevPos();
    void wakes$setPrevPos(Vec3d pos);
    Vec3d wakes$getNumericalVelocity();
    double wakes$getHorizontalVelocity();
    void wakes$setSplashPlane(SplashPlaneParticle particle);

    void wakes$setRecentlyTeleported(boolean b);

    SplashPlaneParticle wakes$getSplashPlane();

}
