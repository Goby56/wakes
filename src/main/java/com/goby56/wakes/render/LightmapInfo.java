package com.goby56.wakes.render;

import org.joml.Vector3f;

public record LightmapInfo(
        float ambientLightFactor,
        float skyFactor,
        float blockFactor,
        float nightVisionFactor,
        float darknessScale,
        float darkenWorldFactor,
        float brightnessFactor,
        Vector3f skyLightColor,
        Vector3f ambientColor,
        int currentTick
) {
}
