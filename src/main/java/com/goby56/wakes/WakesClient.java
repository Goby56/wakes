package com.goby56.wakes;

import com.goby56.wakes.config.WakesMidnightConfig;
import com.goby56.wakes.debug.DebugCommand;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.debug.WakeDebugRenderer;
import com.goby56.wakes.event.PickBoat;
import com.goby56.wakes.event.WakeClientTicker;
import com.goby56.wakes.event.WakeWorldTicker;
import com.goby56.wakes.particle.ModParticles;
import com.goby56.wakes.render.SplashPlaneRenderer;
import com.goby56.wakes.render.WakeRenderer;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPickBlockGatherCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.ladysnake.satin.api.managed.ManagedCoreShader;
import org.ladysnake.satin.api.managed.ShaderEffectManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WakesClient implements ClientModInitializer {

	public static final String MOD_ID = "wakes";
	public static ModMetadata METADATA;
	public static final String CONFIG_PATH = String.format("%s/%s.json", FabricLoader.getInstance().getConfigDir().toString(), MOD_ID);
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static WakesConfig CONFIG_INSTANCE;
	public static final ManagedCoreShader TRANSLUCENT_NO_LIGHT_DIRECTION_PROGRAM = ShaderEffectManager.getInstance().manageCoreShader(
			Identifier.of(MOD_ID, "translucent_no_light_direction"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
	public static final ManagedCoreShader POSITION_TEXTURE_HSV = ShaderEffectManager.getInstance().manageCoreShader(
			Identifier.of(MOD_ID, "position_tex_hsv"), VertexFormats.POSITION_TEXTURE_COLOR);
	public static boolean areShadersEnabled = false;

	@Override
	public void onInitializeClient() {
		FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(container -> METADATA = container.getMetadata());

		// Mod configs
		CONFIG_INSTANCE = WakesConfig.loadConfig();
		MidnightConfig.init("wakes", WakesMidnightConfig.class);

		// Particles
		ModParticles.registerParticles();

		// Wake handler handling
		ClientTickEvents.START_CLIENT_TICK.register(new WakeClientTicker());
		ClientTickEvents.END_WORLD_TICK.register(new WakeWorldTicker());

		// Game events
		ClientPickBlockGatherCallback.EVENT.register(new PickBoat());

		// Rendering events
		WorldRenderEvents.AFTER_TRANSLUCENT.register(new WakeRenderer());
		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(new WakeDebugRenderer());

		SplashPlaneRenderer.initSplashPlane();

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