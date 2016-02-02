package com.github.davidmoten.rtree;

import com.github.davidmoten.rtree.geometry.Geometry;
import com.github.davidmoten.rtree.geometry.HasGeometry;
import com.github.davidmoten.rtree.geometry.Box;

public class Mbr implements HasGeometry {

    private final Box r;

    public Mbr(Box r) {
        this.r = r;
    }

    @Override
    public Geometry geometry() {
        return r;
    }

}
