package com.goby56.wakes.config;

import com.goby56.wakes.utils.WakeColor;
import com.goby56.wakes.utils.WakeNode;
import com.goby56.wakes.utils.WakesUtils;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screen.Screen;

public class YACLIntegration {
    public static Screen createScreen(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(WakesUtils.translatable("config", "title"))
                .category(configCategory("wake_colors")
                        .option(booleanOption("use_water_blending")
                                .binding(true, () -> WakesConfig.useWaterBlending, val -> WakesConfig.useWaterBlending = val)
                                .controller(TickBoxControllerBuilder::create)
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
                .category(configCategory("wake_behaviour")
                        .option(optionOf(Float.class, "wave_speed")
                                .binding(0.95f, () -> WakesConfig.waveSpeed, val -> {
                                    WakesConfig.waveSpeed = val;
                                    WakeNode.calculateAlpha();
                                })
                                .controller(opt -> floatSlider(opt, 0f, 2f, 0.01f))
                                .build())
                        .option(optionOf(Integer.class, "initial_wave_strength")
                                .binding(20, () -> WakesConfig.initialStrength, val -> WakesConfig.initialStrength = val)
                                .controller(opt -> integerSlider(opt, 0, 150))
                                .build())
                        .option(optionOf(Integer.class, "paddle_strength")
                                .binding(100, () -> WakesConfig.paddleStrength, val -> WakesConfig.paddleStrength = val)
                                .controller(opt -> integerSlider(opt, 0, 150))
                                .build())
                        .option(optionOf(Float.class, "wave_decay")
                                .binding(0.9f, () -> WakesConfig.waveDecay, val -> WakesConfig.waveDecay = val)
                                .controller(opt -> floatSlider(opt, 0f, 1f, 0.01f))
                                .build())
                        .option(booleanOption("use_age_decay")
                                .binding(false, () -> WakesConfig.useAgeDecay, val -> WakesConfig.useAgeDecay = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .build())
                .category(configCategory("debug")
                        .option(optionOf(Integer.class, "flood_fill_distance")
                                .binding(3, () -> WakesConfig.floodFillDistance, val -> WakesConfig.floodFillDistance = val)
                                .controller(opt -> integerSlider(opt, 1, 5))
                                .build())
                        .option(optionOf(Integer.class, "ticks_before_fill")
                                .binding(2, () -> WakesConfig.ticksBeforeFill, val -> WakesConfig.ticksBeforeFill = val)
                                .controller(opt -> integerSlider(opt, 1, 5))
                                .build())
                        .option(booleanOption("use_9_point_stencil")
                                .binding(true, () -> WakesConfig.use9PointStencil, val -> WakesConfig.use9PointStencil = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(booleanOption("draw_debug_boxes")
                                .binding(false, () -> WakesConfig.drawDebugBoxes, val -> WakesConfig.drawDebugBoxes = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .build())
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
        return Option.<T>createBuilder()
                .name(WakesUtils.translatable("option", name));
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

    private static Option.Builder<Boolean> booleanOption(String name) {
        return Option.<Boolean>createBuilder()
                .name(WakesUtils.translatable("option", name));
    }

    private static OptionGroup intervalGroup(int n, WakeColor defaultColor, int defaultLower, int defaultUpper) {
        return OptionGroup.createBuilder()
                .name(WakesUtils.translatable("option_group", "interval" + (n+1)))
                .option(optionOf(Integer.class, "lower")
                        .binding(defaultLower, () -> WakesConfig.colorIntervals[n].lower, WakesConfig.colorIntervals[n]::setLower)
                        .controller(opt -> integerSlider(opt, -50, 50))
                        .build())
                .option(optionOf(Integer.class, "upper")
                        .binding(defaultUpper, () -> WakesConfig.colorIntervals[n].upper, WakesConfig.colorIntervals[n]::setUpper)
                        .controller(opt -> integerSlider(opt, -50, 50))
                        .build())
                .option(optionOf(WakeColor.class, "color")
                        .binding(defaultColor, () -> WakesConfig.colorIntervals[n].color, WakesConfig.colorIntervals[n]::setColor)
                        .controller(opt -> EnumControllerBuilder.create(opt)
                                .enumClass(WakeColor.class)
                                .valueFormatter(val -> WakesUtils.translatable("config", "color." + val.name().toLowerCase())))
                        .build())
                .build();
    }
}
