package com.github.davidmoten.rtree3d.geometry;

import rx.functions.Func2;

public final class Intersects {

    private Intersects() {
        // prevent instantiation
    }

    public static final Func2<Box, Circle, Boolean> rectangleIntersectsCircle = new Func2<Box, Circle, Boolean>() {
        @Override
        public Boolean call(Box rectangle, Circle circle) {
            return circleIntersectsRectangle.call(circle, rectangle);
        }
    };

    public static final Func2<Circle, Box, Boolean> circleIntersectsRectangle = new Func2<Circle, Box, Boolean>() {
        @Override
        public Boolean call(Circle circle, Box rectangle) {
            return circle.intersects(rectangle);
        }
    };

    public static final Func2<Point, Circle, Boolean> pointIntersectsCircle = new Func2<Point, Circle, Boolean>() {
        @Override
        public Boolean call(Point point, Circle circle) {
            return circleIntersectsPoint.call(circle, point);
        }
    };

    public static final Func2<Circle, Point, Boolean> circleIntersectsPoint = new Func2<Circle, Point, Boolean>() {
        @Override
        public Boolean call(Circle circle, Point point) {
            return circle.intersects(point);
        }
    };

    public static final Func2<Circle, Circle, Boolean> circleIntersectsCircle = new Func2<Circle, Circle, Boolean>() {
        @Override
        public Boolean call(Circle a, Circle b) {
            return a.intersects(b);
        }
    };

   

    public static final Func2<Geometry, Circle, Boolean> geometryIntersectsCircle = new Func2<Geometry, Circle, Boolean>() {

        @Override
        public Boolean call(Geometry geometry, Circle circle) {
            if (geometry instanceof Box)
                return circle.intersects((Box) geometry);
            else if (geometry instanceof Circle)
                return circle.intersects((Circle) geometry);
            else if (geometry instanceof Point)
                return circle.intersects((Point) geometry);
            else
                throw new RuntimeException("unrecognized geometry: " + geometry);
        }
    };

    public static final Func2<Circle, Geometry, Boolean> circleIntersectsGeometry = new Func2<Circle, Geometry, Boolean>() {

        @Override
        public Boolean call(Circle circle, Geometry geometry) {
            return geometryIntersectsCircle.call(geometry, circle);
        }
    };

    public static final Func2<Geometry, Box, Boolean> geometryIntersectsRectangle = new Func2<Geometry, Box, Boolean>() {

        @Override
        public Boolean call(Geometry geometry, Box r) {
            if (geometry instanceof Box)
                return r.intersects((Box) geometry);
            else if (geometry instanceof Circle)
                return ((Circle) geometry).intersects(r);
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
            return geometryIntersectsRectangle.call(geometry, point.mbr());
        }
    };

    public static final Func2<Point, Geometry, Boolean> pointIntersectsGeometry = new Func2<Point, Geometry, Boolean>() {

        @Override
        public Boolean call(Point point, Geometry geometry) {
            return geometryIntersectsPoint.call(geometry, point);
        }
    };

}
