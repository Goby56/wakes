package com.goby56.wakes.event;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.debug.WakeDebugRenderer;
import com.goby56.wakes.debug.WakesDebugInfo;
import com.goby56.wakes.simulation.WakeHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public class WakeTickEvents implements ClientTickEvents.StartTick, ClientTickEvents.EndLevelTick {
    @Override
    public void onStartTick(Minecraft client) {
        if (client.level == null) {
            WakeHandler.kill();
            return;
        }
        WakeHandler.getInstance().ifPresentOrElse(wakeHandler -> {
            // Dimension change: Minecraft.level is swapped to a new ClientLevel instance
            if (wakeHandler.world != client.level) {
                WakeHandler.kill();
                System.out.println("NEW DIMENSION: " + wakeHandler.world);
                WakeHandler.init(client.level);
            }
        }, () -> WakeHandler.init(client.level));
    }

    @Override
    public void onEndTick(ClientLevel world) {
        WakesClient.areShadersEnabled = WakesClient.areShadersEnabled();
        WakesDebugInfo.reset();
        WakeHandler.getInstance().ifPresent(WakeHandler::tick);
        WakeDebugRenderer.addDebugGizmos();
    }
}
