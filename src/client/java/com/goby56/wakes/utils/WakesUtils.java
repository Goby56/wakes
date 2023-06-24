package com.goby56.wakes.utils;

import com.goby56.wakes.particle.ModParticles;
import com.goby56.wakes.particle.WakeParticleType;
import com.goby56.wakes.particle.custom.WakeParticle;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class WakesUtils {
    public static void spawnWake(World world, Entity owner) {
        WakeParticleType wake = ModParticles.WAKE.withOwner(owner);
        Vec3d pos = owner.getPos();
        world.addParticle(wake, pos.x, pos.y, pos.z, 0, 0, 0);
    }

    public static void spawnWakeNode(World world, Entity producer) {
        float height = getWaterLevel(world, producer);
        Vec3d pos = new Vec3d(producer.getX(), height, producer.getZ());
        WakeNode wakeNode = new WakeNode(pos, 60);
        WakeHandler.getInstance().insert(wakeNode);
    }

    public static float getWaterLevel(World world, Entity entityInWater) {
        // Taken from BoatEntity$getWaterLevelBelow
        Box box = entityInWater.getBoundingBox();
        int minX = MathHelper.floor(box.minX);
        int maxX = MathHelper.ceil(box.maxX);
        int minY = MathHelper.floor(box.minY);
        int maxY = MathHelper.ceil(box.maxY);
        int minZ = MathHelper.floor(box.minZ);
        int maxZ = MathHelper.ceil(box.maxZ);
        BlockPos.Mutable blockPos = new BlockPos.Mutable();

        yLoop:
        for (int y = minY; y < maxY; ++y) {
            float f = 0.0f;
            for (int x = minX; x < maxX; ++x) {
                for (int z = minZ; z < maxZ; ++z) {
                    blockPos.set(x, y, z);
                    FluidState fluidState = world.getFluidState(blockPos);
                    if (fluidState.isIn(FluidTags.WATER)) {
                        f = Math.max(f, fluidState.getHeight(world, blockPos));
                    }
                    if (f >= 1.0f) continue yLoop;
                }
            }
            if (!(f < 1.0f)) continue;
            return blockPos.getY() + f;
        }
        return maxY + 1;
    }
}
