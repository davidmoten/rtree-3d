package com.github.davidmoten.rtree;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;

import com.github.davidmoten.junit.Asserts;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Box;

public class UtilTest {

    @Test
    public void coverPrivateConstructor() {
        Asserts.assertIsUtilityClass(Util.class);
    }
    
    @Test
    public void testMbrWithNegativeValues() {
        Box r = Geometries.rectangle(-2,-2,-1,-1);
        Box mbr = Util.mbr(Collections.singleton(r));
        assertEquals(r,mbr);
        System.out.println(r);
    }

}
