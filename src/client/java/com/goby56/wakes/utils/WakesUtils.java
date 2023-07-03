package com.goby56.wakes.utils;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.duck.ProducesWake;
import net.minecraft.entity.Entity;
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

import java.util.ArrayList;

public class WakesUtils {

    public static void placeSingleSplash(World world, Entity producer) {
        WakeHandler instance = WakeHandler.getInstance();
        if (instance == null) {
            return;
        }
        float height = getWaterLevel(world, producer);
        // TODO DEPENDING ON PRODUCER VELOCITY MAKE BIGGER SPLASH
        instance.insert(new WakeNode(new Vec3d(producer.getX(), height, producer.getZ()), WakesConfig.initialStrength));
    }

//    public static void spawnWake(World world, Entity owner) {
//        WakeParticleType wake = ModParticles.WAKE.withOwner(owner);
//        Vec3d pos = owner.getPos();
//        world.addParticle(wake, pos.x, pos.y, pos.z, 0, 0, 0);
//    }

    public static void placeWakeTrail(World world, Entity producer) {
        WakeHandler instance = WakeHandler.getInstance();
        if (instance == null) {
            return;
        }
        float height = getWaterLevel(world, producer);

        if (producer instanceof BoatEntity boat) {
            for (WakeNode node : WakeNode.Factory.rowingNodes(boat, height)) {
                instance.insert(node);
            }
        }

        Vec3d prevPos = ((ProducesWake) producer).getPrevPos();
        if (prevPos == null) {
            instance.insert(new WakeNode(new Vec3d(producer.getX(), height, producer.getZ()), WakesConfig.initialStrength));
            return;
        }

        for (WakeNode node : WakeNode.Factory.thickNodeTrail(prevPos.x, prevPos.z, producer.getX(), producer.getZ(), height, WakesConfig.initialStrength, producer.getVelocity().horizontalLength(), producer.getWidth())) {
            instance.insert(node);
        }
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
            int k = dy / dx;
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

    public static MutableText translatable(String category, String field) {
        return Text.translatable(String.format("%s.%s.%s", category, WakesClient.MOD_ID, field));
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

    public static float getWaterLevel(World world, Entity entityInWater) {
        // Taken from BoatEntity$getWaterLevelBelow
        Box box = entityInWater.getBoundingBox();
        int minX = MathHelper.floor(box.minX);
        int maxX = MathHelper.ceil(box.maxX);
        int minY = MathHelper.floor(box.minY);
        int maxY = MathHelper.ceil(box.maxY);
        int minZ = MathHelper.floor(box.minZ);
        int maxZ = MathHelper.ceil(box.maxZ);
        BlockPos.Mutable blockPos = new BlockPos.Mutable();

        yLoop:
        for (int y = minY; y < maxY; ++y) {
            float f = 0.0f;
            for (int x = minX; x < maxX; ++x) {
                for (int z = minZ; z < maxZ; ++z) {
                    blockPos.set(x, y, z);
                    FluidState fluidState = world.getFluidState(blockPos);
                    if (fluidState.isIn(FluidTags.WATER)) {
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
