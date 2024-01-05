package com.goby56.wakes.config.enums;

import net.minecraft.util.math.MathHelper;

public enum Resolution {
    EIGHT(8),
    SIXTEEN(16),
    THIRTYTWO(32);

    public final int res;
    public final int power;

    Resolution(int res) {
        this.res = res;
        this.power = MathHelper.floorLog2(res);
    }

    @Override
    public String toString() {
        return String.valueOf(this.res);
    }
}
