package com.goby56.wakes.config;

import com.goby56.wakes.config.enums.EffectSpawningRule;
import com.goby56.wakes.config.enums.Resolution;
import com.goby56.wakes.render.enums.RenderType;
import eu.midnightdust.lib.config.MidnightConfig;

public class WakesMidnightConfig extends MidnightConfig {
    public static final String GENERAL = "general";
    public static final String APPEARANCE = "appearance";
    public static final String DEBUG = "debug";
    // Debug
    @Entry(category = GENERAL) public static boolean disableMod = false; // TODO SWITCH TO ENABLE MOD TOGGLE
    @Entry(category = GENERAL) public static boolean pickBoat = true;


    // Spawning
    @Entry(category = GENERAL) public static EffectSpawningRule boatSpawning = EffectSpawningRule.SIMULATION_AND_PLANES;
    @Entry(category = GENERAL) public static EffectSpawningRule playerSpawning = EffectSpawningRule.ONLY_SIMULATION;
    @Entry(category = GENERAL) public static EffectSpawningRule otherPlayersSpawning = EffectSpawningRule.ONLY_SIMULATION;
    @Entry(category = GENERAL) public static EffectSpawningRule mobSpawning = EffectSpawningRule.ONLY_SIMULATION;
    @Entry(category = GENERAL) public static EffectSpawningRule itemSpawning = EffectSpawningRule.ONLY_SIMULATION;

    // // Behaviour
    // @Entry(category = GENERAL) public float wavePropagationFactor = 0.95f;
    // @Entry(category = GENERAL) public float waveDecayFactor = 0.5f;
    // @Entry(category = GENERAL) public int initialStrength = 20;
    // @Entry(category = GENERAL) public int paddleStrength = 100;
    // @Entry(category = GENERAL) public int splashStrength = 100;
    // @Entry(category = GENERAL) public boolean spawnParticles = true;

    // @Entry(category = APPEARANCE) public Resolution wakeResolution = Resolution.SIXTEEN;
    // @Entry(category = APPEARANCE) public float wakeOpacity = 1f;
    // @Entry(category = APPEARANCE) public boolean firstPersonSplashPlane = false;

    // // Splash plane
    // @Entry(category = APPEARANCE) public float splashPlaneWidth = 2f;
    // @Entry(category = APPEARANCE) public float splashPlaneHeight = 1.5f;
    // @Entry(category = APPEARANCE) public float splashPlaneDepth = 3f;
    // @Entry(category = APPEARANCE) public float splashPlaneOffset = -0.2f;
    // @Entry(category = APPEARANCE) public float splashPlaneGap = 1f;
    // @Entry(category = APPEARANCE) public int splashPlaneResolution = 5;
    // @Entry(category = APPEARANCE) public float maxSplashPlaneVelocity = 0.5f;
    // @Entry(category = APPEARANCE) public float splashPlaneScale = 0.8f;

    // @Entry(category = DEBUG) public static boolean debugColors = false;
    // @Entry(category = DEBUG) public static int floodFillDistance = 2;
    // @Entry(category = DEBUG) public static int ticksBeforeFill = 2;
    // @Entry(category = DEBUG) public static RenderType renderType = RenderType.AUTO;
    // @Entry(category = DEBUG) public static boolean drawDebugBoxes = false;
    // @Entry(category = DEBUG) public static boolean showDebugInfo = false;
    // @Entry(category = DEBUG) public static float shaderLightPassthrough = 0.5f;
}
