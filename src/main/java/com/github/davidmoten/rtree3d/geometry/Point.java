package com.github.davidmoten.rtree3d.geometry;

import com.github.davidmoten.util.ObjectsHelper;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

public final class Point implements Geometry {

    private final Box mbr;

    Point(float x, float y, float z) {
        this.mbr = Box.create(x, y, z, x, y, z);
    }

    public static Point create(double x, double y) {
        return new Point((float) x, (float) y, 0);
    }

    public static Point create(double x, double y, double z) {
        return new Point((float) x, (float) y, (float) z);
    }

    @Override
    public Box mbr() {
        return mbr;
    }

    @Override
    public double distance(Box r) {
        return mbr.distance(r);
    }

    public double distance(Point p) {
        return Math.sqrt(distanceSquared(p));
    }

    public double distanceSquared(Point p) {
        float dx = mbr().x1() - p.mbr().x1();
        float dy = mbr().y1() - p.mbr().y1();
        float dz = mbr().z1() - p.mbr().z1();
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public boolean intersects(Box r) {
        return mbr.intersects(r);
    }

    public float x() {
        return mbr.x1();
    }

    public float y() {
        return mbr.y1();
    }

    public float z() {
        return mbr.z1();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mbr);
    }

    @Override
    public boolean equals(Object obj) {
        Optional<Point> other = ObjectsHelper.asClass(obj, Point.class);
        if (other.isPresent()) {
            return Objects.equal(mbr, other.get().mbr());
        } else
            return false;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Point [x=");
        b.append(mbr.x1());
        b.append(", y=");
        b.append(mbr.y1());
        b.append(", z=");
        b.append(mbr.z1());
        b.append("]");
        return b.toString();
    }

}