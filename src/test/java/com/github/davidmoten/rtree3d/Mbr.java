package com.github.davidmoten.rtree3d;

import com.github.davidmoten.rtree3d.geometry.Box;
import com.github.davidmoten.rtree3d.geometry.Geometry;
import com.github.davidmoten.rtree3d.geometry.HasGeometry;

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
