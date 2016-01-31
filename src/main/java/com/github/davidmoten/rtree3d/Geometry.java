package com.github.davidmoten.rtree3d;

public interface Geometry {

    float distance(Box box);

    /**
     * Returns the minimum bounding rectangle of this geometry.
     * 
     * @return minimum bounding rectangle
     */
    Box mbb();

    boolean intersects(Box r);
}
