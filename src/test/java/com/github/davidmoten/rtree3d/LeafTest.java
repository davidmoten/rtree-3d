package com.github.davidmoten.rtree3d;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import com.github.davidmoten.rtree3d.Context;
import com.github.davidmoten.rtree3d.Entry;
import com.github.davidmoten.rtree3d.Leaf;
import com.github.davidmoten.rtree3d.SelectorMinimalAreaIncrease;
import com.github.davidmoten.rtree3d.SplitterQuadratic;
import com.github.davidmoten.rtree3d.geometry.Box;
import com.github.davidmoten.rtree3d.geometry.Geometries;

public class LeafTest {

    private static Context context = new Context(2, 4, new SelectorMinimalAreaIncrease(),
            new SplitterQuadratic());

    @Test(expected = IllegalArgumentException.class)
    public void testCannotHaveZeroChildren() {
        new Leaf<Object, Box>(new ArrayList<Entry<Object, Box>>(), context);
    }

    @Test
    public void testMbr() {
        Box r1 = Geometries.box(0, 1, 3, 5);
        Box r2 = Geometries.box(1, 2, 4, 6);
        @SuppressWarnings("unchecked")
        Box r = new Leaf<Object, Box>(Arrays.asList(Entry.entry(new Object(), r1),
                Entry.entry(new Object(), r2)), context).geometry().mbr();
        assertEquals(r1.add(r2), r);
    }
}
