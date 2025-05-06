package com.goby56.wakes.utils;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.config.enums.EffectSpawningRule;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.particle.ModParticles;
import com.goby56.wakes.particle.WithOwnerParticleType;
import com.goby56.wakes.render.LightmapWrapper;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class WakesUtils {

    public static int getLightColor(World world, BlockPos blockPos) {
        int lightCoordinate = WorldRenderer.getLightmapCoordinates(world, blockPos);
        int x = LightmapTextureManager.getBlockLightCoordinates(lightCoordinate);
        int y = LightmapTextureManager.getSkyLightCoordinates(lightCoordinate);
        return LightmapWrapper.readPixel(x, y);
    }

    public static void placeFallSplash(Entity entity) {
        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null) return;

        for (WakeNode node : WakeNode.Factory.splashNodes(entity, (int) Math.floor(((ProducesWake) entity).wakes$wakeHeight()))) {
            wakeHandler.insert(node);
        }
    }

    public static void spawnPaddleSplashCloudParticle(World world, BoatEntity boat) {
        // TODO MORE OBJECT ORIENTED APPROACH TO PARTICLE SPAWNING
        for (int i = 0; i < 2; i++) {
            if (boat.isPaddleMoving(i)) {
                double phase = boat.paddlePhases[i] % (2*Math.PI);
                if (BoatEntity.NEXT_PADDLE_PHASE / 2 <= phase && phase <= BoatEntity.EMIT_SOUND_EVENT_PADDLE_ROTATION + BoatEntity.NEXT_PADDLE_PHASE) {
                    Vec3d rot = boat.getRotationVec(1.0f);
                    double x = boat.getX() + (i == 1 ? -rot.z : rot.z);
                    double z = boat.getZ() + (i == 1 ? rot.x : -rot.x);
                    Vec3d pos = new Vec3d(x, ((ProducesWake) boat).wakes$wakeHeight(), z);
                    world.addParticleClient(ModParticles.SPLASH_CLOUD, pos.x, pos.y, pos.z, 0, 0, 0);
                }
            }
        }
    }

    public static void spawnSplashPlane(World world, Entity owner) {
        WithOwnerParticleType wake = ModParticles.SPLASH_PLANE.withOwner(owner);
        Vec3d pos = owner.getPos();
        world.addParticleClient(wake, pos.x, pos.y, pos.z, 0, 0, 0);
    }

    public static void placeWakeTrail(Entity entity) {
        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null) return;

        ProducesWake producer = (ProducesWake) entity;
        double velocity = producer.wakes$getHorizontalVelocity();
        int y = (int) Math.floor(producer.wakes$wakeHeight());

        if (entity instanceof BoatEntity boat) {
            for (WakeNode node : WakeNode.Factory.rowingNodes(boat, y)) {
                wakeHandler.insert(node);
            }
            if (WakesConfig.spawnParticles) {
                WakesUtils.spawnPaddleSplashCloudParticle(entity.getWorld(), boat);
            }
        }
      
        // TODO FIX ENTERING BOAT CREATES LONG WAKE
        // if (velocity < WakesConfig.minimumProducerVelocity) {
        //     ((ProducesWake) entity).setPrevPos(null);
        // }
        Vec3d prevPos = producer.wakes$getPrevPos();
        if (prevPos == null) {
            return;
        }
        for (WakeNode node : WakeNode.Factory.thickNodeTrail(prevPos.x, prevPos.z, entity.getX(), entity.getZ(), y, WakesConfig.initialStrength, velocity, entity.getWidth())) {
            wakeHandler.insert(node);
        }
    }

    public static EffectSpawningRule getEffectRuleFromSource(Entity source) {
        if (source instanceof BoatEntity boat) {
            List<Entity> passengers = boat.getPassengerList();
            if (passengers.contains(MinecraftClient.getInstance().player)) {
                return WakesConfig.boatSpawning;
            }
            if (passengers.stream().anyMatch(Entity::isPlayer)) {
                return WakesConfig.boatSpawning.mask(WakesConfig.otherPlayersSpawning);
            }
            return WakesConfig.boatSpawning;
        }
        if (source instanceof PlayerEntity player) {
            if (player.isSpectator()) {
                return EffectSpawningRule.DISABLED;
            }
            if (player instanceof ClientPlayerEntity) {
                return WakesConfig.playerSpawning;
            }
            if (player instanceof OtherClientPlayerEntity) {
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

    public static MutableText translatable(String ... subKeys) {
        StringBuilder translationKey = new StringBuilder(WakesClient.MOD_ID);
        for (String s : subKeys) {
           translationKey.append(".").append(s);
        }
        return Text.translatable(translationKey.toString());
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

    public static float getFluidLevel(World world, Entity entityInFluid) {
        Box box = entityInFluid.getBoundingBox();
        return getFluidLevel(world,
                MathHelper.floor(box.minX), MathHelper.ceil(box.maxX),
                MathHelper.floor(box.minY), MathHelper.ceil(box.maxY),
                MathHelper.floor(box.minZ), MathHelper.ceil(box.maxZ));
    }

//    public static float getWaterLevel(ModelPart.Cuboid cuboidInWater) {
//        return getWaterLevel(
//                (int) cuboidInWater.minX, (int) cuboidInWater.maxX,
//                (int) cuboidInWater.minY, (int) cuboidInWater.maxY,
//                (int) cuboidInWater.minZ, (int) cuboidInWater.maxZ);
//    }

    private static float getFluidLevel(World world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        // Taken from BoatEntity$getWaterHeightBelow
        BlockPos.Mutable blockPos = new BlockPos.Mutable();
        yLoop:
        for (int y = minY; y < maxY; ++y) {
            float f = 0.0f;
            for (int x = minX; x < maxX; ++x) {
                for (int z = minZ; z < maxZ; ++z) {
                    blockPos.set(x, y, z);
                    FluidState fluidState = world.getFluidState(blockPos);
                    if (fluidState.isStill()) {
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
