package com.goby56.wakes.render;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.util.StringIdentifiable;

public enum BlendingFunction {
    DEFAULT(),
    SCREEN(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR);

    public final GlStateManager.SrcFactor srcFactor;
    public final GlStateManager.DstFactor dstFactor;

    BlendingFunction() {
        this.srcFactor = null;
        this.dstFactor = null;
    }

    BlendingFunction(GlStateManager.SrcFactor srcFactor, GlStateManager.DstFactor dstFactor) {
       this.srcFactor = srcFactor;
       this.dstFactor = dstFactor;
    }
}
