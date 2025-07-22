package com.goby56.wakes.render;

import org.joml.Vector3f;

public record LightmapInfo(float AmbientLightFactor, float SkyFactor, float BlockFactor, boolean UseBrightLightmap, float NightVisionFactor,
                           float DarknessScale, float DarkenWorldFactor, float BrightnessFactor, Vector3f skyLightColor,
                           int currentTick) {
}
