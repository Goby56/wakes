package com.goby56.wakes.render.enums;


import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;

public enum RenderType {
    AUTO(null),
    GENERAL(toProgram(ShaderProgramKeys.POSITION_COLOR_TEX_LIGHTMAP)),
    CUSTOM(WakesClient.TRANSLUCENT_NO_LIGHT_DIRECTION_PROGRAM.getProgram()),
    SOLID(toProgram(ShaderProgramKeys.RENDERTYPE_SOLID)),
    TRANSLUCENT(toProgram(ShaderProgramKeys.RENDERTYPE_TRANSLUCENT)),
    CUTOUT(toProgram(ShaderProgramKeys.RENDERTYPE_CUTOUT)),
    ENTITY_SOLID(toProgram(ShaderProgramKeys.RENDERTYPE_ENTITY_SOLID)),
    ENTITY_TRANSLUCENT(toProgram(ShaderProgramKeys.RENDERTYPE_ENTITY_TRANSLUCENT)),
    ENTITY_TRANSLUCENT_CULL(toProgram(ShaderProgramKeys.RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL)),
    ENTITY_CUTOUT(toProgram(ShaderProgramKeys.RENDERTYPE_ENTITY_CUTOUT)),
    ENTITY_CUTOUT_NO_CULL(toProgram(ShaderProgramKeys.RENDERTYPE_ENTITY_CUTOUT_NO_CULL)),
    ENTITY_CUTOUT_NO_CULL_Z_OFFSET(toProgram(ShaderProgramKeys.RENDERTYPE_ENTITY_CUTOUT_NO_CULL_Z_OFFSET))
    ;

    public final ShaderProgram program;

    RenderType(ShaderProgram program) {
        this.program = program;
    }

    public static ShaderProgram toProgram(ShaderProgramKey programKey) {
        return MinecraftClient.getInstance().getShaderLoader().getOrCreateProgram(programKey);
    }

    public static ShaderProgram getProgram() {
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
