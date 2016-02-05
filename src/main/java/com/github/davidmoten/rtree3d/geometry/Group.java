package com.github.davidmoten.rtree3d.geometry;

import java.util.List;

import com.github.davidmoten.rtree3d.Util;

public class Group<T extends HasGeometry> implements HasGeometry {

    private final List<T> list;
    private final Box mbr;

    public Group(List<T> list) {
        this.list = list;
        this.mbr = Util.mbr(list);
    }

    public List<T> list() {
        return list;
    }

    @Override
    public Geometry geometry() {
        return mbr;
    }

}
