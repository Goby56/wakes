package com.goby56.wakes.tests;

import net.minecraft.util.math.MathHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ResolutionTest {

    @Test
    void logFunctions() {
        assertEquals(MathHelper.floorLog2(4), 2);
        assertEquals(MathHelper.floorLog2(8), 3);
        assertEquals(MathHelper.floorLog2(16), 4);
        assertEquals(MathHelper.floorLog2(32), 5);
        assertEquals(MathHelper.floorLog2(64), 6);
        assertEquals(MathHelper.floorLog2(128), 7);

        assertEquals(MathHelper.ceilLog2(4), 2);
        assertEquals(MathHelper.ceilLog2(8), 3);
        assertEquals(MathHelper.ceilLog2(16), 4);
        assertEquals(MathHelper.ceilLog2(32), 5);
        assertEquals(MathHelper.ceilLog2(64), 6);
        assertEquals(MathHelper.ceilLog2(128), 7);
    }
}
