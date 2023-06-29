package com.goby56.wakes.tests;

import com.goby56.wakes.utils.WakesUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Set;

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

    @Test
    void longAsPos() {
        int[] pos = WakesUtils.longAsPos(55834574857L);
        assertEquals(13, pos[0]);
        assertEquals(9, pos[1]);

        pos = WakesUtils.longAsPos(-8589934541L);
        assertEquals(-2, pos[0]);
        assertEquals(51, pos[1]);
    }

    @Test
    void posAsLong() {
        long l = WakesUtils.posAsLong(13, 9);
        assertEquals(55834574857L, l);

        l = WakesUtils.posAsLong(-2, 51);
        assertEquals(-8589934541L, l);
    }

//    @Test
//    void bresenhamLine() {
//        Set<Long> points = WakesUtils.bresenhamLine(0, 0, 3, 0);
//        assertEquals(WakesUtils.posAsLong(0,0), points.get(0));
//        assertEquals(WakesUtils.posAsLong(1,0), points.get(1));
//        assertEquals(WakesUtils.posAsLong(2,0), points.get(2));
//        assertEquals(WakesUtils.posAsLong(3,0), points.get(3));
//
//        points = WakesUtils.bresenhamLine(0, 0, 2, 2);
//        assertEquals(WakesUtils.posAsLong(0,0), points.get(0));
//        assertEquals(WakesUtils.posAsLong(1,1), points.get(1));
//        assertEquals(WakesUtils.posAsLong(2,2), points.get(2));
//
//        points = WakesUtils.bresenhamLine(0, 0, 22, 13);
//        System.out.println(points);
//        assertEquals(WakesUtils.posAsLong(0,0), points.get(3));
//        assertEquals(WakesUtils.posAsLong(-1,-1), points.get(2));
//        assertEquals(WakesUtils.posAsLong(-1,-2), points.get(1));
//        assertEquals(WakesUtils.posAsLong(-2,-3), points.get(0));
//    }

}