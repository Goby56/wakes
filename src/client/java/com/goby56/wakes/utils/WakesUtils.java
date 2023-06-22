package com.goby56.wakes.utils;

import com.goby56.wakes.particle.ModParticles;
import com.goby56.wakes.particle.WakeParticleType;
import com.goby56.wakes.particle.custom.WakeParticle;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class WakesUtils {
    public static void spawnWake(World world, Entity owner) {
        WakeParticleType wake = ModParticles.WAKE.withOwner(owner);
        Vec3d pos = owner.getPos();
        world.addParticle(wake, pos.x, pos.y, pos.z, 0, 0, 0);
    }
}
