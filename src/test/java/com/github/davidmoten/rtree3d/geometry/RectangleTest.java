package com.github.davidmoten.rtree3d.geometry;

import static org.junit.Assert.*;

import org.junit.Test;

import com.github.davidmoten.rtree3d.geometry.Box;

public class RectangleTest {

    private static final double PRECISION = 0.00001;

    @Test
    public void testDistanceToSelfIsZero() {
        Box r = box(0, 0, 1, 1);
        assertEquals(0, r.distance(r), PRECISION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testXParametersWrongOrderThrowsException() {
        box(2, 0, 1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testYParametersWrongOrderThrowsException() {
        box(0, 2, 1, 1);
    }

    @Test
    public void testDistanceToOverlapIsZero() {
        Box r = box(0, 0, 2, 2);
        Box r2 = box(1, 1, 3, 3);

        assertEquals(0, r.distance(r2), PRECISION);
        assertEquals(0, r2.distance(r), PRECISION);
    }

    @Test
    public void testDistanceWhenSeparatedByXOnly() {
        Box r = box(0, 0, 2, 2);
        Box r2 = box(3, 0, 4, 2);

        assertEquals(1, r.distance(r2), PRECISION);
        assertEquals(1, r2.distance(r), PRECISION);
    }

    @Test
    public void testDistanceWhenSeparatedByXOnlyAndOverlapOnY() {
        Box r = box(0, 0, 2, 2);
        Box r2 = box(3, 1.5f, 4, 3.5f);

        assertEquals(1, r.distance(r2), PRECISION);
        assertEquals(1, r2.distance(r), PRECISION);
    }

    @Test
    public void testDistanceWhenSeparatedByDiagonally() {
        Box r = box(0, 0, 2, 1);
        Box r2 = box(3, 6, 10, 8);

        assertEquals(Math.sqrt(26), r.distance(r2), PRECISION);
        assertEquals(Math.sqrt(26), r2.distance(r), PRECISION);
    }

    @Test
    public void testInequalityWithNull() {
        assertFalse(box(0, 0, 1, 1).equals(null));
    }

    @Test
    public void testSimpleEquality() {
        Box r = box(0, 0, 2, 1);
        Box r2 = box(0, 0, 2, 1);

        assertTrue(r.equals(r2));
    }

    @Test
    public void testSimpleInEquality1() {
        Box r = box(0, 0, 2, 1);
        Box r2 = box(0, 0, 2, 2);

        assertFalse(r.equals(r2));
    }

    @Test
    public void testSimpleInEquality2() {
        Box r = box(0, 0, 2, 1);
        Box r2 = box(1, 0, 2, 1);

        assertFalse(r.equals(r2));
    }

    @Test
    public void testSimpleInEquality3() {
        Box r = box(0, 0, 2, 1);
        Box r2 = box(0, 1, 2, 1);

        assertFalse(r.equals(r2));
    }

    @Test
    public void testSimpleInEquality4() {
        Box r = box(0, 0, 2, 2);
        Box r2 = box(0, 0, 1, 2);

        assertFalse(r.equals(r2));
    }

    @Test
    public void testGeometry() {
        Box r = box(0, 0, 2, 1);
        assertTrue(r.equals(r.geometry()));
    }

    @Test
    public void testIntersects() {
        Box a = box(14, 14, 86, 37);
        Box b = box(13, 23, 50, 80);
        assertTrue(a.intersects(b));
        assertTrue(b.intersects(a));
    }

    @Test
    public void testIntersectsNoRectangleContainsCornerOfAnother() {
        Box a = box(10, 10, 50, 50);
        Box b = box(28.0, 4.0, 34.0, 85.0);
        assertTrue(a.intersects(b));
        assertTrue(b.intersects(a));
    }

    @Test
    public void testIntersectsOneRectangleContainsTheOther() {
        Box a = box(10, 10, 50, 50);
        Box b = box(20, 20, 40, 40);
        assertTrue(a.intersects(b));
        assertTrue(b.intersects(a));
    }
    
    private static Box box(double x1, double y1, double x2, double y2) {
        return Box.create(x1, y1, 0, x2, y2, 1);
    }
    
}