package com.github.davidmoten.rtree3d.geometry;

import static com.github.davidmoten.rtree3d.geometry.Geometries.box;
import static com.github.davidmoten.rtree3d.geometry.Geometries.circle;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.github.davidmoten.junit.Asserts;
import com.github.davidmoten.rtree3d.geometry.Intersects;

public class IntersectsTest {

    @Test
    public void testConstructorIsPrivate() {
        Asserts.assertIsUtilityClass(Intersects.class);
    }

    @Test
    public void testRectangleIntersectsCircle() {
        assertTrue(
                Intersects.rectangleIntersectsCircle.call(box(0, 0, 0, 0), circle(0, 0, 1)));
    }

    @Test
    public void testRectangleDoesNotIntersectCircle() {
        assertFalse(Intersects.rectangleIntersectsCircle.call(box(0, 0, 0, 0),
                circle(100, 100, 1)));
    }

}
