package com.goby56.wakes.config.gui;

import com.goby56.wakes.render.WakeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

public class SliderHandle implements Comparable<SliderHandle> {

    protected WakeColor color;
    protected float value;
    protected boolean focused;
    protected boolean focusable;

    protected SliderHandle(float value) {
        this.value = value;
        this.focused = false;
        this.focusable = value != 0f && value != 1f;
    }

    protected boolean setValue(float value) {
        double d = this.value;
        this.value = (float) MathHelper.clamp(value, 0.0, 1.0);
        return d != this.value;
    }

    public boolean inProximity(float value, int sliderWidth, int handleWidth) {
        return (Math.abs(value - this.value) < (float) handleWidth / (2 * sliderWidth));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SliderHandle handle) {
            return handle.value == this.value;
        }
        return false;
    }

    @Override
    public int compareTo(@NotNull SliderHandle o) {
        return Float.compare(this.value, o.value);
    }
}
