package com.goby56.wakes.mixin;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.config.enums.EffectSpawningRule;
import com.goby56.wakes.particle.custom.SplashPlaneParticle;
import com.goby56.wakes.utils.WakesUtils;
import com.goby56.wakes.duck.ProducesWake;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class WakeSpawnerMixin implements ProducesWake {

	@Shadow public abstract String toString();
	@Shadow private Vec3d pos;
	@Shadow private World world;

	@Shadow public abstract double getX();

	@Shadow public abstract Box getBoundingBox();

	@Shadow public abstract double getZ();

	@Shadow public abstract boolean isTouchingWater();

	@Unique private boolean onFluidSurface = false;
	@Unique private Vec3d prevPosOnSurface = null;
	@Unique private Vec3d numericalVelocity = Vec3d.ZERO;
	@Unique private double horizontalNumericalVelocity = 0;
	@Unique private Float wakeHeight = null;
	@Unique private SplashPlaneParticle splashPlane;
	@Unique private boolean hasRecentlyTeleported = false;

	@Override
	public boolean wakes$onFluidSurface() {
		return this.onFluidSurface;
	}

	@Override
	public Vec3d wakes$getNumericalVelocity() {
		return this.numericalVelocity;
	}
	@Override
	public double wakes$getHorizontalVelocity() {
		return this.horizontalNumericalVelocity;
	}

	@Override
	public Vec3d wakes$getPrevPos() {
		return this.prevPosOnSurface;
	}

	@Override
	public void wakes$setPrevPos(Vec3d pos) {
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
		BlockPos blockPos = BlockPos.ofFloored(this.getX(), hitboxMaxY, this.getZ());
		FluidState fluidState = this.world.getFluidState(blockPos);
		double fluidHeight = (float)blockPos.getY() + fluidState.getHeight(this.world, blockPos);
		return this.isTouchingWater() && hitboxMaxY > fluidHeight;
	}

	@Inject(at = @At("TAIL"), method = "tick")
	private void tick(CallbackInfo info) {
		this.onFluidSurface = onFluidSurface();
		Entity thisEntity = ((Entity) (Object) this);
		Vec3d vel = this.calculateVelocity(thisEntity);
		this.numericalVelocity = vel;
		this.horizontalNumericalVelocity = vel.horizontalLength();

		if (WakesConfig.disableMod) {
			return;
		}

		if (this.onFluidSurface && !this.hasRecentlyTeleported) {
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

	@Inject(at = @At("TAIL"), method = "onSwimmingStart")
	private void onSwimmingStart(CallbackInfo ci) {
		if (WakesConfig.disableMod) {
			return;
		}
		Entity thisEntity = ((Entity) (Object) this);

		EffectSpawningRule rule = WakesUtils.getEffectRuleFromSource(thisEntity);
		if (rule.simulateWakes) {
			if (this.wakeHeight == null)
				this.wakeHeight = WakesUtils.getFluidLevel(this.world, thisEntity);
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
				WakesUtils.spawnSplashPlane(this.world, thisEntity);
			}
		}
	}

	@Unique
	private Vec3d calculateVelocity(Entity thisEntity) {
		if (thisEntity instanceof ClientPlayerEntity) {
			return thisEntity.getVelocity();
		}
		return this.prevPosOnSurface == null ? Vec3d.ZERO : this.pos.subtract(this.prevPosOnSurface);
	}

}