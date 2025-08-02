package com.goby56.wakes;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.debug.WakeDebugRenderer;
import com.goby56.wakes.event.WakeClientTicker;
import com.goby56.wakes.event.WakeWorldTicker;
import com.goby56.wakes.particle.ModParticles;
import com.goby56.wakes.render.SplashPlaneRenderer;
import com.goby56.wakes.render.WakeRenderer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WakesClient implements ClientModInitializer {

	public static final String MOD_ID = "wakes";
	public static ModMetadata METADATA;
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static boolean areShadersEnabled = false;
	public static final RenderPipeline GUI_HSV_PIPELINE = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
					.withLocation(Identifier.of("wakes", "pipeline/gui_hsv"))
					.withFragmentShader(Identifier.of("wakes", "gui_hsv"))
					.build()
	);

	@Override
	public void onInitializeClient() {
		FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(container -> METADATA = container.getMetadata());

		// Mod configs
		MidnightConfig.init(WakesClient.MOD_ID, WakesConfig.class);

		// Particles
		ModParticles.registerParticles();

		// Wake handler handling
		ClientTickEvents.START_CLIENT_TICK.register(new WakeClientTicker());
		ClientTickEvents.END_WORLD_TICK.register(new WakeWorldTicker());

		// Rendering events
		WorldRenderEvents.AFTER_TRANSLUCENT.register(new WakeRenderer());
		WorldRenderEvents.AFTER_TRANSLUCENT.register(new SplashPlaneRenderer());
		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(new WakeDebugRenderer());
		WakeDebugRenderer.registerDebugTextureRenderer();

		SplashPlaneRenderer.initSplashPlane();
	}

	public static boolean areShadersEnabled() {
		if (FabricLoader.getInstance().isModLoaded("iris")) {
			return IrisApi.getInstance().getConfig().areShadersEnabled();
		}
		return false;
	}
}