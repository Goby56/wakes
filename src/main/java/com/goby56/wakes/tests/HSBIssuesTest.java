package com.goby56.wakes.tests;

import com.goby56.wakes.simulation.WakeNode;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class HSBIssuesTest {


    @Test
    void hsbToRgb() {
        assertEquals(0xFFFF0000, Color.HSBtoRGB(1f, 1f, 1f));
        assertEquals(0xFFFF8080, Color.HSBtoRGB(1f, 0.5f, 1f));
    }
}
