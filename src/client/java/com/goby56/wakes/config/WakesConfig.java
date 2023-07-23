package com.goby56.wakes.config;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import com.goby56.wakes.WakesClient;
import com.goby56.wakes.render.BlendingFunction;
import com.goby56.wakes.utils.WakeColor;
import com.google.gson.Gson;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WakesConfig {
    // Spawning
    public Map<String, WakeSpawningRule> wakeSpawningRules = new HashMap<>(Map.of(
            "boat_wake_rules", WakeSpawningRule.WAKES_AND_SPLASHES,
            "player_wake_rules", WakeSpawningRule.WAKES_AND_SPLASHES,
            "other_players_wake_rules", WakeSpawningRule.WAKES_AND_SPLASHES,
            "mobs_wake_rules", WakeSpawningRule.WAKES_AND_SPLASHES,
            "items_wake_rules", WakeSpawningRule.WAKES_AND_SPLASHES
    ));
    public boolean wakesInRunningWater = false;

    // Behaviour
    public float waveSpeed = 0.95f;
    public int initialStrength = 20;
    public int paddleStrength = 100;
    public int splashStrength = 100;
    public double minimumProducerVelocity = 0.1;
    public float waveDecay = 0.9f;
    public boolean useAgeDecay = false;

    // Debug
    public int floodFillDistance = 3;
    public boolean use9PointStencil = true;
    public int ticksBeforeFill = 2;
    public boolean drawDebugBoxes = false;
    public boolean renderWakes = true;
    public boolean spawnWakes = true;

    // Colors
    public boolean useWaterBlending = true;
    public BlendingFunction blendMode = BlendingFunction.SCREEN;
    public List<ColorInterval> colorIntervals = List.of(
            new ColorInterval(WakeColor.TRANSPARENT, -50, -45),
            new ColorInterval(WakeColor.DARK_GRAY, -45, -35),
            new ColorInterval(WakeColor.GRAY, -35, -30),
            new ColorInterval(WakeColor.LIGHT_GRAY, -30, -15),
            new ColorInterval(WakeColor.TRANSPARENT, -15, 2),
            new ColorInterval(WakeColor.LIGHT_GRAY, 2, 10),
            new ColorInterval(WakeColor.WHITE, 10, 20),
            new ColorInterval(WakeColor.LIGHT_GRAY, 20, 40),
            new ColorInterval(WakeColor.GRAY, 40, 50)
    );

    public static class ColorInterval {
        public WakeColor color;
        public int lower;
        public int upper;

        public ColorInterval(WakeColor color, int lower, int upper) {
            this.color = color;
            this.lower = lower;
            this.upper = upper;
        }

        public void setColor(WakeColor color) {
            this.color = color;
        }

        public void setLower(int lower) {
            this.lower = lower;
        }

        public void setUpper(int upper) {
            this.upper = upper;
        }

    }

    public enum WakeSpawningRule {
        WAKES_AND_SPLASHES(true, true),
        ONLY_WAKES(true, false),
        ONLY_SPLASHES(false, true),
        DISABLED(false, false);

        public final boolean spawnsWake;
        public final boolean spawnsSplashes;

        WakeSpawningRule(boolean spawnsWake, boolean spawnsSplashes) {
            this.spawnsWake = spawnsWake;
            this.spawnsSplashes = spawnsSplashes;
        }
    }

    public WakeSpawningRule getSpawningRule(Entity producer) {
        if (producer instanceof BoatEntity boat) {
            if (wakeSpawningRules.get("boat_wake_rules") == WakeSpawningRule.WAKES_AND_SPLASHES) {
                if (!boat.hasPassenger(MinecraftClient.getInstance().player)) {
                    return wakeSpawningRules.get("other_players_wake_rules");
                }
            }
            return wakeSpawningRules.get("boat_wake_rules");
        }
        if (producer instanceof PlayerEntity player) {
            if (player.isSpectator()) {
                return WakeSpawningRule.DISABLED;
            }
            if (player instanceof ClientPlayerEntity) {
                return wakeSpawningRules.get("player_wake_rules");
            }
            return wakeSpawningRules.get("other_players_wake_rules");
        }
        if (producer instanceof LivingEntity) {
            return wakeSpawningRules.get("mobs_wake_rules");
        }
        if (producer instanceof ItemEntity) {
            return wakeSpawningRules.get("items_wake_rules");
        }
        return WakeSpawningRule.DISABLED;
    }

    public static WakesConfig loadConfig() {
        Jankson jankson = Jankson.builder().build();
        try {
            File configFile = new File(WakesClient.CONFIG_PATH);
            if (!configFile.exists()) {
                WakesClient.LOGGER.info(String.format("No config file found for wakes-%s. Edit one or more configs to to create one.", WakesClient.METADATA.getVersion().getFriendlyString()));
//                WakesConfig config = new WakesConfig();
//                config.saveConfig();
                return new WakesConfig();
            }

            JsonObject configJson = jankson.load(configFile);
            String normalized = configJson.toJson(false, false);

//            return jankson.fromJson(configJson, WakesConfig.class);
            return new Gson().fromJson(normalized, WakesConfig.class);
        } catch (IOException | SyntaxError e) {
            e.printStackTrace();
            return new WakesConfig();
        }
    }

    public void saveConfig() {
        File configFile = new File(WakesClient.CONFIG_PATH);
        Jankson jankson = Jankson.builder().build();
        String result = jankson.toJson(this).toJson(true, true);

        try {
            boolean usable = configFile.exists() || configFile.createNewFile();
            if (!usable) return;

            FileOutputStream out = new FileOutputStream(configFile, false);
            out.write(result.getBytes());
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
