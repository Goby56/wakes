package com.goby56.wakes.tests;

import net.minecraft.util.Mth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ResolutionTest {

    @Test
    void logFunctions() {
        assertEquals(Mth.log2(4), 2);
        assertEquals(Mth.log2(8), 3);
        assertEquals(Mth.log2(16), 4);
        assertEquals(Mth.log2(32), 5);
        assertEquals(Mth.log2(64), 6);
        assertEquals(Mth.log2(128), 7);

        assertEquals(Mth.ceillog2(4), 2);
        assertEquals(Mth.ceillog2(8), 3);
        assertEquals(Mth.ceillog2(16), 4);
        assertEquals(Mth.ceillog2(32), 5);
        assertEquals(Mth.ceillog2(64), 6);
        assertEquals(Mth.ceillog2(128), 7);
    }
}
