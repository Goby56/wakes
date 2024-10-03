package com.goby56.wakes.utils;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.config.enums.EffectSpawningRule;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WakesUtils {

    public static void placeFallSplash(Entity entity) {
        WakeHandler instance = WakeHandler.getInstance();
        if (instance == null) {
            return;
        }

        for (WakeNode node : WakeNode.Factory.splashNodes(entity, (int) Math.floor(((ProducesWake) entity).producingWaterLevel()))) {
            instance.insert(node);
        }
    }
    public static void placeWakeTrail(Entity entity) {
        WakeHandler wakeHandler = WakeHandler.getInstance();
        if (wakeHandler == null) {
            return;
        }
        ProducesWake producer = (ProducesWake) entity;
        double velocity = producer.getHorizontalVelocity();
        int y = (int) Math.floor(producer.producingWaterLevel());

        // TODO FIX ENTERING BOAT CREATES LONG WAKE
        // if (velocity < WakesClient.CONFIG_INSTANCE.minimumProducerVelocity) {
        //     ((ProducesWake) entity).setPrevPos(null);
        // }
        Vec3d prevPos = producer.getPrevPos();
        if (prevPos == null) {
            return;
        }
        for (WakeNode node : WakeNode.Factory.thickNodeTrail(prevPos.x, prevPos.z, entity.getX(), entity.getZ(), y, WakesConfig.initialStrength, velocity, entity.getWidth())) {
            wakeHandler.insert(node);
        }
    }

    public static EffectSpawningRule getEffectRuleFromSource(Entity source) {
        Map<String, EffectSpawningRule> effectRule = WakesConfig.effectSpawningRules;
        if (source instanceof BoatEntity boat) {
            List<Entity> passengers = boat.getPassengerList();
            if (passengers.contains(MinecraftClient.getInstance().player)) {
                return effectRule.get("boat");
            }
            if (passengers.stream().anyMatch(Entity::isPlayer)) {
                return effectRule.get("boat").mask(effectRule.get("other_players"));
            }
            return effectRule.get("boat");
        }
        if (source instanceof PlayerEntity player) {
            if (player.isSpectator()) {
                return EffectSpawningRule.DISABLED;
            }
            if (player instanceof ClientPlayerEntity) {
                return effectRule.get("player");
            }
            if (player instanceof OtherClientPlayerEntity) {
                return effectRule.get("other_players");
            }
            return EffectSpawningRule.DISABLED;
        }
        if (source instanceof LivingEntity) {
            return effectRule.get("mobs");
        }
        if (source instanceof ItemEntity) {
            return effectRule.get("items");
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

    public static MutableText translatable(String category, String field) {
        return Text.translatable(String.format("%s.%s.%s", WakesClient.MOD_ID, category, field));
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
}
