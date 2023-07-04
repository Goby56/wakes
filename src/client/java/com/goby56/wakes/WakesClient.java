package com.goby56.wakes;

import com.goby56.wakes.event.WakeTicker;
import com.goby56.wakes.particle.ModParticles;
import com.goby56.wakes.render.WakeTextureRenderer;
import com.goby56.wakes.render.debug.WakeDebugRenderer;
import com.goby56.wakes.render.model.WakeModel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WakesClient implements ClientModInitializer {

	public static final String MOD_ID = "wakes";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		LOGGER.info("Registering client specific stuff for " + MOD_ID);

		// Mod configs
//		AutoConfig.register(WakesConfig.class, GsonConfigSerializer::new);

		EntityModelLayerRegistry.registerModelLayer(WakeModel.MODEL_LAYER, WakeModel::getTexturedModelData);

		ModParticles.registerParticles();

		// Game events
		ClientTickEvents.END_WORLD_TICK.register(new WakeTicker());

		// Rendering events
		WorldRenderEvents.AFTER_TRANSLUCENT.register(new WakeTextureRenderer());
		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(new WakeDebugRenderer());

		// Commands
//		ClientCommandRegistrationCallback.EVENT.register(SpawnWakesCommand::register);
	}

	public static boolean isYACLLoaded() {
		return FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3");
	}
}