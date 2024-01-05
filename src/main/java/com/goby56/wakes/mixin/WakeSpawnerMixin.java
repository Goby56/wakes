package com.goby56.wakes.mixin;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.enums.EffectSpawningRule;
import com.goby56.wakes.particle.custom.SplashPlaneParticle;
import com.goby56.wakes.utils.WakesUtils;
import com.goby56.wakes.duck.ProducesWake;
import net.minecraft.entity.Entity;
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

	@Shadow public abstract boolean isSubmergedInWater();
	@Shadow public abstract String toString();
	@Shadow public abstract boolean isTouchingWater();
	@Shadow public abstract Vec3d getPos();
	@Shadow private Vec3d pos;
	@Shadow private World world;

	@Unique private boolean onWaterSurface = false;
	@Unique private Vec3d prevPosOnSurface = null;
	@Unique private Vec3d numericalVelocity = Vec3d.ZERO;
	@Unique private double horizontalNumericalVelocity = 0;
	@Unique private double verticalNumericalVelocity = 0;
	@Unique private Float producingWaterLevel = null;
	@Unique private SplashPlaneParticle splashPlane;

	@Override
	public boolean onWaterSurface() {
		return this.onWaterSurface;
	}

	@Override
	public Vec3d getNumericalVelocity() {
		return this.numericalVelocity;
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
		if (this.prevPosOnSurface == null) return null;
		return new Vec3d(this.prevPosOnSurface.x, this.prevPosOnSurface.y, this.prevPosOnSurface.z);
	}

	@Override
	public void setPrevPos(Vec3d pos) {
		this.prevPosOnSurface = pos;
	}

	@Override
	public float producingHeight() {
		return this.producingWaterLevel;
	}

	@Override
	public void setSplashPlane(SplashPlaneParticle particle) {
		this.splashPlane = particle;
	}

	@Inject(at = @At("HEAD"), method = "tick")
	private void tick(CallbackInfo info) {

		Vec3d vel = this.prevPosOnSurface == null ? Vec3d.ZERO : this.pos.subtract(this.prevPosOnSurface);
		this.numericalVelocity = vel;
		this.horizontalNumericalVelocity = vel.horizontalLength();
		this.verticalNumericalVelocity = vel.y;
		this.onWaterSurface = this.isTouchingWater() && !this.isSubmergedInWater();

		if (WakesClient.CONFIG_INSTANCE.disableMod) {
			return;
		}

		Entity thisEntity = ((Entity) (Object) this);

		// TODO IMPLEMENT ALL CONFIG CONDITIONAL CHECKS (BETTER AND MORE EXHAUSTIVE APPROACH)
		if (this.onWaterSurface) {
			if (this.producingWaterLevel == null)
				this.producingWaterLevel = WakesUtils.getWaterLevel(this.world, thisEntity);

			Vec3d currPos = new Vec3d(thisEntity.getX(), this.producingWaterLevel, thisEntity.getZ());

			this.spawnEffects(thisEntity);

			this.setPrevPos(currPos);
		} else {
			this.producingWaterLevel = null;
			this.prevPosOnSurface = null;
		}
	}

	@Inject(at = @At("TAIL"), method = "onSwimmingStart")
	private void onSwimmingStart(CallbackInfo ci) {
		if (WakesClient.CONFIG_INSTANCE.disableMod) {
			return;
		}

		Entity thisEntity = ((Entity) (Object) this);

		EffectSpawningRule rule = WakesUtils.getEffectRuleFromSource(thisEntity);
		if (rule.simulateWakes) {
			if (this.producingWaterLevel == null)
				this.producingWaterLevel = WakesUtils.getWaterLevel(this.world, thisEntity);
			WakesUtils.placeFallSplash(((Entity) (Object) this));
		}
		// TODO ADD WAKE WHEN GETTING OUT OF WATER
	}

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

}