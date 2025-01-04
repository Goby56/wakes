package com.goby56.wakes.config.gui;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.simulation.WakeHandler;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class WakeAffectingSlider extends SliderWidget {
    private final Text name;
    private final Supplier<Double> configOptionGetter;
    private final Consumer<Double> configOptionSetter;

    public WakeAffectingSlider(int x, int y, int width, int height, Text name, Supplier<Double> configOptionGetter, Consumer<Double> configOptionSetter) {
        super(x, y, width, height, name, configOptionGetter.get());
        this.configOptionSetter = configOptionSetter;
        this.configOptionGetter = configOptionGetter;
        this.name = name;
        this.updateMessage();
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Text.of(String.format("%s: %.2f", this.name.getString(), this.value)));
    }

    @Override
    protected void applyValue() {
        configOptionSetter.accept(this.value);
        MidnightConfig.write(WakesClient.MOD_ID);
        WakeHandler.getInstance().ifPresent(WakeHandler::recolorWakes);
    }
}
