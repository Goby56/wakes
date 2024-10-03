package com.goby56.wakes;

import com.goby56.wakes.debug.DebugCommand;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.debug.WakeDebugRenderer;
import com.goby56.wakes.event.WakeTicker;
import com.goby56.wakes.render.WakeRenderer;
import eu.midnightdust.lib.config.MidnightConfig;
import ladysnake.satin.api.managed.ManagedCoreShader;
import ladysnake.satin.api.managed.ShaderEffectManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WakesClient implements ClientModInitializer {

	public static final String MOD_ID = "wakes";
	public static ModMetadata METADATA;
	public static final String CONFIG_PATH = String.format("%s/%s.json", FabricLoader.getInstance().getConfigDir().toString(), MOD_ID);
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final ManagedCoreShader TRANSLUCENT_NO_LIGHT_DIRECTION_PROGRAM = ShaderEffectManager.getInstance().manageCoreShader(
			new Identifier(MOD_ID, "translucent_no_light_direction"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
	public static boolean areShadersEnabled = false;

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(container -> METADATA = container.getMetadata());

		// Mod configs
		MidnightConfig.init(MOD_ID, WakesConfig.class);

		// Game events
		ClientTickEvents.END_WORLD_TICK.register(new WakeTicker());

		// Rendering events
		WorldRenderEvents.AFTER_TRANSLUCENT.register(new WakeRenderer());
		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(new WakeDebugRenderer());

		// Commands
		ClientCommandRegistrationCallback.EVENT.register(DebugCommand::register);
	}

	public static boolean areShadersEnabled() {
		if (FabricLoader.getInstance().isModLoaded("iris")) {
			return IrisApi.getInstance().getConfig().areShadersEnabled();
		}
		return false;
	}
}