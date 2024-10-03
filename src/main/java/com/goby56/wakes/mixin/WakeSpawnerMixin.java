package com.goby56.wakes.mixin;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.config.enums.EffectSpawningRule;
import com.goby56.wakes.utils.WakesUtils;
import com.goby56.wakes.duck.ProducesWake;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
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

	@Shadow public abstract boolean isOnGround();

	@Shadow public abstract BlockState getSteppingBlockState();

	@Unique private boolean inInk = false;
	@Unique private Vec3d prevPosInInk = null;
	@Unique private Vec3d numericalVelocity = Vec3d.ZERO;
	@Unique private double horizontalNumericalVelocity = 0;
	@Unique private double verticalNumericalVelocity = 0;
	@Unique private Float producingYLevel = null;
	@Unique private boolean hasRecentlyTeleported = false;

	@Override
	public boolean onWaterSurface() {
		return this.inInk;
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
		return this.prevPosInInk;
	}

	@Override
	public void setPrevPos(Vec3d pos) {
		this.prevPosInInk = pos;
	}

	@Override
	public Float producingWaterLevel() {
		return this.producingYLevel;
	}

	@Override
	public void setProducingHeight(float h) {
		this.producingYLevel = h;
	}

	@Override
	public void setRecentlyTeleported(boolean b) {
		this.hasRecentlyTeleported = b;
	}

	@Inject(at = @At("TAIL"), method = "tick")
	private void tick(CallbackInfo info) {
		this.inInk = this.isOnGround() && this.getSteppingBlockState().isOf(Blocks.OBSIDIAN);
		Entity thisEntity = ((Entity) (Object) this);
		Vec3d vel = this.calculateVelocity(thisEntity);
		this.numericalVelocity = vel;
		this.horizontalNumericalVelocity = vel.horizontalLength();
		this.verticalNumericalVelocity = vel.y;

		if (WakesConfig.disableMod) {
			return;
		}

		if (this.inInk && !this.hasRecentlyTeleported) {
			this.producingYLevel = (float) thisEntity.getPos().y;

			Vec3d currPos = new Vec3d(thisEntity.getX(), this.producingYLevel, thisEntity.getZ());

			EffectSpawningRule rule = WakesUtils.getEffectRuleFromSource(thisEntity);
			if (rule.simulateWakes) {
				WakesUtils.placeWakeTrail(thisEntity);
			}

			this.setPrevPos(currPos);
		} else {
			this.producingYLevel = null;
			this.prevPosInInk = null;
		}
		this.setRecentlyTeleported(false);
	}

	// @Inject(at = @At("TAIL"), method = "onSwimmingStart")
	// private void onSwimmingStart(CallbackInfo ci) {
	// 	if (WakesClient.CONFIG_INSTANCE.disableMod) {
	// 		return;
	// 	}
	// 	Entity thisEntity = ((Entity) (Object) this);
	//
	// 	EffectSpawningRule rule = WakesUtils.getEffectRuleFromSource(thisEntity);
	// 	if (rule.simulateWakes) {
	// 		WakesUtils.placeFallSplash(((Entity) (Object) this));
	// 	}
	// }

	private Vec3d calculateVelocity(Entity thisEntity) {
		if (thisEntity instanceof ClientPlayerEntity) {
			return thisEntity.getVelocity();
		}
		return this.prevPosInInk == null ? Vec3d.ZERO : this.pos.subtract(this.prevPosInInk);
	}

}