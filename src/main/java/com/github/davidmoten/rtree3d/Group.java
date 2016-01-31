package com.github.davidmoten.rtree3d;

import java.util.List;

public class Group<T extends HasGeometry> implements HasGeometry {

    private final List<T> list;
    private final Box mbb;

    public Group(List<T> list) {
        this.list = list;
        this.mbb = Util.mbb(list);
    }

    public List<T> list() {
        return list;
    }

    @Override
    public Geometry geometry() {
        return mbb;
    }

}
