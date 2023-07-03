package com.goby56.wakes.config;

import com.goby56.wakes.utils.WakeColor;
import com.goby56.wakes.utils.WakeNode;
import com.goby56.wakes.utils.WakesUtils;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screen.Screen;

public class WakesConfig {

    public static float waveSpeed = 0.95f;
    public static int initialStrength = 20;
    public static int paddleStrength = 100;
    public static float waveDecay = 0.9f;
    public static boolean useAgeDecay = false;

    public static int floodFillDistance = 3;
    public static boolean use9PointStencil = true;
    public static int ticksBeforeFill = 2;
    public static boolean drawDebugBoxes = false;

    public static ColorInterval[] colorIntervals = new ColorInterval[] {
            new ColorInterval(WakeColor.TRANSPARENT, -50, -45),
            new ColorInterval(WakeColor.DARK_GRAY, -45, -35),
            new ColorInterval(WakeColor.GRAY, -35, -30),
            new ColorInterval(WakeColor.LIGHT_GRAY, -30, -15),
            new ColorInterval(WakeColor.TRANSPARENT, -15, 2),
            new ColorInterval(WakeColor.LIGHT_GRAY, 2, 10),
            new ColorInterval(WakeColor.WHITE, 10, 20),
            new ColorInterval(WakeColor.LIGHT_GRAY, 20, 40),
            new ColorInterval(WakeColor.GRAY, 40, 50),
    };

    public static boolean useWaterBlending = true;

    public static class ColorInterval {
        public WakeColor color;
        public int lower;
        public int upper;

        public ColorInterval(WakeColor color, int lower, int upper) {
            this.color = color;
            this.lower = lower;
            this.upper = upper;
        }

        public void setColor(WakeColor color) {
            this.color = color;
        }

        public void setLower(int lower) {
            this.lower = lower;
        }

        public void setUpper(int upper) {
            this.upper = upper;
        }

    }


    public static Screen createScreen(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(WakesUtils.translatable("config", "title"))
                .category(configCategory("wake_colors")
                        .option(booleanOption("use_water_blending")
                                .binding(true, () -> useWaterBlending, val -> useWaterBlending = val)
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
                                .binding(0.95f, () -> waveSpeed, val -> {
                                    waveSpeed = val;
                                    WakeNode.calculateAlpha();
                                })
                                .controller(opt -> floatSlider(opt, 0f, 2f, 0.01f))
                                .build())
                        .option(optionOf(Integer.class, "initial_wave_strength")
                                .binding(20, () -> initialStrength, val -> initialStrength = val)
                                .controller(opt -> integerSlider(opt, 0, 150))
                                .build())
                        .option(optionOf(Integer.class, "paddle_strength")
                                .binding(100, () -> paddleStrength, val -> paddleStrength = val)
                                .controller(opt -> integerSlider(opt, 0, 150))
                                .build())
                        .option(optionOf(Float.class, "wave_decay")
                                .binding(0.9f, () -> waveDecay, val -> waveDecay = val)
                                .controller(opt -> floatSlider(opt, 0f, 1f, 0.01f))
                                .build())
                        .option(booleanOption("use_age_decay")
                                .binding(false, () -> useAgeDecay, val -> useAgeDecay = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .build())
                .category(configCategory("debug")
                        .option(optionOf(Integer.class, "flood_fill_distance")
                                .binding(3, () -> floodFillDistance, val -> floodFillDistance = val)
                                .controller(opt -> integerSlider(opt, 1, 5))
                                .build())
                        .option(optionOf(Integer.class, "ticks_before_fill")
                                .binding(2, () -> ticksBeforeFill, val -> ticksBeforeFill = val)
                                .controller(opt -> integerSlider(opt, 1, 5))
                                .build())
                        .option(booleanOption("use_9_point_stencil")
                                .binding(true, () -> use9PointStencil, val -> use9PointStencil = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(booleanOption("draw_debug_boxes")
                                .binding(false, () -> drawDebugBoxes, val -> drawDebugBoxes = val)
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
                        .binding(defaultLower, () -> colorIntervals[n].lower, colorIntervals[n]::setLower)
                        .controller(opt -> integerSlider(opt, -50, 50))
                        .build())
                .option(optionOf(Integer.class, "upper")
                        .binding(defaultUpper, () -> colorIntervals[n].upper, colorIntervals[n]::setUpper)
                        .controller(opt -> integerSlider(opt, -50, 50))
                        .build())
                .option(optionOf(WakeColor.class, "color")
                        .binding(defaultColor, () -> colorIntervals[n].color, colorIntervals[n]::setColor)
                        .controller(opt -> EnumControllerBuilder.create(opt)
                                .enumClass(WakeColor.class))
                        .build())
                .build();
    }
}
