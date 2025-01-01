package com.goby56.wakes.duck;

import com.goby56.wakes.particle.custom.SplashPlaneParticle;
import net.minecraft.util.math.Vec3d;

public interface ProducesWake {
    boolean wakes$onWaterSurface();
    Float wakes$producingWaterLevel();
    void wakes$setProducingHeight(float h);
    Vec3d wakes$getPrevPos();
    void wakes$setPrevPos(Vec3d pos);
    Vec3d wakes$getNumericalVelocity();
    double wakes$getHorizontalVelocity();
    double wakes$getVerticalVelocity();
    void wakes$setSplashPlane(SplashPlaneParticle particle);

    void setRecentlyTeleported(boolean b);

    SplashPlaneParticle wakes$getSplashPlane();

}
