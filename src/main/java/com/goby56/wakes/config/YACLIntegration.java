package com.goby56.wakes.config;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.utils.WakeColor;
import com.goby56.wakes.utils.WakeHandler;
import com.goby56.wakes.utils.WakeNode;
import com.goby56.wakes.utils.WakesUtils;
import dev.isxander.yacl.api.*;
import dev.isxander.yacl.gui.controllers.*;
import dev.isxander.yacl.gui.controllers.cycling.EnumController;
import dev.isxander.yacl.gui.controllers.slider.FloatSliderController;
import dev.isxander.yacl.gui.controllers.slider.IntegerSliderController;
import dev.isxander.yacl.gui.controllers.string.number.DoubleFieldController;
import net.minecraft.client.gui.screen.Screen;

public class YACLIntegration {
    public static Screen createScreen(Screen parent) {
        WakesConfig config = WakesClient.CONFIG_INSTANCE;
        Option<Integer> wakeOpacityOption = optionOf(Integer.class, "wake_opacity")
                .binding(100, () -> (int) (config.wakeOpacity * 100), val -> config.wakeOpacity = val / 100f)
                .controller(opt -> integerSlider(opt, 0, 100))
                .available(config.blendMode.canVaryOpacity)
                .build();
        return YetAnotherConfigLib.createBuilder()
                .title(WakesUtils.translatable("config", "title"))
                .category(configCategory("wake_appearance")
                        .option(optionOf(WakesConfig.Resolution.class, "wake_resolution")
                                .binding(WakesConfig.Resolution.SIXTEEN, () -> config.wakeResolution, WakeHandler::scheduleResolutionChange)
                                .controller(opt -> new EnumController(opt, val -> WakesUtils.translatable("resolution", val.toString().toLowerCase())))
                                .build())
                        .option(wakeOpacityOption)
                        .build())
                .category(configCategory("wake_behaviour")
                        .group(group("wake_spawning")
                                .option(wakeSpawningRulesOption("boat_wake_rules"))
                                .option(wakeSpawningRulesOption("player_wake_rules"))
                                .option(wakeSpawningRulesOption("other_players_wake_rules"))
                                .option(wakeSpawningRulesOption("mobs_wake_rules"))
                                .option(wakeSpawningRulesOption("items_wake_rules"))
                                .option(booleanOption("wakes_in_running_water")
                                        .binding(false, () -> config.wakesInRunningWater, val -> config.wakesInRunningWater = val)
                                        .build())
                                .build())
                        .option(optionOf(Float.class, "wave_speed")
                                .binding(0.95f, () -> config.waveSpeed, val -> {
                                    config.waveSpeed = val;
                                    WakeNode.calculateAlpha();
                                })
                                .controller(opt -> floatSlider(opt, 0f, 2f, 0.01f))
                                .build())
                        .option(optionOf(Integer.class, "initial_wave_strength")
                                .binding(20, () -> config.initialStrength, val -> config.initialStrength = val)
                                .controller(opt -> integerSlider(opt, 0, 150))
                                .build())
                        .option(optionOf(Integer.class, "paddle_strength")
                                .binding(100, () -> config.paddleStrength, val -> config.paddleStrength = val)
                                .controller(opt -> integerSlider(opt, 0, 150))
                                .build())
                        .option(optionOf(Integer.class, "splash_strength")
                                .binding(100, () -> config.splashStrength, val -> config.splashStrength = val)
                                .controller(opt -> integerSlider(opt, 0, 150))
                                .build())
                        .option(optionOf(Double.class, "minimum_producer_velocity")
                                .binding(0.1, () -> config.minimumProducerVelocity, val -> config.minimumProducerVelocity = val)
                                .controller(DoubleFieldController::new)
                                .build())
                        .option(optionOf(Float.class, "wave_decay")
                                .binding(0.9f, () -> config.waveDecay, val -> config.waveDecay = val)
                                .controller(opt -> floatSlider(opt, 0f, 1f, 0.01f))
                                .build())
                        .build())
                .category(configCategory("debug")
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
                        .option(booleanOption("render_wakes")
                                .binding(true, () -> config.renderWakes, val -> config.renderWakes = val)
                                .build())
                        .option(booleanOption("spawn_wakes")
                                .binding(true, () -> config.spawnWakes, val -> config.spawnWakes = val)
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

    private static ConfigCategory.Builder configCategory(String name) {
        return ConfigCategory.createBuilder()
                .name(WakesUtils.translatable("config_category", name));
    }

    private static OptionGroup.Builder group(String name) {
        return OptionGroup.createBuilder()
                .name(WakesUtils.translatable("option_group", name));
    }

    private static <T> Option.Builder<T> optionOf(Class<T> optionType, String name) {
        return Option.createBuilder(optionType)
                .name(WakesUtils.translatable("option", name));
    }

    private static IntegerSliderController integerSlider(Option<Integer> option, int min, int max) {
        return new IntegerSliderController(option, min, max, 1);
    }

    private static FloatSliderController floatSlider(Option<Float> option, float min, float max, float step) {
        return new FloatSliderController(option, min, max, step);
    }

    private static Option.Builder<Boolean> booleanOption(String name) {
        return Option.createBuilder(Boolean.class)
                .name(WakesUtils.translatable("option", name))
                .controller(TickBoxController::new);
    }

    private static Option<WakesConfig.WakeSpawningRule> wakeSpawningRulesOption(String name) {
        WakesConfig config = WakesClient.CONFIG_INSTANCE;
        return Option.createBuilder(WakesConfig.WakeSpawningRule.class)
                .name(WakesUtils.translatable("option", name))
                .binding(WakesConfig.WakeSpawningRule.WAKES_AND_SPLASHES, () -> config.wakeSpawningRules.get(name), val -> config.wakeSpawningRules.put(name, val))
                .controller(opt -> new EnumController(opt, val -> WakesUtils.translatable("wake_spawn_rule", val.toString().toLowerCase())))
                .build();
    }

    private static OptionGroup intervalGroup(int n, WakeColor defaultColor, int defaultLower, int defaultUpper) {
        WakesConfig config = WakesClient.CONFIG_INSTANCE;
        return OptionGroup.createBuilder()
                .name(WakesUtils.translatable("option_group", "interval" + (n+1)))
                .option(optionOf(Integer.class, "lower")
                        .binding(defaultLower, () -> config.colorIntervals.get(n).lower, config.colorIntervals.get(n)::setLower)
                        .controller(opt -> integerSlider(opt, -50, 50))
                        .build())
                .option(optionOf(Integer.class, "upper")
                        .binding(defaultUpper, () -> config.colorIntervals.get(n).upper, config.colorIntervals.get(n)::setUpper)
                        .controller(opt -> integerSlider(opt, -50, 50))
                        .build())
                .option(optionOf(WakeColor.class, "color")
                        .binding(defaultColor, () -> config.colorIntervals.get(n).color, config.colorIntervals.get(n)::setColor)
                        .controller(opt -> new EnumController(opt, val -> WakesUtils.translatable("config", "color." + val.toString().toLowerCase())))
                        .build())
                .build();
    }
}
