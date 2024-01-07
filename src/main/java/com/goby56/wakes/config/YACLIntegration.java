package com.goby56.wakes.config;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.enums.EffectSpawningRule;
import com.goby56.wakes.config.enums.Resolution;
import com.goby56.wakes.render.enums.BlendingFunction;
import com.goby56.wakes.render.enums.RenderType;
import com.goby56.wakes.render.enums.WakeColor;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import com.goby56.wakes.utils.*;
import com.mojang.blaze3d.platform.GlStateManager;
import dev.isxander.yacl.api.*;
import dev.isxander.yacl.gui.controllers.*;
import dev.isxander.yacl.gui.controllers.cycling.EnumController;
import dev.isxander.yacl.gui.controllers.slider.FloatSliderController;
import dev.isxander.yacl.gui.controllers.slider.IntegerSliderController;
import dev.isxander.yacl.gui.controllers.string.number.DoubleFieldController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class YACLIntegration {
    public static Screen createScreen(Screen parent) {
        WakesConfig config = WakesClient.CONFIG_INSTANCE;
        boolean isUsingCustomBlendFunc = config.blendFunc == BlendingFunction.CUSTOM;
        Option<Integer> wakeOpacityOption = optionOf(Integer.class, "wake_opacity")
                .binding(100, () -> (int) (config.wakeOpacity * 100), val -> config.wakeOpacity = val / 100f)
                .controller(opt -> integerSlider(opt, 0, 100))
                .available(config.blendFunc.canVaryOpacity)
                .build();
        Option<GlStateManager.SrcFactor> srcFactorOption = optionOf(GlStateManager.SrcFactor.class, "src_factor")
                .binding(GlStateManager.SrcFactor.SRC_ALPHA, () -> config.srcFactor, val -> config.srcFactor = val)
                .controller(EnumController::new)
                .build();
        Option<GlStateManager.DstFactor> dstFactorOption = optionOf(GlStateManager.DstFactor.class, "dst_factor")
                .binding(GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, () -> config.dstFactor, val -> config.dstFactor = val)
                .controller(EnumController::new)
                .build();
        return YetAnotherConfigLib.createBuilder()
                .title(WakesUtils.translatable("config", "title"))
                .category(configCategory("basic")
                        .group(group("wake_appearance")
                                .option(optionOf(Resolution.class, "wake_resolution")
                                        .binding(Resolution.SIXTEEN, () -> config.wakeResolution, WakeHandler::scheduleResolutionChange)
                                        .controller(opt -> new EnumController<>(opt, val -> Text.of(val.toString())))
                                        .build())
                                .option(wakeOpacityOption)
                                .option(optionOf(BlendingFunction.class, "blending_function")
                                        .binding(BlendingFunction.DEFAULT, () -> config.blendFunc, val -> {
                                            config.blendFunc = val;
                                            wakeOpacityOption.setAvailable(val.canVaryOpacity);
                                        })
                                        .controller(opt -> new EnumController<>(opt, val -> WakesUtils.translatable("blending_function", val.name().toLowerCase())))
                                        .build())
                                .option(booleanOption("use_water_blending")
                                        .binding(true, () -> config.useWaterBlending, val -> config.useWaterBlending = val)
                                        .build())
                                .build())
                        .group(group("effect_spawning")
                                .option(effectSpawningRuleOption("boat"))
                                .option(effectSpawningRuleOption("player"))
                                .option(effectSpawningRuleOption("other_players"))
                                .option(effectSpawningRuleOption("mobs"))
                                .option(effectSpawningRuleOption("items"))
                                .option(booleanOption("wakes_in_running_water")
                                        .binding(false, () -> config.wakesInRunningWater, val -> config.wakesInRunningWater = val)
                                        .build())
                                .option(booleanOption("spawn_particles")
                                        .binding(true, () -> config.spawnParticles, val -> config.spawnParticles = val)
                                        .build())
                                .build())
                        .build())
                .category(configCategory("advanced")
                        .group(group("wake_behaviour")
                                .option(optionOf(Float.class, "wave_propagation_factor")
                                        .binding(0.95f, () -> config.wavePropagationFactor, val -> {
                                            config.wavePropagationFactor = val;
                                            WakeNode.calculateWaveDevelopmentFactors();
                                        })
                                        .controller(opt -> floatSlider(opt, 0f, 2f, 0.01f))
                                        .build())
                                .option(optionOf(Float.class, "wave_decay_factor")
                                        .binding(0.5f, () -> config.waveDecayFactor, val -> config.waveDecayFactor = val)
                                        .controller(opt -> floatSlider(opt, 0f, 1f, 0.01f))
                                        .build())
                                .build())
                        .group(group("splash_plane")
                                .option(optionOf(Float.class, "splash_plane.cap_velocity")
                                        .binding(0.5f, () -> config.maxSplashPlaneVelocity, val -> config.maxSplashPlaneVelocity = val)
                                        .controller(opt -> floatSlider(opt, 0.1f, 2f, 0.1f))
                                        .build())
                                .option(optionOf(Float.class, "splash_plane.scale")
                                        .binding(1f, () -> config.splashPlaneScale, val -> config.splashPlaneScale = val)
                                        .controller(opt -> floatSlider(opt, 0.1f, 2f, 0.1f))
                                        .build())
                                .option(optionOf(Float.class, "splash_plane.offset")
                                        .binding(0f, () -> config.splashPlaneOffset, val -> config.splashPlaneOffset = val)
                                        .controller(opt -> floatSlider(opt, -1f, 1f, 0.1f))
                                        .build())
                                .option(optionOf(Float.class, "splash_plane.width")
                                        .binding(3f, () -> config.splashPlaneWidth, val -> config.splashPlaneWidth = val)
                                        .controller(opt -> floatSlider(opt, 0f, 10f, 0.1f))
                                        .build())
                                .option(optionOf(Float.class, "splash_plane.height")
                                        .binding(1.5f, () -> config.splashPlaneHeight, val -> config.splashPlaneHeight = val)
                                        .controller(opt -> floatSlider(opt, 0f, 10f, 0.1f))
                                        .build())
                                .option(optionOf(Float.class, "splash_plane.depth")
                                        .binding(2f, () -> config.splashPlaneDepth, val -> config.splashPlaneDepth = val)
                                        .controller(opt -> floatSlider(opt, 0f, 10f, 0.1f))
                                        .build())
                                .build())
                        .group(group("initial_wave_strengths")
                                .option(optionOf(Integer.class, "initial_wave_strength.wake")
                                        .binding(20, () -> config.initialStrength, val -> config.initialStrength = val)
                                        .controller(opt -> integerSlider(opt, 0, 150))
                                        .build())
                                .option(optionOf(Integer.class, "initial_wave_strength.paddle")
                                        .binding(100, () -> config.paddleStrength, val -> config.paddleStrength = val)
                                        .controller(opt -> integerSlider(opt, 0, 150))
                                        .build())
                                .option(optionOf(Integer.class, "initial_wave_strength.splash")
                                        .binding(100, () -> config.splashStrength, val -> config.splashStrength = val)
                                        .controller(opt -> integerSlider(opt, 0, 150))
                                        .build())
                                .build())
                        .build())
                .category(configCategory("debug")
                        .option(optionOf(RenderType.class, "render_type")
                                .binding(RenderType.AUTO, () -> config.renderType, val -> config.renderType = val)
                                .controller(EnumController::new)
                                .build())
                        .option(optionOf(Integer.class, "flood_fill_distance")
                                .binding(3, () -> config.floodFillDistance, val -> config.floodFillDistance = val)
                                .controller(opt -> integerSlider(opt, 1, 5))
                                .build())
                        .option(optionOf(Integer.class, "ticks_before_fill")
                                .binding(2, () -> config.ticksBeforeFill, val -> config.ticksBeforeFill = val)
                                .controller(opt -> integerSlider(opt, 1, 5))
                                .build())
                        .option(booleanOption("use_9_point_stencil")
                                .binding(true, () -> config.use9PointStencil, val -> config.use9PointStencil = val)
                                .build())
                        .option(booleanOption("draw_debug_boxes")
                                .binding(false, () -> config.drawDebugBoxes, val -> config.drawDebugBoxes = val)
                                .build())
                        .option(booleanOption("disable_mod")
                                .binding(false, () -> config.disableMod, val -> config.disableMod = val)
                                .build())
                        // TODO SWITCH TO OPTIONIF
                        .option(srcFactorOption)
                        .option(dstFactorOption)
                        .group(intervalGroup(0, WakeColor.TRANSPARENT, -50, -45))
                        .group(intervalGroup(1, WakeColor.DARK_GRAY, -45, -35))
                        .group(intervalGroup(2, WakeColor.GRAY, -35, -30))
                        .group(intervalGroup(3, WakeColor.LIGHT_GRAY, -30, -15))
                        .group(intervalGroup(4, WakeColor.TRANSPARENT, -15, 2))
                        .group(intervalGroup(5, WakeColor.LIGHT_GRAY, 2, 10))
                        .group(intervalGroup(6, WakeColor.WHITE, 10, 20))
                        .group(intervalGroup(7, WakeColor.LIGHT_GRAY, 20, 40))
                        .group(intervalGroup(8, WakeColor.GRAY, 40, 50))
                        .build())
                .save(config::saveConfig)
                .build()
                .generateScreen(parent);
    }

    private static ConfigCategory.Builder configCategory(String name) {
        return ConfigCategory.createBuilder()
                .name(WakesUtils.translatable("config.category", name));
    }

    private static OptionGroup.Builder group(String name) {
        return OptionGroup.createBuilder()
                .name(WakesUtils.translatable("config.group", name));
    }

    private static <T> Option.Builder<T> optionOf(Class<T> optionType, String name) {
        return Option.createBuilder(optionType)
                .name(WakesUtils.translatable("config.option", name));
    }

    private static IntegerSliderController integerSlider(Option<Integer> option, int min, int max) {
        return new IntegerSliderController(option, min, max, 1);
    }

    private static FloatSliderController floatSlider(Option<Float> option, float min, float max, float step) {
        return new FloatSliderController(option, min, max, step);
    }

    private static Option.Builder<Boolean> booleanOption(String name) {
        return Option.createBuilder(Boolean.class)
                .name(WakesUtils.translatable("config.option", name))
                .controller(TickBoxController::new);
    }

    private static Option<EffectSpawningRule> effectSpawningRuleOption(String name) {
        WakesConfig config = WakesClient.CONFIG_INSTANCE;
        return Option.createBuilder(EffectSpawningRule.class)
                .name(WakesUtils.translatable("config.option.effect_spawning_rules.source", name))
                .binding(EffectSpawningRule.SIMULATION_AND_PLANES, () -> config.effectSpawningRules.get(name), val -> config.effectSpawningRules.put(name, val))
                .controller(opt -> new EnumController(opt, val -> WakesUtils.translatable("config.option.effect_spawning_rules.effect", val.toString().toLowerCase())))
                .build();
    }

    private static OptionGroup intervalGroup(int n, WakeColor defaultColor, int defaultLower, int defaultUpper) {
        WakesConfig config = WakesClient.CONFIG_INSTANCE;
        return OptionGroup.createBuilder()
                .name(Text.of(String.valueOf(n+1)))
                .option(optionOf(Integer.class, "interval.lower")
                        .binding(defaultLower, () -> config.colorIntervals.get(n).lower, config.colorIntervals.get(n)::setLower)
                        .controller(opt -> integerSlider(opt, -50, 50))
                        .build())
                .option(optionOf(Integer.class, "interval.upper")
                        .binding(defaultUpper, () -> config.colorIntervals.get(n).upper, config.colorIntervals.get(n)::setUpper)
                        .controller(opt -> integerSlider(opt, -50, 50))
                        .build())
                .option(optionOf(WakeColor.class, "interval.color")
                        .binding(defaultColor, () -> config.colorIntervals.get(n).color, config.colorIntervals.get(n)::setColor)
                        .controller(EnumController::new)
                        .build())
                .build();
    }
}
