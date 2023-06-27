package com.goby56.wakes.tests;

import com.goby56.wakes.utils.WakesUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WakesUtilsTest {

    @Test
    void rgbaArr2abgrInt() {
        // Green
        int[] rgba = {0, 255, 0, 255};
        int val = WakesUtils.rgbaArr2abgrInt(rgba);
        assertEquals(0xFF00FF00, val);
    }

    @Test
    void abgrInt2rgbaArr() {
        // Green
        int val = 0xFF00FF00;
        int[] rgba = WakesUtils.abgrInt2rgbaArr(val);
        assertArrayEquals(new int[]{0, 255, 0, 255}, rgba);
    }



}