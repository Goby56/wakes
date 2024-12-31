package com.goby56.wakes.config.gui;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.utils.WakesUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameModeSelectionScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipBackgroundRenderer;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ColorPickerScreen extends Screen {
    private final Screen parent;
    private boolean showInfoText = false;
    public ColorPickerScreen(Screen parent) {
        super(Text.of("Configure wake colors"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addDrawableChild(new ColorIntervalSlider(
                this,
                (int) (width / 2 - width * 0.8f / 2), 24,
                (int) (width * 0.8f), 40));
        TextIconButtonWidget infoButton = TextIconButtonWidget.builder(Text.empty(), this::onInfoClick, true)
                        .texture(Identifier.ofVanilla("icon/info"), 20, 20)
                        .dimension(30, 30).build();
        infoButton.setPosition((int) (width / 2 - width * 0.8f / 2 - 35), 29);
        this.addDrawableChild(infoButton);
    }

    private void onInfoClick(ButtonWidget button) {
        this.showInfoText = !this.showInfoText;
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
            // TODO DYNAMIC TOOLTIP BACKGROUND SIZE
            TooltipBackgroundRenderer.render(context, width - 350, height - 60, 325, 34, 0);
            context.drawTextWrapped(textRenderer, WakesUtils.translatable("gui", "colorIntervalSlider", "info"), width - 350, height - 60, 325, 0xa8a8a8);
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
