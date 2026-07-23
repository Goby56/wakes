package com.goby56.wakes.render;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/**
 * Loads per-entity occlusion-dimension overrides from datapacks, one JSON file per entity id
 * under "data/<namespace>/wakes_occlusion_dimensions/<path>.json".
 */
public class OcclusionDimensionsManager extends SimpleJsonResourceReloadListener<OcclusionDimensions> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("wakes", "occlusion_dimensions");

    private static Map<Identifier, OcclusionDimensions> overrides = Map.of();

    public OcclusionDimensionsManager() {
        super(OcclusionDimensions.CODEC, FileToIdConverter.json("wakes_occlusion_dimensions"));
    }

    @Override
    protected void apply(Map<Identifier, OcclusionDimensions> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        overrides = Map.copyOf(map);
    }

    /** entityTypeId is the entity's registry id, e.g. "minecraft:oak_chest_boat". */
    public static OcclusionDimensions get(Identifier entityTypeId) {
        return overrides.getOrDefault(entityTypeId, OcclusionDimensions.DEFAULT_BOAT);
    }
}
