package com.goby56.wakes.render.medium;

import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.render.enums.WakeColor;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class WakeMedium {
    abstract public WakeColor getTintColor(World world, BlockPos blockPos);

    abstract boolean isInMedium(World world, Entity entity);

    abstract boolean validMediumPos(World world, BlockPos blockPos);

    abstract void onEntityTick(ProducesWake entity);

    abstract void onEntityLand(ProducesWake entity);
}
