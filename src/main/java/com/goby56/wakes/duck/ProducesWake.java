package com.goby56.wakes.duck;

import net.minecraft.util.math.Vec3d;

public interface ProducesWake {
    boolean onWaterSurface();
    Float producingWaterLevel();
    void setProducingHeight(float h);
    Vec3d getPrevPos();
    void setPrevPos(Vec3d pos);
    Vec3d getNumericalVelocity();
    double getHorizontalVelocity();
    double getVerticalVelocity();

    void setRecentlyTeleported(boolean b);
}
