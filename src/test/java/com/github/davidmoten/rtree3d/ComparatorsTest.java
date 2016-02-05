package com.github.davidmoten.rtree3d;

import org.junit.Test;

import com.github.davidmoten.junit.Asserts;
import com.github.davidmoten.rtree3d.Comparators;

public class ComparatorsTest {

    @Test
    public void testConstructorIsPrivate() {
        Asserts.assertIsUtilityClass(Comparators.class);
    }

}
