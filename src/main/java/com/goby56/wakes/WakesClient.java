package com.goby56.wakes;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.debug.WakeDebugRenderer;
import com.goby56.wakes.debug.WakesDebugInfo;
import com.goby56.wakes.event.WakeClientTicker;
import com.goby56.wakes.event.WakeWorldTicker;
import com.goby56.wakes.particle.ModParticles;
import com.goby56.wakes.render.SplashPlaneRenderer;
import com.goby56.wakes.render.WakeRenderer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WakesClient implements ClientModInitializer {

	public static final String MOD_ID = "wakes";
	public static ModMetadata METADATA;
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static boolean areShadersEnabled = false;
	public static final RenderPipeline GUI_HSV_PIPELINE = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
					.withLocation(Identifier.fromNamespaceAndPath("wakes", "pipeline/gui_hsv"))
					.withFragmentShader(Identifier.fromNamespaceAndPath("wakes", "gui_hsv"))
					.build()
	);
	public static final RenderPipeline SPLASH_PLANE_PIPELINE = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
					.withLocation("pipeline/beacon_beam_translucent")
					.withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.TRIANGLES)
					.withDepthWrite(false)
					.withCull(false)
					.withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
					.withBlend(BlendFunction.TRANSLUCENT)
					.build());

	public static WakeRenderer wakeRenderer;

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
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(new WakeWorldTicker());

		// Rendering events
		wakeRenderer = new WakeRenderer();
		WorldRenderEvents.BEFORE_TRANSLUCENT.register(wakeRenderer);
		WorldRenderEvents.END_MAIN.register(new SplashPlaneRenderer());

		SplashPlaneRenderer.initSplashPlane();
        DebugScreenEntries.register(
                Identifier.fromNamespaceAndPath("wakes", "debug_entry"),
                new WakesDebugInfo());
	}

	public static boolean areShadersEnabled() {
		if (FabricLoader.getInstance().isModLoaded("iris")) {
			return IrisApi.getInstance().getConfig().areShadersEnabled();
		}
		return false;
	}
}