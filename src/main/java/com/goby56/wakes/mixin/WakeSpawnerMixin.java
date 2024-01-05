package com.goby56.wakes.mixin;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.enums.EffectSpawningRule;
import com.goby56.wakes.utils.WakesUtils;
import com.goby56.wakes.duck.ProducesWake;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class WakeSpawnerMixin implements ProducesWake {

	private Particle wakeParticle;

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

	@Shadow public float fallDistance;

	public boolean onWaterSurface = false;

	public boolean hasWake = false;

	public Vec3d prevWakeProdPos = null;

	public double horizontalNumericalVelocity = 0;
	public double verticalNumericalVelocity = 0;

	public Float producingWaterLevel = null;

	@Override
	public boolean onWaterSurface() {
		return this.onWaterSurface;
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

	@Override
	public float producingHeight() {
		return this.producingWaterLevel;
	}

	@Override
	public void setWakeParticle(Particle particle) {
		this.wakeParticle = particle;
	}

	@Inject(at = @At("HEAD"), method = "tick")
	private void tick(CallbackInfo info) {
		Vec3d vel = this.prevWakeProdPos == null ? Vec3d.ZERO : this.pos.subtract(this.prevWakeProdPos);
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

			EffectSpawningRule rule = WakesUtils.getEffectRuleFromSource(thisEntity);
			if (rule.simulateWakes) {
				WakesUtils.placeWakeTrail(thisEntity);
			} else if (rule.renderPlanes) {
				if (this.wakeParticle == null && vel.horizontalLength() > 1e-2) {
					WakesUtils.spawnSplashPlane(this.world, thisEntity);
				}
			} else {
				this.prevWakeProdPos = null;
			}
		} else {
			this.producingWaterLevel = null;
			this.prevWakeProdPos = null;
		}
//		if (this.shouldSpawnWake && !this.hasWake) {
//			WakesUtils.spawnWake(this.world, ((Entity) (Object) this));
//		}
//		this.hasWake = this.shouldSpawnWake;
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
}