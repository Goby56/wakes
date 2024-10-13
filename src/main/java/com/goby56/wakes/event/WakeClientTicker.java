package com.goby56.wakes.event;

import com.goby56.wakes.simulation.WakeHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class WakeClientTicker implements ClientTickEvents.StartTick {
    @Override
    public void onStartTick(MinecraftClient client) {
        if (client.world == null) {
            WakeHandler.kill();
        } else if (WakeHandler.getInstance().isEmpty()) {
            WakeHandler.init(client.world);
        }
    }
}
