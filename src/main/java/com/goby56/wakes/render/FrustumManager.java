package com.goby56.wakes.render;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;

public class FrustumManager implements LevelRenderEvents.EndExtraction {
    private static Frustum frustum;

    public static boolean isVisible(AABB aabb) {
        if (frustum == null) {
            return true;
        }
        return frustum.isVisible(aabb);
    }

    @Override
    public void endExtraction(LevelExtractionContext context) {
        frustum = context.camera().getCapturedFrustum();
    }
}
