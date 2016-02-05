package com.github.davidmoten.rtree3d;

import com.github.davidmoten.rtree3d.geometry.Box;

final class RectangleDepth {
    private final Box rectangle;
    private final int depth;

    RectangleDepth(Box rectangle, int depth) {
        super();
        this.rectangle = rectangle;
        this.depth = depth;
    }

    Box getRectangle() {
        return rectangle;
    }

    int getDepth() {
        return depth;
    }

}
