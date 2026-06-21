package com.goby56.wakes.utils;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.config.enums.EffectSpawningRule;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.particle.ModParticles;
import com.goby56.wakes.particle.WithOwnerParticleType;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class WakesUtils {

    /**
     * Returns an ARGB light color for the given position, sampling real block + sky light
     * (day/night aware). Block light is given a warm tint; sky light is neutral. A small
     * ambient floor keeps wakes from going fully black. This is multiplied into the wake
     * pixel color so wakes darken in caves / at night and warm up near light sources.
     */
    public static int getLightColor(Level world, BlockPos blockPos) {
        if (world == null) {
            return 0xFFFFFFFF;
        }
        net.minecraft.world.level.dimension.DimensionType dim = world.dimensionType();
        int blockLevel = world.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, blockPos);
        int skyLevel = Math.max(0, world.getBrightness(net.minecraft.world.level.LightLayer.SKY, blockPos) - world.getSkyDarken());

        float blockB = net.minecraft.client.renderer.Lightmap.getBrightness(dim, blockLevel);
        float skyB = net.minecraft.client.renderer.Lightmap.getBrightness(dim, skyLevel);

        // Warm block light, neutral sky light, with an ambient floor so wakes stay
        // visible at night / in caves rather than fading to black.
        float floor = 0.2f;
        float r = Mth.clamp(blockB * 1.00f + skyB + floor, 0f, 1f);
        float g = Mth.clamp(blockB * 0.92f + skyB + floor, 0f, 1f);
        float b = Mth.clamp(blockB * 0.80f + skyB + floor, 0f, 1f);

        int ri = (int) (r * 255f);
        int gi = (int) (g * 255f);
        int bi = (int) (b * 255f);
        return 0xFF000000 | ri << 16 | gi << 8 | bi;
    }

    /** Bilinearly interpolates four packed ARGB light colors. fx/fy in [0,1]. */
    public static int lerpLightColor(int c00, int c10, int c01, int c11, float fx, float fy) {
        int r = lerpChannel(c00, c10, c01, c11, 16, fx, fy);
        int g = lerpChannel(c00, c10, c01, c11, 8, fx, fy);
        int b = lerpChannel(c00, c10, c01, c11, 0, fx, fy);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static int lerpChannel(int c00, int c10, int c01, int c11, int shift, float fx, float fy) {
        float top = Mth.lerp(fx, (c00 >> shift) & 0xFF, (c10 >> shift) & 0xFF);
        float bottom = Mth.lerp(fx, (c01 >> shift) & 0xFF, (c11 >> shift) & 0xFF);
        return Mth.clamp((int) Mth.lerp(fy, top, bottom), 0, 255);
    }

    public static void placeFallSplash(Entity entity) {
        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null) return;

        for (WakeNode node : WakeNode.Factory.splashNodes(entity, (int) Math.floor(((ProducesWake) entity).wakes$wakeHeight()))) {
            wakeHandler.insert(node);
        }
    }

    public static void spawnPaddleSplashCloudParticle(Level world, AbstractBoat boat) {
        // TODO MORE OBJECT ORIENTED APPROACH TO PARTICLE SPAWNING
        for (int i = 0; i < 2; i++) {
            if (boat.getPaddleState(i)) {
                double phase = boat.paddlePositions[i] % (2*Math.PI);
                if (AbstractBoat.PADDLE_SPEED / 2 <= phase && phase <= AbstractBoat.PADDLE_SOUND_TIME + AbstractBoat.PADDLE_SPEED) {
                    Vec3 rot = boat.getViewVector(1.0f);
                    double x = boat.getX() + (i == 1 ? -rot.z : rot.z);
                    double z = boat.getZ() + (i == 1 ? rot.x : -rot.x);
                    Vec3 pos = new Vec3(x, ((ProducesWake) boat).wakes$wakeHeight(), z);
                    world.addParticle(ModParticles.SPLASH_CLOUD.withOwner(boat), pos.x, pos.y, pos.z, 0, 0, 0);
                }
            }
        }
    }

    public static void spawnSplashPlane(Level world, Entity owner) {
        WithOwnerParticleType wake = ModParticles.SPLASH_PLANE.withOwner(owner);
        Vec3 pos = owner.position();
        world.addParticle(wake, pos.x, pos.y, pos.z, 0, 0, 0);
    }

    public static void placeWakeTrail(Entity entity) {
        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null) return;

        ProducesWake producer = (ProducesWake) entity;
        double velocity = producer.wakes$getHorizontalVelocity();
        int y = (int) Math.floor(producer.wakes$wakeHeight());

        if (entity instanceof AbstractBoat boat) {
            for (WakeNode node : WakeNode.Factory.rowingNodes(boat, y)) {
                wakeHandler.insert(node);
            }
            if (WakesConfig.spawnParticles) {
                WakesUtils.spawnPaddleSplashCloudParticle(entity.level(), boat);
            }
        }
      
        // TODO FIX ENTERING BOAT CREATES LONG WAKE
        // if (velocity < WakesConfig.minimumProducerVelocity) {
        //     ((ProducesWake) entity).setPrevPos(null);
        // }
        Vec3 prevPos = producer.wakes$getPrevPos();
        if (prevPos == null) {
            return;
        }
        for (WakeNode node : WakeNode.Factory.thickNodeTrail(prevPos.x, prevPos.z, entity.getX(), entity.getZ(), y, WakesConfig.initialStrength, velocity, entity.getBbWidth())) {
            wakeHandler.insert(node);
        }
    }

    public static EffectSpawningRule getEffectRuleFromSource(Entity source) {
        if (source instanceof AbstractBoat boat) {
            List<Entity> passengers = boat.getPassengers();
            if (passengers.contains(Minecraft.getInstance().player)) {
                return WakesConfig.boatSpawning;
            }
            if (passengers.stream().anyMatch(Entity::isAlwaysTicking)) {
                return WakesConfig.boatSpawning.mask(WakesConfig.otherPlayersSpawning);
            }
            return WakesConfig.boatSpawning;
        }
        if (source instanceof Player player) {
            if (player.isSpectator()) {
                return EffectSpawningRule.DISABLED;
            }
            if (player instanceof LocalPlayer) {
                return WakesConfig.playerSpawning;
            }
            if (player instanceof RemotePlayer) {
                return WakesConfig.otherPlayersSpawning;
            }
            return EffectSpawningRule.DISABLED;
        }
        if (source instanceof LivingEntity) {
            return WakesConfig.mobSpawning;
        }
        if (source instanceof ItemEntity) {
            return WakesConfig.itemSpawning;
        }
        return EffectSpawningRule.DISABLED;
    }

    public static void bresenhamLine(int x1, int y1, int x2, int y2, ArrayList<Long> points) {
        // https://www.youtube.com/watch?v=IDFB5CDpLDE credit
        // and of course Bresenham himself :P
        int dy = y2 - y1;
        int dx = x2 - x1;
        if (dx == 0) {
            if (y2 < y1) {
                int temp = y1;
                y1 = y2;
                y2 = temp;
            }
            for (int y = y1; y < y2 + 1; y++) {
                points.add(posAsLong(x1, y));
            }
        } else {
            float k = (float) dy / dx;
            int adjust = k >= 0 ? 1 : -1;
            int offset = 0;
            if (k <= 1 && k >= -1) {
                int delta = Math.abs(dy) * 2;
                int threshold = Math.abs(dx);
                int thresholdInc = Math.abs(dx) * 2;
                int y = y1;
                if (x2 < x1) {
                    int temp = x1;
                    x1 = x2;
                    x2 = temp;
                    y = y2;
                }
                for (int x = x1; x < x2 + 1; x++) {
                    points.add(posAsLong(x, y));
                    offset += delta;
                    if (offset >= threshold) {
                        y += adjust;
                        threshold += thresholdInc;
                    }
                }
            } else {
                int delta = Math.abs(dx) * 2;
                int threshold = Math.abs(dy);
                int thresholdInc = Math.abs(dy) * 2;
                int x = x1;
                if (y2 < y1) {
                    int temp = y1;
                    y1 = y2;
                    y2 = temp;
                }
                for (int y = y1; y < y2 + 1; y++) {
                    points.add(posAsLong(x, y));
                    offset += delta;
                    if (offset >= threshold) {
                        x += adjust;
                        threshold += thresholdInc;
                    }
                }
            }
        }
    }

    public static long posAsLong(int x, int y) {
        int xs = x >> 31 & 1;
        int ys = y >> 31 & 1;
        x &= ~(1 << 31);
        y &= ~(1 << 31);
        long pos = (long) x << 32 | y;
        pos ^= (-xs ^ pos) & (1L << 63);
        pos ^= (-ys ^ pos) & (1L << 31);
        return pos;
    }

    public static int[] longAsPos(long pos) {
        return new int[] {(int) (pos >> 32), (int) pos};
    }

    public static MutableComponent translatable(String ... subKeys) {
        StringBuilder translationKey = new StringBuilder(WakesClient.MOD_ID);
        for (String s : subKeys) {
           translationKey.append(".").append(s);
        }
        return Component.translatable(translationKey.toString());
    }

    public static int[] abgrInt2rgbaArr(int n) {
        int[] arr = new int[4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 8; j++) {
                arr[i] |= (n >> i*8+j & 1) << 7-j;
            }
        }
        return arr;
    }

    public static int rgbaArr2abgrInt(int[] arr) {
        int n = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 8; j++) {
                n |= (arr[i] >> j & 1) << i*8+j;
            }
        }
        return n;
    }

    // public static float getFluidColor() {
    //     return
    // }

    public static float getFluidLevel(Level world, Entity entityInFluid) {
        AABB box = entityInFluid.getBoundingBox();
        return getFluidLevel(world,
                Mth.floor(box.minX), Mth.ceil(box.maxX),
                Mth.floor(box.minY), Mth.ceil(box.maxY),
                Mth.floor(box.minZ), Mth.ceil(box.maxZ));
    }

//    public static float getWaterLevel(ModelPart.Cuboid cuboidInWater) {
//        return getWaterLevel(
//                (int) cuboidInWater.minX, (int) cuboidInWater.maxX,
//                (int) cuboidInWater.minY, (int) cuboidInWater.maxY,
//                (int) cuboidInWater.minZ, (int) cuboidInWater.maxZ);
//    }

    private static float getFluidLevel(Level world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        // Taken from BoatEntity$getWaterHeightBelow
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        yLoop:
        for (int y = minY; y < maxY; ++y) {
            float f = 0.0f;
            for (int x = minX; x < maxX; ++x) {
                for (int z = minZ; z < maxZ; ++z) {
                    blockPos.set(x, y, z);
                    FluidState fluidState = world.getFluidState(blockPos);
                    if (fluidState.isSource()) {
                        f = Math.max(f, fluidState.getHeight(world, blockPos));
                    }
                    if (f >= 1.0f) continue yLoop;
                }
            }
            if (!(f < 1.0f)) continue;
            return blockPos.getY() + f;
        }
        return maxY + 1;

    }
}
