package com.goby56.wakes.mixin;

import com.goby56.wakes.duck.LightmapAccess;
import com.goby56.wakes.render.LightmapInfo;
import net.minecraft.client.renderer.Lightmap;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Lightmap.class)
public abstract class LightmapTextureManagerMixin implements LightmapAccess {
    @Override
    public LightmapInfo wakes$getLightmapInfo() {
        return null;
    }
}
