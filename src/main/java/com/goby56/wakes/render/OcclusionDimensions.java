package com.goby56.wakes.render;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Width/length of an entity's occlusion footprint (see OcclusionZone). Entities in
 * WakesConfig.OCCLUDING_TYPES that don't have an override file (see
 * OcclusionDimensionsManager) fall back to DEFAULT_BOAT.
 */
public record OcclusionDimensions(float width, float length) {
    public static final Codec<OcclusionDimensions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("width").forGetter(OcclusionDimensions::width),
            Codec.FLOAT.fieldOf("length").forGetter(OcclusionDimensions::length)
    ).apply(instance, OcclusionDimensions::new));

    public static final OcclusionDimensions DEFAULT_BOAT = new OcclusionDimensions(1.4f, 2.2f);
}
