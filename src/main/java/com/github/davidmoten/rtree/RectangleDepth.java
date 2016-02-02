package com.github.davidmoten.rtree;

import com.github.davidmoten.rtree.geometry.Box;

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
