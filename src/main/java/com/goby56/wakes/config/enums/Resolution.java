package com.goby56.wakes.config.enums;

import net.minecraft.util.Mth;

public enum Resolution {
    EIGHT(8),
    SIXTEEN(16),
    THIRTYTWO(32);

    public final int res;
    public final int power;

    Resolution(int res) {
        this.res = res;
        this.power = Mth.log2(res);
    }

    public static Resolution getHighest() {
        return THIRTYTWO;
    }

    @Override
    public String toString() {
        return String.valueOf(this.res);
    }
}
