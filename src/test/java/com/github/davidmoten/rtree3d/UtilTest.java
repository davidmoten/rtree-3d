package com.github.davidmoten.rtree3d;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;

import com.github.davidmoten.junit.Asserts;
import com.github.davidmoten.rtree3d.Util;
import com.github.davidmoten.rtree3d.geometry.Box;
import com.github.davidmoten.rtree3d.geometry.Geometries;

public class UtilTest {

    @Test
    public void coverPrivateConstructor() {
        Asserts.assertIsUtilityClass(Util.class);
    }
    
    @Test
    public void testMbrWithNegativeValues() {
        Box r = Geometries.box(-2,-2,-1,-1);
        Box mbr = Util.mbr(Collections.singleton(r));
        assertEquals(r,mbr);
        System.out.println(r);
    }

}
