package com.goby56.wakes.config.gui;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.simulation.WakeHandler;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class OpacitySlider extends SliderWidget {
    public OpacitySlider(int x, int y, int width, int height) {
        super(x, y, width, height, Text.of("Wake opacity"), WakesConfig.wakeOpacity);
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Text.of(String.format("%.2f", this.value)));
    }

    @Override
    protected void applyValue() {
        WakesConfig.wakeOpacity = (float) this.value;
        MidnightConfig.write(WakesClient.MOD_ID);
        WakeHandler.getInstance().ifPresent(WakeHandler::recolorWakes);
    }
}
