package com.goby56.wakes;

import com.goby56.wakes.command.DebugCommand;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.event.WakeTicker;
import com.goby56.wakes.particle.ModParticles;
import com.goby56.wakes.render.SplashPlaneRenderer;
import com.goby56.wakes.render.WakeTextureRenderer;
import com.goby56.wakes.render.debug.WakeDebugRenderer;
import com.goby56.wakes.render.model.WakeModel;
import ladysnake.satin.api.managed.ManagedCoreShader;
import ladysnake.satin.api.managed.ShaderEffectManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WakesClient implements ClientModInitializer {

	public static final String MOD_ID = "wakes";
	public static ModMetadata METADATA;
	public static final String CONFIG_PATH = String.format("%s/%s.json", FabricLoader.getInstance().getConfigDir().toString(), MOD_ID);
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static WakesConfig CONFIG_INSTANCE;
	public static final ManagedCoreShader TRANSLUCENT_NO_LIGHT_DIRECTION_PROGRAM = ShaderEffectManager.getInstance().manageCoreShader(
			new Identifier(MOD_ID, "translucent_no_light_direction"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(container -> METADATA = container.getMetadata());

		// Mod configs
//		AutoConfig.register(WakesConfig.class, GsonConfigSerializer::new);
		CONFIG_INSTANCE = WakesConfig.loadConfig();

		EntityModelLayerRegistry.registerModelLayer(WakeModel.MODEL_LAYER, WakeModel::getTexturedModelData);

		// Particles
		ModParticles.registerParticles();

		// Game events
		ClientTickEvents.END_WORLD_TICK.register(new WakeTicker());

		// Rendering events
		WorldRenderEvents.AFTER_TRANSLUCENT.register(new WakeTextureRenderer());
		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(new WakeDebugRenderer());

		ClientLifecycleEvents.CLIENT_STARTED.register(new SplashPlaneRenderer());

		// Commands
		ClientCommandRegistrationCallback.EVENT.register(DebugCommand::register);
	}

	public static boolean isYACLLoaded() {
		return FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3");
	}

	public static boolean areShadersEnabled() {
		if (FabricLoader.getInstance().isModLoaded("iris")) {
			return IrisApi.getInstance().getConfig().areShadersEnabled();
		}
		return false;
	}
}