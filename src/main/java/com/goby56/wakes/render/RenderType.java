package com.goby56.wakes.render;


import com.goby56.wakes.WakesClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.render.GameRenderer;

import java.util.function.Supplier;

public enum RenderType {
    AUTO(null),
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
            if (MinecraftClient.isFabulousGraphicsOrBetter()) {
                return ENTITY_CUTOUT.program;
            } else {
                return ENTITY_TRANSLUCENT_CULL.program;
            }
        }
        return WakesClient.CONFIG_INSTANCE.renderType.program;
    }
}
