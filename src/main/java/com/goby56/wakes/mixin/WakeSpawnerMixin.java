package com.goby56.wakes.mixin;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.enums.EffectSpawningRule;
import com.goby56.wakes.particle.custom.SplashPlaneParticle;
import com.goby56.wakes.utils.WakesUtils;
import com.goby56.wakes.duck.ProducesWake;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(Entity.class)
public abstract class WakeSpawnerMixin implements ProducesWake {

	@Shadow public abstract boolean isSubmergedInWater();
	@Shadow public abstract String toString();
	@Shadow public abstract boolean isTouchingWater();
	@Shadow private Vec3d pos;
	@Shadow private World world;

	@Unique private boolean onWaterSurface = false;
	@Unique private Vec3d prevPosOnSurface = null;
	@Unique private Vec3d numericalVelocity = Vec3d.ZERO;
	@Unique private double horizontalNumericalVelocity = 0;
	@Unique private double verticalNumericalVelocity = 0;
	@Unique private Float producingWaterLevel = null;
	@Unique private SplashPlaneParticle splashPlane;
	@Unique private boolean hasRecentlyTeleported = false;

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
		return this.prevPosOnSurface;
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

	@Override
	public void setRecentlyTeleported(boolean b) {
		this.hasRecentlyTeleported = b;
	}

	// TODO FIX PLAYER TELEPORTATION CAUSING LONG WAKES
//	@Inject(at = @At("TAIL"), method = "teleport(Lnet/minecraft/server/world/ServerWorld;DDDLjava/util/Set;FF)Z")
//	private void onTeleport(ServerWorld world, double destX, double destY, double destZ, Set<PositionFlag> flags, float yaw, float pitch, CallbackInfoReturnable<Boolean> cir) {
//		this.setRecentlyTeleported(true);
//		System.out.printf("%s wants to teleport\n", this);
//	}

	@Inject(at = @At("TAIL"), method = "tick")
	private void tick(CallbackInfo info) {
		this.onWaterSurface = this.isTouchingWater() && !this.isSubmergedInWater();
		Entity thisEntity = ((Entity) (Object) this);
		Vec3d vel = this.calculateVelocity(thisEntity);
		this.numericalVelocity = vel;
		this.horizontalNumericalVelocity = vel.horizontalLength();
		this.verticalNumericalVelocity = vel.y;

		if (WakesClient.CONFIG_INSTANCE.disableMod) {
			return;
		}

		if (this.onWaterSurface && !this.hasRecentlyTeleported) {
			if (this.producingWaterLevel == null)
				this.producingWaterLevel = WakesUtils.getWaterLevel(this.world, thisEntity);

			Vec3d currPos = new Vec3d(thisEntity.getX(), this.producingWaterLevel, thisEntity.getZ());

			this.spawnEffects(thisEntity);

			this.setPrevPos(currPos);
		} else {
			this.producingWaterLevel = null;
			this.prevPosOnSurface = null;
		}
		this.setRecentlyTeleported(false);
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

	private Vec3d calculateVelocity(Entity thisEntity) {
		if (thisEntity instanceof ClientPlayerEntity) {
			return thisEntity.getVelocity();
		}
		return this.prevPosOnSurface == null ? Vec3d.ZERO : this.pos.subtract(this.prevPosOnSurface);
	}

}