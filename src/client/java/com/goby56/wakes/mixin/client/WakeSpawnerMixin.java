package com.goby56.wakes.mixin.client;

import com.goby56.wakes.utils.WakesUtils;
import com.goby56.wakes.duck.ProducesWake;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class WakeSpawnerMixin implements ProducesWake {

	@Shadow public abstract boolean isSubmergedInWater();

	@Shadow public abstract String toString();

	@Shadow public abstract boolean isTouchingWater();

	@Shadow private World world;

	@Shadow public abstract Vec3d getVelocity();

	@Shadow public abstract Vec3d getPos();

	public boolean shouldSpawnWake = false;

	public boolean hasWake = false;

	public Vec3d prevWakeProdPos = null;

	@Override
	public boolean shouldSpawnWake() {
		return this.shouldSpawnWake;
	}

	@Override
	public Vec3d getPrevPos() {
		return this.prevWakeProdPos;
	}

	@Inject(at = @At("TAIL"), method = "tick")
	private void tick(CallbackInfo info) {
		this.shouldSpawnWake = this.isTouchingWater() && !this.isSubmergedInWater();
		if (this.shouldSpawnWake) {
			WakesUtils.placeWakeTrail(this.world, ((Entity) (Object) this));
			this.prevWakeProdPos = this.getPos();
		} else {
			this.prevWakeProdPos = null;
		}
//		if (this.shouldSpawnWake && !this.hasWake) {
//			WakesUtils.spawnWake(this.world, ((Entity) (Object) this));
//		}
//		this.hasWake = this.shouldSpawnWake;
	}

	@Inject(at = @At("TAIL"), method = "onSwimmingStart")
	private void onSwimmingStart(CallbackInfo ci) {
		// TODO ADD WAKE WHEN GETTING OUT OF WATER
		WakesUtils.placeSingleSplash(this.world, ((Entity) (Object) this));
	}
}