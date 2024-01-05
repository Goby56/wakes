package com.goby56.wakes.tests;

import com.goby56.wakes.simulation.QuadTree;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class QuadTreeTest {

    @Test
    void pointInsideAABB() {
        QuadTree.AABB aabb = new QuadTree.AABB(0, 0, 1);
        assertTrue(aabb.contains(0,0));
        assertTrue(aabb.contains(0,-1));
        assertTrue(aabb.contains(1,-1));
        assertTrue(aabb.contains(1,0));
        assertTrue(aabb.contains(1,1));
        assertTrue(aabb.contains(0,1));
        assertTrue(aabb.contains(-1,1));
        assertTrue(aabb.contains(-1,0));
        assertTrue(aabb.contains(-1,-1));

        assertFalse(aabb.contains(0,-2));
        assertFalse(aabb.contains(2,-2));
        assertFalse(aabb.contains(2,0));
        assertFalse(aabb.contains(2,2));
        assertFalse(aabb.contains(0,2));
        assertFalse(aabb.contains(-2,2));
        assertFalse(aabb.contains(-2,0));
        assertFalse(aabb.contains(-2,-2));
    }

    @Test
    void AABBintersectsAABB() {
        QuadTree.AABB aabb1 = new QuadTree.AABB(-1, 0, 1);
        QuadTree.AABB aabb2 = new QuadTree.AABB(1, 0, 1);
        assertTrue(aabb1.intersects(aabb2));
        assertTrue(aabb2.intersects(aabb1));

        QuadTree.AABB aabb3 = new QuadTree.AABB(0, -1, 1);
        QuadTree.AABB aabb4 = new QuadTree.AABB(0, 1, 1);
        assertTrue(aabb3.intersects(aabb4));
        assertTrue(aabb4.intersects(aabb3));

        QuadTree.AABB aabb5 = new QuadTree.AABB(-2, 0, 1);
        QuadTree.AABB aabb6 = new QuadTree.AABB(2, 0, 1);
        assertFalse(aabb5.intersects(aabb6));
        assertFalse(aabb6.intersects(aabb5));

        QuadTree.AABB aabb7 = new QuadTree.AABB(0, -2, 1);
        QuadTree.AABB aabb8 = new QuadTree.AABB(0, 2, 1);
        assertFalse(aabb7.intersects(aabb8));
        assertFalse(aabb8.intersects(aabb7));
    }

    @Test
    void noduplicateInsertions() {
        QuadTree<GenericNode> tree = new QuadTree<>(0, 0, 10000);
        tree.insert(new GenericNode(1, 1, 0.89f, 0));
        tree.insert(new GenericNode(1, 1, 62.888f, 15));
        tree.insert(new GenericNode(1, 1, -23.5f, 8));
        ArrayList<GenericNode> nodes = new ArrayList<>();
        tree.query(new QuadTree.AABB(0, 0, 1), nodes);
        assertEquals(1, nodes.size());
    }
}