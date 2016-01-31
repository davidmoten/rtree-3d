package com.github.davidmoten.rtree3d;

public final class Geometries {

    public static Box box(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return Box.create(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static Point point(float x, float y, float z) {
        return new Point(x, y, z);
    }
}
