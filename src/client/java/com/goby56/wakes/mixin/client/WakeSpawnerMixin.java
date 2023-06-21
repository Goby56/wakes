package com.goby56.wakes.mixin.client;

import com.goby56.wakes.WakesUtils;
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

	public boolean shouldSpawnWakes = false;

	public boolean hasWake = false;

	@Override
	public boolean getWakeSpawning() {
		return this.shouldSpawnWakes;
	}

	@Inject(at = @At("TAIL"), method = "tick")
	private void tick(CallbackInfo info) {
		// TODO CHECK IF VELOCITY IS HIGH ENOUGH
		this.shouldSpawnWakes = this.isTouchingWater() && !this.isSubmergedInWater() && this.getVelocity().horizontalLength() > 0.2;
		if (this.shouldSpawnWakes && !this.hasWake) {
			WakesUtils.spawnWake(this.world, ((Entity) (Object) this));
		}
		this.hasWake = this.shouldSpawnWakes;
	}
}