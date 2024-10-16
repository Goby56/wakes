package com.goby56.wakes.event;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfigScreen;
import com.goby56.wakes.render.SplashPlaneRenderer;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.debug.WakesDebugInfo;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class WakeWorldTicker implements ClientTickEvents.EndWorldTick, ServerEntityWorldChangeEvents.AfterPlayerChange {
    public static boolean openConfigScreenNextTick = false;

    @Override
    public void onEndTick(ClientWorld world) {
        WakesClient.areShadersEnabled = WakesClient.areShadersEnabled();
        WakesDebugInfo.reset();
        SplashPlaneRenderer.tick();
        WakeHandler.getInstance().ifPresent(WakeHandler::tick);
        if (openConfigScreenNextTick) {
            MinecraftClient.getInstance().setScreenAndRender(new WakesConfigScreen());
            openConfigScreenNextTick = false;
        }
    }

    @Override
    public void afterChangeWorld(ServerPlayerEntity player, ServerWorld origin, ServerWorld destination) {
        WakeHandler.init(destination);
    }
}
