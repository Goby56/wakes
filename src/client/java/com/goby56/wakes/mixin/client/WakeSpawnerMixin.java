package com.goby56.wakes.mixin.client;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.utils.WakesUtils;
import com.goby56.wakes.duck.ProducesWake;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
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

	@Shadow public abstract Vec3d getPos();


	@Shadow private Vec3d velocity;

	@Shadow public abstract String getEntityName();

	@Shadow public float horizontalSpeed;
	@Shadow public float prevHorizontalSpeed;
	@Shadow private Vec3d pos;
	@Shadow public double prevX;
	@Shadow public double prevY;
	@Shadow public double prevZ;

	@Shadow public abstract EntityType<?> getType();

	@Shadow public abstract boolean isPlayer();

	public boolean shouldSpawnWake = false;

	public boolean hasWake = false;

	public Vec3d prevWakeProdPos = null;

	public double horizontalNumericalVelocity = 0;
	public double verticalNumericalVelocity = 0;

	@Override
	public boolean shouldSpawnWake() {
		return this.shouldSpawnWake;
	}

	@Override
	public double getHorizontalVelocity() {
		return this.horizontalNumericalVelocity;
	}

	@Override
	public double getVerticalVelocity() {
		return this.verticalNumericalVelocity;
	}

	@Override
	public Vec3d getPrevPos() {
		if (this.prevWakeProdPos == null) return null;
		return new Vec3d(this.prevWakeProdPos.x, this.prevWakeProdPos.y, this.prevWakeProdPos.z);
	}

	@Override
	public void setPrevPos(Vec3d pos) {
		this.prevWakeProdPos = pos;
	}

	@Inject(at = @At("HEAD"), method = "move")
	private void tick(CallbackInfo info) {
		Vec3d vel = this.pos.subtract(this.prevX, this.prevY, this.prevZ);
		if (this.isPlayer()) {
			System.out.println(vel);
		}
		this.horizontalNumericalVelocity = vel.horizontalLength();
		this.verticalNumericalVelocity = vel.y;

		if (!WakesClient.CONFIG_INSTANCE.spawnWakes) {
			return;
		}

		if (this.isTouchingWater() && !this.isSubmergedInWater()) {
			if (WakesClient.CONFIG_INSTANCE.getSpawningRule(((Entity) (Object) this)).spawnsWake) {
				// TODO FUCKING NOT SHOWING FOR OTHER PLAYERS. MUST BE SOMETHING WRONG WITH VELOCITY. VELOCITY IS STUCK OR NOT CHANGING
				WakesUtils.placeWakeTrail(this.world, ((Entity) (Object) this));
			}
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
		if (!WakesClient.CONFIG_INSTANCE.spawnWakes) {
			return;
		}
		// TODO ADD WAKE WHEN GETTING OUT OF WATER
		WakesConfig.WakeSpawningRule spawningRule = WakesClient.CONFIG_INSTANCE.getSpawningRule(((Entity) (Object) this));
		if (spawningRule.spawnsSplashes) {
			WakesUtils.placeSingleSplash(this.world, ((Entity) (Object) this));
		}
	}
}