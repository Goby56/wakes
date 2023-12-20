package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GraphicsMode;

public enum BlendingFunction {
    DEFAULT(true),
    SCREEN(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, false);

    public final GlStateManager.SrcFactor srcFactor;
    public final GlStateManager.DstFactor dstFactor;
    public final boolean canVaryOpacity;

    BlendingFunction(boolean variableOpacity) {
        this.srcFactor = null;
        this.dstFactor = null;
        this.canVaryOpacity = variableOpacity;
    }

    BlendingFunction(GlStateManager.SrcFactor srcFactor, GlStateManager.DstFactor dstFactor, boolean variableOpacity) {
       this.srcFactor = srcFactor;
       this.dstFactor = dstFactor;
       this.canVaryOpacity = variableOpacity;
    }

    public static BlendingFunction getBlendFunc() {
        if (MinecraftClient.isFabulousGraphicsOrBetter()) {
            return SCREEN;
        }
        return WakesClient.CONFIG_INSTANCE.blendMode;
    }
}
