package com.goby56.wakes.tests;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class OpacityTest {

    @Test
    void opacity() {
        assertEquals(0.75f, getOpacity(0,15));
        assertEquals(0f, getOpacity(0.5f,15));
    }

    private float getOpacity(float falloff, int nodeAge) {
        float t = nodeAge / 30f;
        float f = t / (1 - falloff);
        return Math.max(0, 1f - f*f);
    }
}
