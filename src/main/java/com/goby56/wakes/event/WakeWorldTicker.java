package com.goby56.wakes.event;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.debug.WakesDebugInfo;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

public class WakeWorldTicker implements ClientTickEvents.EndWorldTick, ServerEntityWorldChangeEvents.AfterPlayerChange {

    @Override
    public void onEndTick(ClientLevel world) {
        WakesClient.areShadersEnabled = WakesClient.areShadersEnabled();
        WakesDebugInfo.reset();
        WakeHandler.getInstance().ifPresent(WakeHandler::tick);
    }

    @Override
    public void afterChangeWorld(ServerPlayer player, ServerLevel origin, ServerLevel destination) {
        WakeHandler.init(destination);
    }
}
