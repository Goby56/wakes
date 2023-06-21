package com.goby56.wakes;

import net.fabricmc.api.ClientModInitializer;

public class WakesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		Wakes.LOGGER.info("Registering client specific stuff for " + Wakes.MOD_ID);
	}
}