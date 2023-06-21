package com.goby56.wakes;

import com.goby56.wakes.particle.ModParticles;
import com.goby56.wakes.render.model.WakeModel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WakesClient implements ClientModInitializer {

	public static final String MOD_ID = "wakes";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		LOGGER.info("Registering client specific stuff for " + MOD_ID);

		EntityModelLayerRegistry.registerModelLayer(WakeModel.MODEL_LAYER, WakeModel::getTexturedModelData);

		ModParticles.registerParticles();
	}
}