package com.goby56.wakes.duck;

import net.minecraft.util.math.Vec3d;

public interface ProducesWake {
    boolean shouldSpawnWake();
    Vec3d getPrevPos();

}
