package com.goby56.wakes.config;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.enums.EffectSpawningRule;
import com.goby56.wakes.config.enums.Resolution;
import com.goby56.wakes.render.enums.RenderType;
import com.goby56.wakes.render.enums.WakeColor;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class WakesConfig {

    public int highlightIndex = 2;
    public ArrayList<Float> wakeGradientRanges = new ArrayList<>(List.of(0.1f, 0.3f, 0.6f, 0.8f));
    public ArrayList<WakeColor> wakeColors = new ArrayList<>(List.of(
            new WakeColor(255, 0, 0, 255),
            new WakeColor(255, 255, 0, 255),
            new WakeColor(255, 255, 255, 255),
            new WakeColor(0, 255, 255, 255),
            new WakeColor(0, 255, 255, 255)
    ));

    // Spawning
    public Map<String, EffectSpawningRule> effectSpawningRules = new HashMap<>(Map.of(
            "boat", EffectSpawningRule.SIMULATION_AND_PLANES,
            "player", EffectSpawningRule.ONLY_SIMULATION,
            "other_players", EffectSpawningRule.ONLY_SIMULATION,
            "mobs", EffectSpawningRule.ONLY_SIMULATION,
            "items", EffectSpawningRule.ONLY_SIMULATION
    ));

    // Behaviour
    public float wavePropagationFactor = 0.95f;
    public float waveDecayFactor = 0.5f;
    public int initialStrength = 20;
    public int paddleStrength = 100;
    public int splashStrength = 100;
    public boolean spawnParticles = true;

    // Debug
    public boolean disableMod = false;
    public int floodFillDistance = 2;
    public int ticksBeforeFill = 2;
    public boolean pickBoat = true;
    public RenderType renderType = RenderType.AUTO;
    public boolean drawDebugBoxes = false;
    public boolean showDebugInfo = false;
    public float shaderLightPassthrough = 0.5f;

    // Appearance
    public Resolution wakeResolution = Resolution.SIXTEEN;
    public float wakeOpacity = 1f;
    public boolean firstPersonSplashPlane = false;

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
    public float splashPlaneWidth = 2f;
    public float splashPlaneHeight = 1.5f;
    public float splashPlaneDepth = 3f;
    public float splashPlaneOffset = -0.2f;
    public float splashPlaneGap = 1f;
    public int splashPlaneResolution = 5;
    public float maxSplashPlaneVelocity = 0.5f;
    public float splashPlaneScale = 0.8f;

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
