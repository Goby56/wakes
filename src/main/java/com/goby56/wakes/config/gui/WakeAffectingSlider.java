package com.goby56.wakes.config.gui;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.utils.WakesUtils;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class WakeAffectingSlider extends SliderWidget {
    private final Text name;
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
        this.setMessage(Text.of(String.format("%s: %.2f", this.name.getString(), this.value)));
    }

    @Override
    protected void applyValue() {
        configOptionSetter.accept(this.value);
        MidnightConfig.write(WakesClient.MOD_ID);
        WakeHandler.getInstance().ifPresent(WakeHandler::recolorWakes);
    }

    public void resetValue() {
        try {
            var config = MidnightConfig.configClass.get(WakesClient.MOD_ID);
            this.value = config.getField(configEntryName).getFloat(config);
            configOptionSetter.accept(this.value);

        } catch (NoSuchFieldException e) {
            System.out.println("Could not find field");
            return;
        } catch (IllegalAccessException e) {
            System.out.println("Could not retrieve float");
            return;
        }
        this.updateMessage();
    }
}
