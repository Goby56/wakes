package com.goby56.wakes;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.debug.WakeDebugRenderer;
import com.goby56.wakes.debug.WakesDebugInfo;
import com.goby56.wakes.event.WakeClientTicker;
import com.goby56.wakes.event.WakeWorldTicker;
import com.goby56.wakes.particle.ModParticles;
import com.goby56.wakes.render.FrustumManager;
import com.goby56.wakes.render.SplashPlaneRenderer;
import com.goby56.wakes.render.WakeRenderer;
import com.goby56.wakes.render.WakeTextureAtlas;
import com.goby56.wakes.simulation.WakeHandler;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.BindGroupLayouts;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

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

	// Pass 1: render all alpha values to color buffer, no depth write — faint wakes remain visible
	public static final RenderPipeline WAKE_COLOR_PIPELINE = buildWakeColorPipeline();
	public static final RenderType WAKE_COLOR_RENDER_TYPE = buildWakeColorRenderType();

	// Pass 2: depth-only — ALPHA_CUTOUT discards low-alpha pixels, survivors write depth but no color
	// (color already came from pass 1; writing it again here would double-blend and cause a visible seam at the cutoff)
	public static final RenderPipeline WAKE_PIPELINE = buildWakePipeline();
	public static final RenderType WAKE_RENDER_TYPE = buildWakeRenderType();

	private static RenderPipeline buildWakeColorPipeline() {
		return RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
				.withLocation(Identifier.fromNamespaceAndPath("wakes", "pipeline/entity_translucent_wake_color"))
				.withShaderDefine("PER_FACE_LIGHTING")
				.withBindGroupLayout(BindGroupLayouts.SAMPLER1)
				.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
				.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
				.withCull(false)
				.build()
		);
	}

	private static RenderType buildWakeColorRenderType() {
		RenderSetup setup = RenderSetup.builder(WAKE_COLOR_PIPELINE)
				.withTexture("Sampler0", WakeTextureAtlas.ATLAS_ID)
				.useLightmap()
				.useOverlay()
				.createRenderSetup();
		return RenderType.create("wakes:entity_translucent_wake_color", setup);
	}

	private static RenderPipeline buildWakePipeline() {
		return RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
					.withLocation(Identifier.fromNamespaceAndPath("wakes", "pipeline/entity_translucent_wake"))
					.withShaderDefine("ALPHA_CUTOUT", 0.8f)
					.withShaderDefine("PER_FACE_LIGHTING")
					.withBindGroupLayout(BindGroupLayouts.SAMPLER1)
					.withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_NONE))
					// .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
					.withCull(false)
					.build()
		);
	}

	private static RenderType buildWakeRenderType() {
		RenderSetup setup = RenderSetup.builder(WAKE_PIPELINE)
				.withTexture("Sampler0", WakeTextureAtlas.ATLAS_ID)
				.useLightmap()
				.useOverlay()
				.createRenderSetup();
		return RenderType.create("wakes:entity_translucent_wake", setup);
	}

	// Hull mask: depth-only, no ALPHA_CUTOUT (every fragment survives and writes depth).
	// Submitted before the wake color pass so it can occlude fresh wake nodes spawned
	// underneath a boat's open hull, without touching the boat's own (vanilla) geometry.
	public static final RenderPipeline HULL_MASK_PIPELINE = buildHullMaskPipeline();
	public static final RenderType HULL_MASK_RENDER_TYPE = buildHullMaskRenderType();

	private static RenderPipeline buildHullMaskPipeline() {
		return RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
					.withLocation(Identifier.fromNamespaceAndPath("wakes", "pipeline/hull_mask"))
					.withShaderDefine("PER_FACE_LIGHTING")
					.withBindGroupLayout(BindGroupLayouts.SAMPLER1)
					.withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_NONE))
					.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
					.withCull(false)
					.build()
		);
	}

	private static RenderType buildHullMaskRenderType() {
		RenderSetup setup = RenderSetup.builder(HULL_MASK_PIPELINE)
				.withTexture("Sampler0", WakeTextureAtlas.ATLAS_ID)
				.useLightmap()
				.useOverlay()
				.createRenderSetup();
		return RenderType.create("wakes:hull_mask", setup);
	}

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
		ClientTickEvents.END_LEVEL_TICK.register(new WakeWorldTicker());
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(new WakeWorldTicker());

		// Rendering events
		wakeRenderer = new WakeRenderer();
		LevelRenderEvents.COLLECT_SUBMITS.register(wakeRenderer);
		SplashPlaneRenderer splashPlaneRenderer = new SplashPlaneRenderer();
		LevelRenderEvents.COLLECT_SUBMITS.register(splashPlaneRenderer);

		FrustumManager frustumManager = new FrustumManager();
		LevelExtractionEvents.END_EXTRACTION.register(frustumManager);

		SplashPlaneRenderer.initSplashPlane();
        DebugScreenEntries.register(
                Identifier.fromNamespaceAndPath("wakes", "debug_entry"),
                new WakesDebugInfo());

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "atlas_debug"),
                WakeDebugRenderer::drawAtlasOverlay);
	}

	public static boolean areShadersEnabled() {
		if (FabricLoader.getInstance().isModLoaded("iris")) {
			return IrisApi.getInstance().getConfig().areShadersEnabled();
		}
		return false;
	}
}
