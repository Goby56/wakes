package com.goby56.wakes.mixin;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.config.enums.EffectSpawningRule;
import com.goby56.wakes.particle.custom.SplashPlaneParticle;
import com.goby56.wakes.utils.WakesUtils;
import com.goby56.wakes.duck.ProducesWake;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class WakeSpawnerMixin implements ProducesWake {

	@Shadow public abstract String toString();
	@Shadow private Vec3 position;
	@Shadow private Level level;

	@Shadow public abstract double getX();

	@Shadow public abstract AABB getBoundingBox();

	@Shadow public abstract double getZ();

	@Shadow public abstract boolean isInLiquid();

	@Shadow public abstract boolean isInWater();

	@Unique private boolean onFluidSurface = false;
	@Unique private Vec3 prevPosOnSurface = null;
	@Unique private Vec3 numericalVelocity = Vec3.ZERO;
	@Unique private double horizontalNumericalVelocity = 0;
	@Unique private Float wakeHeight = null;
	@Unique private SplashPlaneParticle splashPlane;
	@Unique private boolean hasRecentlyTeleported = false;

	@Override
	public boolean wakes$onFluidSurface() {
		return this.onFluidSurface;
	}

	@Override
	public Vec3 wakes$getNumericalVelocity() {
		return this.numericalVelocity;
	}
	@Override
	public double wakes$getHorizontalVelocity() {
		return this.horizontalNumericalVelocity;
	}

	@Override
	public Vec3 wakes$getPrevPos() {
		return this.prevPosOnSurface;
	}

	@Override
	public void wakes$setPrevPos(Vec3 pos) {
		this.prevPosOnSurface = pos;
	}

	@Override
	public Float wakes$wakeHeight() {
		return this.wakeHeight;
	}

	@Override
	public void wakes$setWakeHeight(float h) {
		this.wakeHeight = h;
	}

	@Override
	public void wakes$setSplashPlane(SplashPlaneParticle particle) {
		this.splashPlane = particle;
	}

	@Override
	public void wakes$setRecentlyTeleported(boolean b) {
		this.hasRecentlyTeleported = b;
	}

	@Override
	public SplashPlaneParticle wakes$getSplashPlane() {
		return this.splashPlane;
	}

	// TODO FIX PLAYER TELEPORTATION CAUSING LONG WAKES
//	@Inject(at = @At("TAIL"), method = "teleport(Lnet/minecraft/server/world/ServerWorld;DDDLjava/util/Set;FF)Z")
//	private void onTeleport(ServerWorld world, double destX, double destY, double destZ, Set<PositionFlag> flags, float yaw, float pitch, CallbackInfoReturnable<Boolean> cir) {
//		this.setRecentlyTeleported(true);
//		System.out.printf("%s wants to teleport\n", this);
//	}

	@Unique
	private boolean onFluidSurface() {
		double hitboxMaxY = this.getBoundingBox().maxY;
		BlockPos blockPos = BlockPos.containing(this.getX(), hitboxMaxY, this.getZ());
		FluidState fluidState = this.level.getFluidState(blockPos);
		double fluidHeight = (float)blockPos.getY() + fluidState.getHeight(this.level, blockPos);
		return this.isInWater() && hitboxMaxY > fluidHeight;
	}

	@Inject(at = @At("TAIL"), method = "tick")
	private void tick(CallbackInfo info) {
		this.onFluidSurface = onFluidSurface();
		Entity thisEntity = ((Entity) (Object) this);
		Vec3 vel = this.calculateVelocity(thisEntity);
		this.numericalVelocity = vel;
		this.horizontalNumericalVelocity = vel.horizontalDistance();

		if (WakesConfig.disableMod) {
			return;
		}

		if (this.onFluidSurface && !this.hasRecentlyTeleported) {
			this.wakeHeight = WakesUtils.getFluidLevel(this.level, thisEntity);

			Vec3 currPos = new Vec3(thisEntity.getX(), this.wakeHeight, thisEntity.getZ());

			this.spawnEffects(thisEntity);

			this.wakes$setPrevPos(currPos);
		} else {
			this.wakeHeight = null;
			this.prevPosOnSurface = null;
		}
		this.wakes$setRecentlyTeleported(false);
	}

	@Inject(at = @At("TAIL"), method = "doWaterSplashEffect")
	private void onSwimmingStart(CallbackInfo ci) {
		if (WakesConfig.disableMod) {
			return;
		}
		Entity thisEntity = ((Entity) (Object) this);

		EffectSpawningRule rule = WakesUtils.getEffectRuleFromSource(thisEntity);
		if (rule.simulateWakes) {
			if (this.wakeHeight == null)
				this.wakeHeight = WakesUtils.getFluidLevel(this.level, thisEntity);
			WakesUtils.placeFallSplash(((Entity) (Object) this));
		}
		// TODO ADD WAKE WHEN GETTING OUT OF WATER
	}

	@Unique
	private void spawnEffects(Entity thisEntity) {
		EffectSpawningRule rule = WakesUtils.getEffectRuleFromSource(thisEntity);
		if (rule.simulateWakes) {
			WakesUtils.placeWakeTrail(thisEntity);
		}
		if (rule.renderPlanes) {
			if (this.splashPlane == null && this.horizontalNumericalVelocity > 1e-2) {
				WakesUtils.spawnSplashPlane(this.level, thisEntity);
			}
		}
	}

	@Unique
	private Vec3 calculateVelocity(Entity thisEntity) {
		if (thisEntity instanceof LocalPlayer) {
			return thisEntity.getDeltaMovement();
		}
		return this.prevPosOnSurface == null ? Vec3.ZERO : this.position.subtract(this.prevPosOnSurface);
	}

}