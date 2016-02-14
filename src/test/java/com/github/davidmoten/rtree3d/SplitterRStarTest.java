package com.github.davidmoten.rtree3d;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.github.davidmoten.rtree3d.SplitterRStar;
import com.github.davidmoten.rtree3d.geometry.Geometries;
import com.github.davidmoten.rtree3d.geometry.HasGeometry;
import com.github.davidmoten.rtree3d.geometry.ListPair;
import com.github.davidmoten.rtree3d.geometry.Point;
import com.google.common.collect.Lists;

public class SplitterRStarTest {

    @Test
    public void testGetPairs() {

        int minSize = 2;
        List<HasGeometry> list = Lists.newArrayList();
        list.add(point(1, 1).mbb());
        list.add(point(2, 2).mbb());
        list.add(point(3, 3).mbb());
        list.add(point(4, 4).mbb());
        list.add(point(5, 5).mbb());
        List<ListPair<HasGeometry>> pairs = SplitterRStar.getPairs(minSize, list);
        assertEquals(2, pairs.size());
    }
    
    private static Point point(double x, double y) {
        return Point.create(x,  y, 0);
    }
}
