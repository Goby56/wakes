package com.goby56.wakes.tests;

import com.goby56.wakes.simulation.WakeNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NodeTrailTest {

    @Test
    void thickTrail() {
        for (var node : WakeNode.Factory.thickNodeTrail(2, 2, 4, 4, 64, 100, 0.3, 1.5f)) {
            assertNotEquals(0, node.x);
            assertNotEquals(0, node.z);
        }
    }
}
