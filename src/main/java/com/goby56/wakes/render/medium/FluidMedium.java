package com.goby56.wakes.render.medium;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.utils.WakesUtils;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class FluidMedium extends WakeMedium {
    public final Fluid fluid;

    protected FluidMedium(Fluid fluid) {
        this.fluid = fluid;
    }

    @Override
    public boolean isInMedium(World world, Entity entity) {
        double hitboxMaxY = entity.getBoundingBox().maxY;
        BlockPos blockPos = BlockPos.ofFloored(entity.getX(), hitboxMaxY, entity.getZ());
        FluidState fluidState = world.getFluidState(blockPos);
        double fluidHeight = (float)blockPos.getY() + fluidState.getHeight(world, blockPos);
        return entity.isInFluid() && hitboxMaxY > fluidHeight;
    }

    @Override
    public boolean validMediumPos(World world, BlockPos blockPos) {
        FluidState fluidState = world.getFluidState(blockPos);
        FluidState fluidStateAbove = world.getFluidState(blockPos.up());
        if (fluidState.isStill() && fluidStateAbove.isEmpty()) {
            return fluidState.isStill();
        }
        return false;
    }

    @Override
    public void onEntityTick(ProducesWake entity) {
        if (entity.wakes$onFluidSurface() && !entity..hasRecentlyTeleported) {
            this.wakeHeight = WakesUtils.getFluidLevel(this.world, thisEntity);

            Vec3d currPos = new Vec3d(thisEntity.getX(), this.wakeHeight, thisEntity.getZ());

            this.spawnEffects(thisEntity);

            this.wakes$setPrevPos(currPos);
        } else {
            this.wakeHeight = null;
            this.prevPosOnSurface = null;
        }
        this.wakes$setRecentlyTeleported(false);
    }

    @Override
    public void onEntityLand(ProducesWake entity) {

    }
}
