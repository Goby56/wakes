package com.goby56.wakes;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.debug.WakeDebugRenderer;
import com.goby56.wakes.debug.WakesDebugInfo;
import com.goby56.wakes.event.WakeTickEvents;
import com.goby56.wakes.particle.ModParticles;
import com.goby56.wakes.render.FrustumManager;
import com.goby56.wakes.render.OcclusionDimensionsManager;
import com.goby56.wakes.render.SplashPlaneRenderer;
import com.goby56.wakes.render.WakeRenderer;
import com.goby56.wakes.render.WakeTextureAtlas;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.BindGroupLayouts;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
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
	// Used instead of pass 1 when "Improved Transparency" (OIT) is on, see docs/wake_transparency_tradeoffs.md
	public static final OitPipelineSet WAKE_OIT_PIPELINES = buildWakeOitPipelines();
	public static final RenderType WAKE_COLOR_RENDER_TYPE = buildWakeColorRenderType();

	// Pass 2: depth-only — ALPHA_CUTOUT discards low-alpha pixels, survivors write depth but no color
	public static final float WAKE_DEPTH_CUTOUT = 0.8f;
	// Iris replaces ALPHA_CUTOUT with its own alpha > 0.1 test on texture.a * vertex.a, so pass 2 emits this
	// vertex alpha to land that test on WAKE_DEPTH_CUTOUT. Vanilla cuts on texture.a alone, so it's unaffected
	public static final float WAKE_DEPTH_PASS_ALPHA = 0.1f / WAKE_DEPTH_CUTOUT;
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

	// Mirrors vanilla's OIT_ENTITY set, minus OIT_ENTITY_SNIPPET's ALPHA_CUTOUT 0.1 so faint wake keeps fading
	private static OitPipelineSet buildWakeOitPipelines() {
		RenderPipeline.Builder base = RenderPipeline.builder()
				.withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
				.withBindGroupLayout(BindGroupLayouts.FOG)
				.withBindGroupLayout(BindGroupLayouts.LIGHTING)
				.withBindGroupLayout(BindGroupLayouts.SAMPLER0)
				.withVertexShader("core/entity")
				.withFragmentShader("core/entity")
				.withVertexBinding(0, DefaultVertexFormat.ENTITY)
				.withPrimitiveTopology(PrimitiveTopology.QUADS)
				.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
				.withCull(false);
		OitPipelineSet set = OitPipelineSet.builder("wakes_wake", base)
				.withAccumulateModifier(builder -> builder
						.withShaderDefine("PER_FACE_LIGHTING")
						.withBindGroupLayout(BindGroupLayouts.SAMPLER1)
						.withBindGroupLayout(BindGroupLayouts.SAMPLER2))
				.build();
		RenderPipelines.register(set.depthBoundsPipeline());
		RenderPipelines.register(set.transmittancePipeline());
		RenderPipelines.register(set.accumulatePipeline());
		return set;
	}

	private static RenderType buildWakeColorRenderType() {
		RenderSetup setup = RenderSetup.builder(WAKE_COLOR_PIPELINE)
				.setOitPipelines(WAKE_OIT_PIPELINES)
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
					.withShaderDefine("ALPHA_CUTOUT", WAKE_DEPTH_CUTOUT)
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

	public static WakeRenderer wakeRenderer;

	@Override
	public void onInitializeClient() {
		FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(container -> METADATA = container.getMetadata());

		// Mod configs
		MidnightConfig.init(WakesClient.MOD_ID, WakesConfig.class);

		// Particles
		ModParticles.registerParticles();

		// Wake handler handling
		ClientTickEvents.START_CLIENT_TICK.register(new WakeTickEvents());
		ClientTickEvents.END_LEVEL_TICK.register(new WakeTickEvents());

		// Tell Iris what our custom wake pipelines actually are, instead of leaving it to guess
		// via internal fuzzy-matching (ShaderKey.findBestMatch), since that's what several shader
		// packs were choking on. See docs/shader_compat.md.
		if (FabricLoader.getInstance().isModLoaded("iris")) {
			IrisApi.getInstance().assignPipeline(WAKE_COLOR_PIPELINE, IrisProgram.ENTITIES_TRANSLUCENT);
			// No ENTITIES_CUTOUT in the public IrisProgram enum, so ENTITIES is the closest
			// (solid/depth-writing) category for this alpha-cutout, depth-only pass.
			IrisApi.getInstance().assignPipeline(WAKE_PIPELINE, IrisProgram.ENTITIES);
		}

		// Rendering events
		wakeRenderer = new WakeRenderer();
		LevelRenderEvents.COLLECT_SUBMITS.register(wakeRenderer);
		SplashPlaneRenderer splashPlaneRenderer = new SplashPlaneRenderer();
		LevelRenderEvents.COLLECT_SUBMITS.register(splashPlaneRenderer);

		// Hooked to render (not tick) so it can interpolate to partial-tick like the boat model
		// itself does, instead of snapping to a new position/yaw only once per game tick.
		LevelRenderEvents.COLLECT_SUBMITS.register(WakeDebugRenderer::addOcclusionZoneGizmos);

		FrustumManager frustumManager = new FrustumManager();
		LevelExtractionEvents.END_EXTRACTION.register(frustumManager);

		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
				OcclusionDimensionsManager.ID, new OcclusionDimensionsManager());

		SplashPlaneRenderer.initSplashPlane();
        DebugScreenEntries.register(
                Identifier.fromNamespaceAndPath("wakes", "debug_entry"),
                new WakesDebugInfo());

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "atlas_debug"),
                WakeDebugRenderer::drawAtlasOverlay);
	}

	// OIT already makes near-opaque wake occlude water, so the depth-only pass would only add back a hard edge
	public static boolean useDepthPass() {
		return !Minecraft.getInstance().gameRenderer.useImprovedTransparency();
	}

	public static boolean areShadersEnabled() {
		if (FabricLoader.getInstance().isModLoaded("iris")) {
			return IrisApi.getInstance().getConfig().areShadersEnabled();
		}
		return false;
	}
}
