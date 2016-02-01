package com.github.davidmoten.rtree3d;

import static com.github.davidmoten.rtree3d.Geometries.point;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RTreeTest {

    @Test
    public void test() {
        RTree<Object, Point> tree = RTree.create();
        tree = tree.add(new Object(), point(1, 1, 1));
        tree = tree.add(new Object(), point(1, 2, 1));
        tree = tree.add(new Object(), point(1, 3, 1));
        tree = tree.add(new Object(), point(2, 6, 2));
        tree = tree.add(new Object(), point(4, 5, 3));
        tree = tree.add(new Object(), point(6, 0, 1));
        tree = tree.add(new Object(), point(1, 1, 5));
        tree = tree.add(new Object(), point(3, 1, 1));
        assertEquals(8,(int) tree.entries().count().toBlocking().single());
    }
    
}
