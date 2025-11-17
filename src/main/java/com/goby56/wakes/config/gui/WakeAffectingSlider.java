package com.goby56.wakes.config.gui;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.utils.WakesUtils;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class WakeAffectingSlider extends AbstractSliderButton {
    private final Component name;
    private final String configEntryName;
    private final Supplier<Double> configOptionGetter;
    private final Consumer<Double> configOptionSetter;

    public WakeAffectingSlider(int x, int y, int width, int height, String configEntryName, Supplier<Double> configOptionGetter, Consumer<Double> configOptionSetter) {
        super(x, y, width, height, WakesUtils.translatable("midnightconfig", configEntryName), configOptionGetter.get());
        this.configOptionSetter = configOptionSetter;
        this.configOptionGetter = configOptionGetter;
        this.name = WakesUtils.translatable("midnightconfig", configEntryName);
        this.configEntryName = configEntryName;
        this.updateMessage();
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.nullToEmpty(String.format("%s: %.2f", this.name.getString(), this.value)));
    }

    @Override
    protected void applyValue() {
        configOptionSetter.accept(this.value);
        MidnightConfig.write(WakesClient.MOD_ID);
        WakeHandler.getInstance().ifPresent(WakeHandler::recolorWakes);
    }

    public void resetValue() {
        Float val = (Float) MidnightConfig.getDefaultValue(WakesClient.MOD_ID, configEntryName);
        if (val != null) {
            this.value = val;
            configOptionSetter.accept(this.value);
        }
        this.updateMessage();
    }
}
