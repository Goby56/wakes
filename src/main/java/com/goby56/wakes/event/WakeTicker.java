package com.goby56.wakes.event;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.render.SplashPlaneRenderer;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.debug.WakesDebugInfo;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.world.ClientWorld;

public class WakeTicker implements ClientTickEvents.EndWorldTick {
    @Override
    public void onEndTick(ClientWorld world) {
        WakesClient.areShadersEnabled = WakesClient.areShadersEnabled();
        WakesDebugInfo.reset();
        WakeHandler.getInstance().tick();
        SplashPlaneRenderer.tick();
    }
}
