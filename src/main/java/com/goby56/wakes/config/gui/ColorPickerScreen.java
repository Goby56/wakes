package com.goby56.wakes.config.gui;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.utils.WakesUtils;
import com.google.common.collect.Lists;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ColorPickerScreen extends Screen {
    private final Screen parent;
    private boolean showInfoText = false;
    private ColorIntervalSlider colorIntervalSlider;
    private WakeAffectingSlider wakeOpacitySlider;
    private WakeAffectingSlider blendStrengthSlider;
    private static final ResourceLocation INFO_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/sprites/icon/info.png");
    private static final ResourceLocation RESET_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath(WakesClient.MOD_ID, "textures/reset_icon.png");
    public ColorPickerScreen(Screen parent) {
        super(Component.nullToEmpty("Configure wake colors"));
        this.parent = parent;
        this.colorIntervalSlider = null;
    }

    @Override
    protected void init() {
        this.colorIntervalSlider = new ColorIntervalSlider(
                this,
                (int) (width / 2 - width * 0.8f / 2), 24,
                (int) (width * 0.8f), 40);
        this.addRenderableWidget(this.colorIntervalSlider);
        this.wakeOpacitySlider = new WakeAffectingSlider(width / 2 - 160, 69, 150, 20, "wakeOpacity",
                () -> (double) WakesConfig.wakeOpacity, (val) -> WakesConfig.wakeOpacity = val.floatValue());
        this.addRenderableWidget(this.wakeOpacitySlider);
        this.blendStrengthSlider = new WakeAffectingSlider(width / 2 + 10, 69, 150, 20, "blendStrength",
                () -> (double) WakesConfig.blendStrength, (val) -> WakesConfig.blendStrength = val.floatValue());
        this.addRenderableWidget(this.blendStrengthSlider);

        TexturedButton infoButton = TexturedButton.builder(this::onInfoClick)
                        .texture(INFO_ICON_TEXTURE, 20, 20)
                        .dimension(30, 30).build();
        infoButton.setPosition((int) (width / 2f - width * 0.8f / 2 - 35), 29);
        infoButton.setTooltip(Tooltip.create(WakesUtils.translatable("gui", "colorIntervalSlider", "infoButton", "tooltip")));
        this.addRenderableWidget(infoButton);

        TexturedButton resetButton = TexturedButton.builder(this::resetConfigurations)
                .texture(RESET_ICON_TEXTURE, 20, 20)
                .dimension(30, 30).build();
        resetButton.setPosition((int) (width / 2f + width * 0.8f / 2 + 5), 29);
        resetButton.setTooltip(Tooltip.create(WakesUtils.translatable("gui", "colorIntervalSlider", "resetButton", "tooltip")));
        this.addRenderableWidget(resetButton);
    }

    private void onInfoClick(Button button) {
        this.showInfoText = !this.showInfoText;
    }

    private void resetConfigurations(Button button) {
        WakesConfig.wakeColorIntervals = Lists.newArrayList(WakesConfig.defaultWakeColorIntervals);
        WakesConfig.wakeColors = Lists.newArrayList(WakesConfig.defaultWakeColors);
        this.colorIntervalSlider.initHandles();
        this.colorIntervalSlider.updateColorPicker();
        this.wakeOpacitySlider.resetValue();
        this.blendStrengthSlider.resetValue();

        WakeHandler.getInstance().ifPresent(WakeHandler::recolorWakes);
        MidnightConfig.write(WakesClient.MOD_ID);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(font, WakesUtils.translatable("gui", "colorIntervalSlider", "title"), width / 2, 10, 0xffffffff);
        if (this.showInfoText) {
            context.drawWordWrap(font, WakesUtils.translatable("gui", "colorIntervalSlider", "infoText"), width - 325, height - 45, 320, 0xffffffff, true);
        }
    }

    @Override
    protected void renderBlurredBackground(GuiGraphics context) {

    }

    public void addWidget(AbstractWidget widget) {
        this.addRenderableWidget(widget);
    }
}
