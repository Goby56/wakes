package com.goby56.wakes.config;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import com.goby56.wakes.WakesClient;
import com.goby56.wakes.render.BlendingFunction;
import com.goby56.wakes.render.RenderType;
import com.goby56.wakes.utils.WakeColor;
import com.google.gson.Gson;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.MathHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WakesConfig {
    // Spawning
    public Map<String, WakeSpawningRule> wakeSpawningRules = new HashMap<>(Map.of(
            "boat", WakeSpawningRule.WAKES_AND_SPLASHES,
            "player", WakeSpawningRule.WAKES_AND_SPLASHES,
            "other_players", WakeSpawningRule.WAKES_AND_SPLASHES,
            "mobs", WakeSpawningRule.WAKES_AND_SPLASHES,
            "items", WakeSpawningRule.WAKES_AND_SPLASHES
    ));
    public boolean wakesInRunningWater = false;

    // Behaviour
    public float wavePropagationFactor = 0.95f;
    public float waveDecayFactor = 0.5f;
    public int initialStrength = 20;
    public int paddleStrength = 100;
    public int splashStrength = 100;
    public boolean spawnParticles = true;

    // Debug
    public int floodFillDistance = 3;
    public boolean use9PointStencil = true;
    public int ticksBeforeFill = 2;
    public boolean drawDebugBoxes = false;
    public boolean renderWakes = true;
    public boolean spawnWakes = true;
    public RenderType renderType = RenderType.AUTO;

    // Appearance
    public Resolution wakeResolution = Resolution.SIXTEEN;
    public float wakeOpacity = 1f;
    public boolean useWaterBlending = true;
    public BlendingFunction blendFunc = BlendingFunction.DEFAULT;
    public GlStateManager.SrcFactor srcFactor = GlStateManager.SrcFactor.SRC_ALPHA;
    public GlStateManager.DstFactor dstFactor = GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA;
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

    // Splash plane
    public boolean renderSplashPlane = true;
    public float splashPlaneWidth = 3f;
    public float splashPlaneHeight = 1.5f;
    public float splashPlaneDepth = 2f;
    public int splashPlaneResolution = 5;
    public float maxSplashPlaneVelocity = 0.5f;
    public float splashPlaneScale = 1f;
    public float splashPlaneOffset = 0f;

    public enum WakeSpawningRule {
        // TODO MORE EXHAUSTIVE CONFIG CONDITION CHECKS
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

    public enum Resolution {
        EIGHT(8),
        SIXTEEN(16),
        THIRTYTWO(32);

        public final int res;
        public final int power;

        Resolution(int res) {
            this.res = res;
            this.power = MathHelper.floorLog2(res);
        }

        @Override
        public String toString() {
            return String.valueOf(this.res);
        }
    }

    public WakeSpawningRule getSpawningRule(Entity producer) {
        if (producer instanceof BoatEntity boat) {
            if (wakeSpawningRules.get("boat") == WakeSpawningRule.WAKES_AND_SPLASHES) {
                if (!boat.hasPassenger(MinecraftClient.getInstance().player)) {
                    return wakeSpawningRules.get("other_players");
                }
            }
            return wakeSpawningRules.get("boat");
        }
        if (producer instanceof PlayerEntity player) {
            if (player.isSpectator()) {
                return WakeSpawningRule.DISABLED;
            }
            if (player instanceof ClientPlayerEntity) {
                return wakeSpawningRules.get("player");
            }
            return wakeSpawningRules.get("other_players");
        }
        if (producer instanceof LivingEntity) {
            return wakeSpawningRules.get("mobs");
        }
        if (producer instanceof ItemEntity) {
            return wakeSpawningRules.get("items");
        }
        return WakeSpawningRule.DISABLED;
    }

    public static WakesConfig loadConfig() {
        Jankson jankson = Jankson.builder().build();
        try {
            File configFile = new File(WakesClient.CONFIG_PATH);
            if (!configFile.exists()) {
                WakesClient.LOGGER.info(String.format("No config file found for wakes-%s. Creating one...", WakesClient.METADATA.getVersion().getFriendlyString()));
                WakesConfig config = new WakesConfig();
                config.saveConfig();
                return config;
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
