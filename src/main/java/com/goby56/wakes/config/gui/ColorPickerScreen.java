package com.goby56.wakes.config.gui;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.utils.WakesUtils;
import com.google.common.collect.Lists;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.tooltip.TooltipBackgroundRenderer;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ColorPickerScreen extends Screen {
    private final Screen parent;
    private boolean showInfoText = false;
    private ColorIntervalSlider colorIntervalSlider;
    private static final Identifier INFO_ICON_TEXTURE = Identifier.of("minecraft", "textures/gui/sprites/icon/info.png");
    private static final Identifier RESET_ICON_TEXTURE = Identifier.of(WakesClient.MOD_ID, "textures/reset_icon.png");
    public ColorPickerScreen(Screen parent) {
        super(Text.of("Configure wake colors"));
        this.parent = parent;
        this.colorIntervalSlider = null;
    }

    @Override
    protected void init() {
        this.colorIntervalSlider = new ColorIntervalSlider(
                this,
                (int) (width / 2 - width * 0.8f / 2), 24,
                (int) (width * 0.8f), 40);
        this.addDrawableChild(this.colorIntervalSlider);

        this.addDrawableChild(new WakeAffectingSlider(width / 2 - 160, 69, 150, 20, WakesUtils.translatable("midnightconfig", "wakeOpacity"),
                () -> (double) WakesConfig.wakeOpacity, (val) -> WakesConfig.wakeOpacity = val.floatValue()));
        this.addDrawableChild(new WakeAffectingSlider(width / 2 + 10, 69, 150, 20, WakesUtils.translatable("midnightconfig", "blendStrength"),
                () -> (double) WakesConfig.blendStrength, (val) -> WakesConfig.blendStrength = val.floatValue()));

        TexturedButton infoButton = TexturedButton.builder(this::onInfoClick)
                        .texture(INFO_ICON_TEXTURE, 20, 20)
                        .dimension(30, 30).build();
        infoButton.setPosition((int) (width / 2 - width * 0.8f / 2 - 35), 29);
        infoButton.setTooltip(Tooltip.of(WakesUtils.translatable("gui", "colorIntervalSlider", "infoButton", "tooltip")));
        this.addDrawableChild(infoButton);

        TexturedButton resetButton = TexturedButton.builder(this::resetConfigurations)
                .texture(RESET_ICON_TEXTURE, 20, 20)
                .dimension(30, 30).build();
        resetButton.setPosition((int) (width / 2 + width * 0.8f / 2 + 5), 29);
        resetButton.setTooltip(Tooltip.of(WakesUtils.translatable("gui", "colorIntervalSlider", "resetButton", "tooltip")));
        this.addDrawableChild(resetButton);
    }

    private void onInfoClick(ButtonWidget button) {
        this.showInfoText = !this.showInfoText;
    }

    private void resetConfigurations(ButtonWidget button) {
        WakesConfig.wakeColorIntervals = Lists.newArrayList(WakesConfig.defaultWakeColorIntervals);
        WakesConfig.wakeColors = Lists.newArrayList(WakesConfig.defaultWakeColors);
        this.colorIntervalSlider.updateColorPicker();
        WakeHandler.getInstance().ifPresent(WakeHandler::recolorWakes);
        MidnightConfig.write(WakesClient.MOD_ID);
        // TODO RESET OPACITY AND BLEND STRENGTH TOO
    }

    @Override
    public void close() {
        client.setScreen(this.parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, WakesUtils.translatable("gui", "colorIntervalSlider", "title"), width / 2, 10, 0xffffff);
        if (this.showInfoText) {
            // TODO DYNAMIC TOOLTIP BACKGROUND SIZE DEPENDING ON INFO TEXT LENGTH
            TooltipBackgroundRenderer.render(context, width - 350, height - 60, 325, 34, 0);
            context.drawTextWrapped(textRenderer, WakesUtils.translatable("gui", "colorIntervalSlider", "infoText"), width - 350, height - 60, 325, 0xa8a8a8);
        }
    }

    @Override
    protected void applyBlur(float delta) {
        // No Song 2
    }

    public void addWidget(ClickableWidget widget) {
        this.addDrawableChild(widget);
    }
}
