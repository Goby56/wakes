package com.goby56.wakes.event;

import com.goby56.wakes.simulation.WakeHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public class WakeClientTicker implements ClientTickEvents.StartTick {
    @Override
    public void onStartTick(Minecraft client) {
        if (client.level == null) {
            WakeHandler.kill();
        } else if (WakeHandler.getInstance().isEmpty()) {
            WakeHandler.init(client.level);
        }
    }
}
