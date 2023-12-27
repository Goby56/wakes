package com.goby56.wakes.event;

import com.goby56.wakes.render.SplashPlaneRenderer;
import com.goby56.wakes.render.WakeTextureRenderer;
import com.goby56.wakes.utils.WakeHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.world.ClientWorld;

public class WakeTicker implements ClientTickEvents.EndWorldTick {
    @Override
    public void onEndTick(ClientWorld world) {
        WakeHandler.getInstance().tick();
        SplashPlaneRenderer.tick();
    }
}
