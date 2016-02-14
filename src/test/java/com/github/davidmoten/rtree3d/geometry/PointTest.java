package com.github.davidmoten.rtree3d.geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PointTest {

    private static final double PRECISION = 0.000001;

    @Test
    public void testCoordinates() {
        Point point = point(1, 2);
        assertEquals(1, point.x(), PRECISION);
        assertEquals(2, point.y(), PRECISION);
    }

    @Test
    public void testDistanceToRectangle() {
        Point p1 = point(1, 2);
        Box r = Geometries.box(4, 6, 4, 6);
        assertEquals(5, p1.distance(r), PRECISION);
    }

    @Test
    public void testDistanceToPoint() {
        Point p1 = point(1, 2);
        Point p2 = point(4, 6);
        assertEquals(5, p1.distance(p2), PRECISION);
    }

    @Test
    public void testMbr() {
        Point p = point(1, 2);
        Box r = Geometries.box(1, 2, 0, 1, 2, 0);
        assertEquals(r, p.mbb());
    }

    @Test
    public void testPointIntersectsItself() {
        Point p = point(1, 2);
        assertTrue(p.distance(p.mbb()) == 0);
    }

    @Test
    public void testIntersectIsFalseWhenPointsDiffer() {
        Point p1 = point(1, 2);
        Point p2 = point(1, 2.000001);
        assertFalse(p1.distance(p2.mbb()) == 0);
    }

    @Test
    public void testEquality() {
        Point p1 = point(1, 2);
        Point p2 = point(1, 2);
        assertTrue(p1.equals(p2));
    }

    @Test
    public void testInequality() {
        Point p1 = point(1, 2);
        Point p2 = point(1, 3);
        assertFalse(p1.equals(p2));
    }

    @Test
    public void testInequalityToNull() {
        Point p1 = point(1, 2);
        assertFalse(p1.equals(null));
    }

    @Test
    public void testHashCode() {
        Point p = point(1, 2);
        assertEquals(-1056041056, p.hashCode());
    }
    
    private static Point point(double x, double y) {
        return Point.create(x,  y, 0);
    }
}
