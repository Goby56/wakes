package com.goby56.wakes.config;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.enums.EffectSpawningRule;
import com.goby56.wakes.config.enums.Resolution;
import com.goby56.wakes.render.enums.RenderType;
import com.goby56.wakes.render.enums.WakeColor;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import com.goby56.wakes.utils.*;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class YACLIntegration {
    public static Screen createScreen(Screen parent) {
        WakesConfig config = WakesClient.CONFIG_INSTANCE;
        return YetAnotherConfigLib.createBuilder()
                .title(WakesUtils.translatable("config", "title"))
                .category(configCategory("basic")
                        .group(group("wake_appearance")
                                .option(booleanOption("first_person_splash_plane", true)
                                        .binding(false, () -> config.firstPersonSplashPlane, val -> config.firstPersonSplashPlane = val)
                                        .build())
                                .option(optionOf(Resolution.class, "wake_resolution", true)
                                        .binding(Resolution.SIXTEEN, () -> config.wakeResolution, WakeHandler::scheduleResolutionChange)
                                        .controller(opt -> EnumControllerBuilder.create(opt)
                                                .enumClass(Resolution.class)
                                                .formatValue(val -> Text.of(val.toString())))
                                        .build())
                                .option(optionOf(Integer.class, "wake_opacity", false)
                                        .binding(100, () -> (int) (config.wakeOpacity * 100), val -> config.wakeOpacity = val / 100f)
                                        .controller(opt -> integerSlider(opt, 0, 100))
                                        .build())
                                .build())
                        .group(group("effect_spawning")
                                .option(effectSpawningRuleOption("boat"))
                                .option(effectSpawningRuleOption("player"))
                                .option(effectSpawningRuleOption("other_players"))
                                .option(effectSpawningRuleOption("mobs"))
                                .option(effectSpawningRuleOption("items"))
                                .option(booleanOption("wakes_in_running_water", false)
                                        .binding(false, () -> config.wakesInRunningWater, val -> config.wakesInRunningWater = val)
                                        .build())
                                .option(booleanOption("spawn_particles", false)
                                        .binding(true, () -> config.spawnParticles, val -> config.spawnParticles = val)
                                        .build())
                                .build())
                        .build())
                .category(configCategory("advanced")
                        .group(group("wake_behaviour")
                                .option(optionOf(Float.class, "wave_propagation_factor", true)
                                        .binding(0.95f, () -> config.wavePropagationFactor, val -> {
                                            config.wavePropagationFactor = val;
                                            WakeNode.calculateWaveDevelopmentFactors();
                                        })
                                        .controller(opt -> floatSlider(opt, 0f, 2f, 0.01f))
                                        .build())
                                .option(optionOf(Float.class, "wave_decay_factor", true)
                                        .binding(0.5f, () -> config.waveDecayFactor, val -> config.waveDecayFactor = val)
                                        .controller(opt -> floatSlider(opt, 0f, 1f, 0.01f))
                                        .build())
                                .build())
                        .group(group("splash_plane")
                                .option(optionOf(Float.class, "splash_plane.cap_velocity", true)
                                        .binding(0.5f, () -> config.maxSplashPlaneVelocity, val -> config.maxSplashPlaneVelocity = val)
                                        .controller(opt -> floatSlider(opt, 0.1f, 2f, 0.1f))
                                        .build())
                                .option(optionOf(Float.class, "splash_plane.scale", false)
                                        .binding(1f, () -> config.splashPlaneScale, val -> config.splashPlaneScale = val)
                                        .controller(opt -> floatSlider(opt, 0.1f, 2f, 0.1f))
                                        .build())
                                .option(optionOf(Float.class, "splash_plane.offset", false)
                                        .binding(0f, () -> config.splashPlaneOffset, val -> config.splashPlaneOffset = val)
                                        .controller(opt -> floatSlider(opt, -1f, 1f, 0.1f))
                                        .build())
                                .option(optionOf(Float.class, "splash_plane.width", false)
                                        .binding(3f, () -> config.splashPlaneWidth, val -> config.splashPlaneWidth = val)
                                        .controller(opt -> floatSlider(opt, 0f, 10f, 0.1f))
                                        .build())
                                .option(optionOf(Float.class, "splash_plane.height", false)
                                        .binding(1.5f, () -> config.splashPlaneHeight, val -> config.splashPlaneHeight = val)
                                        .controller(opt -> floatSlider(opt, 0f, 10f, 0.1f))
                                        .build())
                                .option(optionOf(Float.class, "splash_plane.depth", false)
                                        .binding(2f, () -> config.splashPlaneDepth, val -> config.splashPlaneDepth = val)
                                        .controller(opt -> floatSlider(opt, 0f, 10f, 0.1f))
                                        .build())
                                .build())
                        .group(group("initial_wave_strengths")
                                .description(description("initial_wave_strengths")
                                        .build())
                                .option(optionOf(Integer.class, "initial_wave_strength.wake", false)
                                        .binding(20, () -> config.initialStrength, val -> config.initialStrength = val)
                                        .controller(opt -> integerSlider(opt, 0, 150))
                                        .build())
                                .option(optionOf(Integer.class, "initial_wave_strength.paddle", false)
                                        .binding(100, () -> config.paddleStrength, val -> config.paddleStrength = val)
                                        .controller(opt -> integerSlider(opt, 0, 150))
                                        .build())
                                .option(optionOf(Integer.class, "initial_wave_strength.splash", false)
                                        .binding(100, () -> config.splashStrength, val -> config.splashStrength = val)
                                        .controller(opt -> integerSlider(opt, 0, 150))
                                        .build())
                                .build())
                        .build())
                .category(configCategory("debug")
                        .option(optionOf(RenderType.class, "render_type", false)
                                .binding(RenderType.AUTO, () -> config.renderType, val -> config.renderType = val)
                                .controller(opt -> EnumControllerBuilder.create(opt)
                                        .enumClass(RenderType.class))
                                .build())
                        .option(optionOf(Integer.class, "flood_fill_distance", false)
                                .binding(3, () -> config.floodFillDistance, val -> config.floodFillDistance = val)
                                .controller(opt -> integerSlider(opt, 1, 5))
                                .build())
                        .option(optionOf(Integer.class, "ticks_before_fill", false)
                                .binding(2, () -> config.ticksBeforeFill, val -> config.ticksBeforeFill = val)
                                .controller(opt -> integerSlider(opt, 1, 5))
                                .build())
                        .option(booleanOption("draw_debug_boxes", false)
                                .binding(false, () -> config.drawDebugBoxes, val -> config.drawDebugBoxes = val)
                                .build())
                        .option(booleanOption("disable_mod", false)
                                .binding(false, () -> config.disableMod, val -> config.disableMod = val)
                                .build())
                        .option(booleanOption("use_lods", false)
                                .binding(false, () -> config.useLODs, val -> config.useLODs = val)
                                .build())
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

    private static OptionDescription.Builder description(String name) {
        return OptionDescription.createBuilder()
                .text(WakesUtils.translatable("config.description", name));
    }

    private static OptionDescription.Builder description(String parent, String[] names) {
        // TODO ADD IMAGES
        OptionDescription.Builder desc = OptionDescription.createBuilder();
        for (String name : names) {
            desc.text(WakesUtils.translatable("config.description", String.format("%s.%s", parent, name)));
        }
        return desc;
    }

    private static ConfigCategory.Builder configCategory(String name) {
        return ConfigCategory.createBuilder()
                .name(WakesUtils.translatable("config.category", name));
    }

    private static OptionGroup.Builder group(String name) {
        return OptionGroup.createBuilder()
                .name(WakesUtils.translatable("config.group", name));
    }

    private static <T> Option.Builder<T> optionOf(Class<T> optionType, String name, boolean desc) {
        Option.Builder<T> opt = Option.<T>createBuilder();
        if (desc) opt.description(description(name).build());
        return opt.name(WakesUtils.translatable("config.option", name));
    }

    private static IntegerSliderControllerBuilder integerSlider(Option<Integer> option, int min, int max) {
        return IntegerSliderControllerBuilder.create(option)
                .range(min, max)
                .step(1);
    }

    private static FloatSliderControllerBuilder floatSlider(Option<Float> option, float min, float max, float step) {
        return FloatSliderControllerBuilder.create(option)
                .range(min, max)
                .step(step);
    }

    private static Option.Builder<Boolean> booleanOption(String name, boolean desc) {
        Option.Builder<Boolean> opt = Option.<Boolean>createBuilder()
                .controller(BooleanControllerBuilder::create);
        if (desc) opt.description(description(name).build());
        return opt.name(WakesUtils.translatable("config.option", name));
    }

    private static Option<EffectSpawningRule> effectSpawningRuleOption(String name) {
        WakesConfig config = WakesClient.CONFIG_INSTANCE;
        return Option.<EffectSpawningRule>createBuilder()
                .name(WakesUtils.translatable("config.option.effect_spawning_rules.source", name))
                .description(description("effect_spawning_rules").build())
                .binding(EffectSpawningRule.SIMULATION_AND_PLANES, () -> config.effectSpawningRules.get(name), val -> config.effectSpawningRules.put(name, val))
                .controller(opt -> EnumControllerBuilder.create(opt)
                        .enumClass(EffectSpawningRule.class)
                        .formatValue(val -> WakesUtils.translatable("config.option.effect_spawning_rules.effect", val.toString().toLowerCase())))
                .build();
    }

    private static OptionGroup intervalGroup(int n, WakeColor defaultColor, int defaultLower, int defaultUpper) {
        WakesConfig config = WakesClient.CONFIG_INSTANCE;
        return OptionGroup.createBuilder()
                .name(Text.of(String.valueOf(n+1)))
                .option(optionOf(Integer.class, "interval.lower", false)
                        .binding(defaultLower, () -> config.colorIntervals.get(n).lower, config.colorIntervals.get(n)::setLower)
                        .controller(opt -> integerSlider(opt, -50, 50))
                        .build())
                .option(optionOf(Integer.class, "interval.upper", false)
                        .binding(defaultUpper, () -> config.colorIntervals.get(n).upper, config.colorIntervals.get(n)::setUpper)
                        .controller(opt -> integerSlider(opt, -50, 50))
                        .build())
                .option(optionOf(WakeColor.class, "interval.color", false)
                        .binding(defaultColor, () -> config.colorIntervals.get(n).color, config.colorIntervals.get(n)::setColor)
                        .controller(opt -> EnumControllerBuilder.create(opt)
                                .enumClass(WakeColor.class))
                        .build())
                .build();
    }
}
