package com.goby56.wakes.mixin.client;

import com.goby56.wakes.utils.WakesUtils;
import com.goby56.wakes.duck.ProducesWake;
import net.minecraft.entity.Entity;
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

	public boolean shouldSpawnWake = false;

	public boolean hasWake = false;

	@Override
	public boolean shouldSpawnWake() {
		return this.shouldSpawnWake;
	}

	@Inject(at = @At("TAIL"), method = "tick")
	private void tick(CallbackInfo info) {
		// TODO CHECK IF VELOCITY IS HIGH ENOUGH
		this.shouldSpawnWake = this.isTouchingWater() && !this.isSubmergedInWater() && this.getVelocity().horizontalLength() > 0.2;
		if (this.shouldSpawnWake) {
			WakesUtils.spawnWakeNode(this.world, ((Entity) (Object) this));
		}
//		if (this.shouldSpawnWake && !this.hasWake) {
//			WakesUtils.spawnWake(this.world, ((Entity) (Object) this));
//		}
		this.hasWake = this.shouldSpawnWake;
	}
}