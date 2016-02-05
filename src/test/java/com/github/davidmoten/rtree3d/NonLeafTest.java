package com.github.davidmoten.rtree3d;

import java.util.Collections;

import org.junit.Test;

import com.github.davidmoten.rtree3d.Node;
import com.github.davidmoten.rtree3d.NonLeaf;
import com.github.davidmoten.rtree3d.geometry.Geometry;

public class NonLeafTest {

    @Test(expected=IllegalArgumentException.class)
    public void testNonLeafPrecondition() {
        new NonLeaf<Object,Geometry>(Collections.<Node<Object,Geometry>>emptyList(), null);
    }
    
}
