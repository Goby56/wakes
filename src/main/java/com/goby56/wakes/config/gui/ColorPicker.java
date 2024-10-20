package com.goby56.wakes.config.gui;

import com.goby56.wakes.config.WakesConfigScreen;
import com.goby56.wakes.render.enums.WakeColor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.awt.*;
import java.util.function.Function;

public class ColorPicker extends ClickableWidget {
    private static final Identifier BACKGROUND_TEXTURE = Identifier.ofVanilla("widget/button");
    private static final Identifier FRAME_TEXTURE = Identifier.ofVanilla("widget/slot_frame");

    private final WakesConfigScreen screenContext;
    private final TextFieldWidget hexInput;
    private final GradientSlider hueSlider;
    private final GradientSlider alphaSlider;
    private final Vector2i colorPickerPos;
    private final Vector2i colorPickerDim;
    private PickListener listener;

    public interface PickListener {
        void onPickedColor(WakeColor color);
    }


    public ColorPicker(WakesConfigScreen screenContext, int x, int y, int width, int height) {
        super(x, y, width, height, Text.of(""));
        this.screenContext = screenContext;

        var topLeft = globalSpace(new Vector2f(0f, 0f));
        var bottomRight = globalSpace(new Vector2f(1f, 2f / 3f));
        this.colorPickerPos = topLeft;
        this.colorPickerDim = bottomRight.sub(bottomRight);

        this.hexInput = new HexInputField(new Vector2f(0f, 5f / 6f), new Vector2f(1f / 3f, 1f), this::globalSpace, screenContext.textRenderer);
        this.hueSlider = new GradientSlider(new Vector2f(0f, 2f / 3f), new Vector2f(1f, 5f / 6f), this::globalSpace);
        this.alphaSlider = new GradientSlider(new Vector2f(1f / 3f, 5f / 6f), new Vector2f(1f, 1f), this::globalSpace);

        screenContext.addWidget(this);
        screenContext.addWidget(hueSlider);
        screenContext.addWidget(alphaSlider);
    }

    public void toggleActive() {
        boolean active = !this.active;
        this.active = active;
        this.hexInput.active = active;
        this.hueSlider.active = active;
        this.alphaSlider.active = active;
    }

    public void registerListener(PickListener listener) {
        this.listener = listener;
    }

    private Vector2f relativeSpace(Vector2i globalSpace) {
        return new Vector2f((float) (globalSpace.x - getX()) / width, (float) (globalSpace.y - getY()) / width);
    }

    private Vector2i globalSpace(Vector2f relativeSpace) {
        return new Vector2i((int) (relativeSpace.x * width + getX()), (int) (relativeSpace.y * height + getY()));
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        System.out.printf("%f, %f\n", (mouseX - getX()) / width, (mouseY - getY()) / height);
        super.onClick(mouseX, mouseY);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        super.onDrag(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!active) return;
        int x = getX();
        int y = getY();

        context.drawGuiTexture(BACKGROUND_TEXTURE, x, y, width, height);

        drawColorPicker(context, colorPickerPos.x, colorPickerPos.y, colorPickerDim.x, colorPickerDim.y);
    }

    private void drawColorPicker(DrawContext context, int x, int y, int w, int h) {
        // Color picker
        context.drawGuiTexture(FRAME_TEXTURE, x - 3, y - 3, w + 6, h + 6);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        buffer.vertex(matrix, x, y, 5).color(Color.HSBtoRGB(1, 0, 1));
        buffer.vertex(matrix, x, y + h, 5).color(Color.HSBtoRGB(1, 0, 0));
        buffer.vertex(matrix, x + w, y + h, 5).color(Color.HSBtoRGB(1, 1, 0));
        buffer.vertex(matrix, x + w, y, 5).color(Color.HSBtoRGB(1, 1, 1));
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }

    private class HexInputField extends TextFieldWidget {

        public HexInputField(Vector2f topLeft, Vector2f bottomRight, Function<Vector2f, Vector2i> globalSpaceConverter, TextRenderer textRenderer) {
            super(textRenderer, 0, 0, 0, 0, Text.of(""));
            Vector2i globalPos = globalSpaceConverter.apply(topLeft);
            Vector2i dimensions = globalSpaceConverter.apply(bottomRight).sub(globalPos);
            this.setX(globalPos.x);
            this.setY(globalPos.y);
            this.setWidth(dimensions.x);
            this.setHeight(dimensions.y);
        }
    }

    private class GradientSlider extends SliderWidget {


        public GradientSlider(Vector2f topLeft, Vector2f bottomRight, Function<Vector2f, Vector2i> globalSpaceConverter) {
            super(0, 0, 0, 0, Text.of(""), 1f);
            Vector2i globalPos = globalSpaceConverter.apply(topLeft);
            Vector2i dimensions = globalSpaceConverter.apply(bottomRight).sub(globalPos);
            this.setX(globalPos.x);
            this.setY(globalPos.y);
            this.setWidth(dimensions.x);
            this.setHeight(dimensions.y);
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            super.renderWidget(context, mouseX, mouseY, delta);
        }

        @Override
        protected void updateMessage() {

        }

        @Override
        protected void applyValue() {

        }
    }
}
