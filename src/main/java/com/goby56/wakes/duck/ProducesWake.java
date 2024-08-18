package com.goby56.wakes.duck;

import com.goby56.wakes.particle.custom.SplashPlaneParticle;
import net.minecraft.util.math.Vec3d;

public interface ProducesWake {
    boolean onWaterSurface();
    float producingHeight();
    void setProducingHeight(float h);
    Vec3d getPrevPos();
    void setPrevPos(Vec3d pos);
    Vec3d getNumericalVelocity();
    double getHorizontalVelocity();
    double getVerticalVelocity();
    void setSplashPlane(SplashPlaneParticle particle);

    void setRecentlyTeleported(boolean b);

    SplashPlaneParticle getSplashPlane();

}
