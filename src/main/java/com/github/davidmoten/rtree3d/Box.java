package com.github.davidmoten.rtree3d;

import com.google.common.base.Preconditions;

public final class Box implements Geometry {

    private final float minX, minY, minZ, maxX, maxY, maxZ;

    private Box(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        Preconditions.checkArgument(minX <= maxX);
        Preconditions.checkArgument(minY <= maxY);
        Preconditions.checkArgument(minZ <= maxZ);
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public static Box create(float minX, float minY, float minZ, float maxX, float maxY,
            float maxZ) {
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public float minX() {
        return minX;
    }

    public float minY() {
        return minY;
    }

    public float minZ() {
        return minZ;
    }

    public float maxX() {
        return maxX;
    }

    public float maxY() {
        return maxY;
    }

    public float maxZ() {
        return maxZ;
    }

    @Override
    public float distance(Box box) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Box mbb() {
        return this;
    }

    @Override
    public boolean intersects(Box r) {
        float xMaxMin = Math.max(minX(), r.minX());
        float xMinMax = Math.min(maxX(), r.maxX());
        if (xMinMax<xMaxMin) 
            return false;
        else {
            float yMaxMin = Math.max(minY(), r.minY());
            float yMinMax = Math.min(maxY(), r.maxY());
            if( yMinMax>=yMaxMin) {
                float zMaxMin = Math.max(minY(), r.minY());
                float zMinMax = Math.min(maxY(), r.maxY());
                return zMinMax >= zMaxMin;
            }
            else return false;
        }
    }

    public float volume() {
        return (maxX - minX) * (maxY - minY) * (maxZ - minZ);
    }

    public Box add(Box b) {
        return create(Math.min(minX, b.minX), Math.min(minY, b.minY), Math.min(minZ, b.minZ),
                Math.max(maxX, b.maxX), Math.max(maxY, b.maxY), Math.max(maxZ, b.maxZ));
    }

}
