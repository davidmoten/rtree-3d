package com.github.davidmoten.rtree3d.geometry;

import rx.functions.Func2;

public final class Intersects {

    private Intersects() {
        // prevent instantiation
    }

    public static final Func2<Geometry, Box, Boolean> geometryIntersectsRectangle = new Func2<Geometry, Box, Boolean>() {

        @Override
        public Boolean call(Geometry geometry, Box r) {
            if (geometry instanceof Box)
                return r.intersects((Box) geometry);
            else if (geometry instanceof Point)
                return ((Point) geometry).intersects(r);
            else
                throw new RuntimeException("unrecognized geometry: " + geometry);
        }
    };

    public static final Func2<Box, Geometry, Boolean> rectangleIntersectsGeometry = new Func2<Box, Geometry, Boolean>() {

        @Override
        public Boolean call(Box r, Geometry geometry) {
            return geometryIntersectsRectangle.call(geometry, r);
        }
    };

    public static final Func2<Geometry, Point, Boolean> geometryIntersectsPoint = new Func2<Geometry, Point, Boolean>() {

        @Override
        public Boolean call(Geometry geometry, Point point) {
            return geometryIntersectsRectangle.call(geometry, point.mbb());
        }
    };

    public static final Func2<Point, Geometry, Boolean> pointIntersectsGeometry = new Func2<Point, Geometry, Boolean>() {

        @Override
        public Boolean call(Point point, Geometry geometry) {
            return geometryIntersectsPoint.call(geometry, point);
        }
    };

}
