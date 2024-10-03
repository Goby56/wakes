package com.goby56.wakes.render.enums;


import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Shader;

import java.util.function.Supplier;

public enum RenderType {
    AUTO(null),
    GENERAL(GameRenderer::getPositionColorTexLightmapShader),
    CUSTOM(WakesClient.TRANSLUCENT_NO_LIGHT_DIRECTION_PROGRAM::getProgram),
    SOLID(GameRenderer::getRenderTypeSolidShader),
    TRANSLUCENT(GameRenderer::getRenderTypeTranslucentShader),
    CUTOUT(GameRenderer::getRenderTypeCutoutShader),
    ENTITY_SOLID(GameRenderer::getRenderTypeEntitySolidShader),
    ENTITY_TRANSLUCENT(GameRenderer::getRenderTypeEntityTranslucentShader),
    ENTITY_TRANSLUCENT_CULL(GameRenderer::getRenderTypeEntityTranslucentCullShader),
    ENTITY_CUTOUT(GameRenderer::getRenderTypeEntityCutoutShader),
    ENTITY_CUTOUT_NO_CULL(GameRenderer::getRenderTypeEntityCutoutNoNullShader),
    ENTITY_CUTOUT_NO_CULL_Z_OFFSET(GameRenderer::getRenderTypeEntityCutoutNoNullZOffsetShader)
    ;

    public final Supplier<Shader> program;

    RenderType(Supplier<Shader> program) {
        this.program = program;
    }

    public static Supplier<Shader> getProgram() {
        if (WakesConfig.renderType == RenderType.AUTO) {
            if (WakesClient.areShadersEnabled) {
                return ENTITY_TRANSLUCENT_CULL.program;
            } else {
                return CUSTOM.program;
            }
        }
        return WakesConfig.renderType.program;
    }
}
