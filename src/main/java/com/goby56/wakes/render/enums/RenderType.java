package com.goby56.wakes.render.enums;


import com.goby56.wakes.WakesClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.GameRenderer;

import java.util.function.Supplier;

public enum RenderType {
    AUTO(null),
    GENERAL(GameRenderer::getPositionColorTexLightmapProgram),
    CUSTOM(WakesClient.TRANSLUCENT_NO_LIGHT_DIRECTION_PROGRAM::getProgram),
    SOLID(GameRenderer::getRenderTypeSolidProgram),
    TRANSLUCENT(GameRenderer::getRenderTypeTranslucentProgram),
    CUTOUT(GameRenderer::getRenderTypeCutoutProgram),
    ENTITY_SOLID(GameRenderer::getRenderTypeEntitySolidProgram),
    ENTITY_TRANSLUCENT(GameRenderer::getRenderTypeEntityTranslucentProgram),
    ENTITY_TRANSLUCENT_CULL(GameRenderer::getRenderTypeEntityTranslucentCullProgram),
    ENTITY_CUTOUT(GameRenderer::getRenderTypeEntityCutoutProgram),
    ENTITY_CUTOUT_NO_CULL(GameRenderer::getRenderTypeEntityCutoutNoNullProgram),
    ENTITY_CUTOUT_NO_CULL_Z_OFFSET(GameRenderer::getRenderTypeEntityCutoutNoNullZOffsetProgram)
    ;

    public final Supplier<ShaderProgram> program;

    RenderType(Supplier<ShaderProgram> program) {
        this.program = program;
    }

    public static Supplier<ShaderProgram> getProgram() {
        if (WakesClient.CONFIG_INSTANCE.renderType == RenderType.AUTO) {
            if (WakesClient.areShadersEnabled()) {
                return ENTITY_TRANSLUCENT_CULL.program;
            } else {
                return CUSTOM.program;
            }
        }
        return WakesClient.CONFIG_INSTANCE.renderType.program;
    }
}
