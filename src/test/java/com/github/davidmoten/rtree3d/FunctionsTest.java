package com.github.davidmoten.rtree3d;

import org.junit.Test;

import com.github.davidmoten.junit.Asserts;
import com.github.davidmoten.rtree3d.Functions;

public class FunctionsTest {

    @Test
    public void testConstructorIsPrivate() {
        Asserts.assertIsUtilityClass(Functions.class);
    }
}
