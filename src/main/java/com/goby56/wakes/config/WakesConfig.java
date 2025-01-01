package com.goby56.wakes.config;

import com.goby56.wakes.config.enums.EffectSpawningRule;
import com.goby56.wakes.config.enums.Resolution;
import com.goby56.wakes.render.enums.RenderType;
import com.goby56.wakes.render.enums.WakeColor;
import com.google.common.collect.Lists;
import eu.midnightdust.lib.config.MidnightConfig;

import java.util.List;

public class WakesConfig extends MidnightConfig {
    public static final String GENERAL = "general";
    public static final String APPEARANCE = "appearance";
    public static final String DEBUG = "debug";

    // Debug
    @Entry(category = GENERAL) public static boolean disableMod = false; // TODO SWITCH TO ENABLE MOD TOGGLE
    @Entry(category = GENERAL) public static boolean pickBoat = true;


    // Spawning
    @Comment(category = GENERAL, centered = true) public static Comment spawningRuleDivider;
    @Entry(category = GENERAL) public static EffectSpawningRule boatSpawning = EffectSpawningRule.SIMULATION_AND_PLANES;
    @Entry(category = GENERAL) public static EffectSpawningRule playerSpawning = EffectSpawningRule.ONLY_SIMULATION;
    @Entry(category = GENERAL) public static EffectSpawningRule otherPlayersSpawning = EffectSpawningRule.ONLY_SIMULATION;
    @Entry(category = GENERAL) public static EffectSpawningRule mobSpawning = EffectSpawningRule.ONLY_SIMULATION;
    @Entry(category = GENERAL) public static EffectSpawningRule itemSpawning = EffectSpawningRule.ONLY_SIMULATION;

    @Comment(category = GENERAL, centered = true) public static Comment wakeBehaviourDivider;
    @Entry(category = GENERAL) public static float wavePropagationFactor = 0.95f;
    @Entry(category = GENERAL) public static float waveDecayFactor = 0.5f;
    @Entry(category = GENERAL) public static int initialStrength = 20;
    @Entry(category = GENERAL) public static int paddleStrength = 100;
    @Entry(category = GENERAL) public static int splashStrength = 100;


    @Entry(category = APPEARANCE) public static Resolution wakeResolution = Resolution.SIXTEEN;
    @Entry(category = APPEARANCE, isSlider = true, min = 0, max = 1) public static float wakeOpacity = 1f;
    @Entry(category = APPEARANCE) public static boolean firstPersonSplashPlane = false;
    @Entry(category = APPEARANCE) public static boolean spawnParticles = true;
    @Entry(category = APPEARANCE, isSlider = true, min = 0, max = 1) public static float shaderLightPassthrough = 0.5f;

    // Splash plane
    @Comment(category = APPEARANCE, centered = true) public static Comment splashPlaneDivider;
    @Entry(category = APPEARANCE) public static float splashPlaneWidth = 2f;
    @Entry(category = APPEARANCE) public static float splashPlaneHeight = 1.5f;
    @Entry(category = APPEARANCE) public static float splashPlaneDepth = 3f;
    @Entry(category = APPEARANCE) public static float splashPlaneOffset = -0.2f;
    @Entry(category = APPEARANCE) public static float splashPlaneGap = 1f;
    @Entry(category = APPEARANCE) public static int splashPlaneResolution = 5;
    @Entry(category = APPEARANCE) public static float maxSplashPlaneVelocity = 0.5f;
    @Entry(category = APPEARANCE) public static float splashPlaneScale = 0.8f;

    @Hidden @Entry(category = APPEARANCE) public static List<Float> wakeColorIntervals = Lists.newArrayList(0.05f, 0.15f, 0.2f, 0.35f, 0.52f, 0.6f, 0.7f, 0.9f);
    @Hidden @Entry(selectionMode = 1, category = APPEARANCE, isColor = true) public static List<String> wakeColors = Lists.newArrayList(
            "#00000000", // TRANSPARENT
            "#289399a6", // DARK GRAY
            "#649ea5b0", // GRAY
            "#b4c4cad1", // LIGHT GRAY
            "#00000000", // TRANSPARENT
            "#b4c4cad1", // LIGHT GRAY
            "#ffffffff", // WHITE
            "#b4c4cad1", // LIGHT GRAY
            "#649ea5b0" // GRAY
    );
    public static List<Float> defaultWakeColorIntervals = Lists.newArrayList(wakeColorIntervals);
    public static List<String> defaultWakeColors = Lists.newArrayList(wakeColors);

    @Entry(category = DEBUG) public static boolean debugColors = false;
    @Entry(category = DEBUG) public static boolean drawDebugBoxes = false;
    @Entry(category = DEBUG) public static boolean showDebugInfo = false;
    @Entry(category = DEBUG, isSlider = true, min = 1, max = 6) public static int floodFillDistance = 2;
    @Entry(category = DEBUG, isSlider = true, min = 1, max = 20) public static int floodFillTickDelay = 2;
    @Entry(category = DEBUG) public static RenderType renderType = RenderType.AUTO;

    public static WakeColor getWakeColor(int i) {
        return new WakeColor(wakeColors.get(i));
    }
}
