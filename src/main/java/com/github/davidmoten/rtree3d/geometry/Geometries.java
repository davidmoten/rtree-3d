package com.github.davidmoten.rtree3d.geometry;

import com.google.common.annotations.VisibleForTesting;

public final class Geometries {

    private Geometries() {
        // prevent instantiation
    }

    public static Point point(double x, double y, double z) {
        return Point.create(x, y, z);
    }

    public static Box box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Box.create(x1, y1, z1, x2, y2, z2);
    }

    public static Box boxGeographic(double lon1, double lat1, double z1, double lon2, double lat2, double z2) {
        double x1 = normalizeLongitude(lon1);
        double x2 = normalizeLongitude(lon2);
        if (x2 < x1) {
            x2 += 360;
        }
        return box(x1, lat1, z1, x2, lat2, z2);
    }

    public static Point pointGeographic(double lon, double lat, double z) {
        return point(normalizeLongitude(lon), lat, z);
    }

    @VisibleForTesting
    static double normalizeLongitude(double d) {
        if (d == -180.0)
            return -180.0;
        else {
            double sign = Math.signum(d);
            double x = Math.abs(d) / 360;
            double x2 = (x - Math.floor(x)) * 360;
            if (x2 >= 180)
                x2 -= 360;
            return x2 * sign;
        }
    }

}
