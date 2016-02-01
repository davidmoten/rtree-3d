package com.github.davidmoten.rtree3d;

import rx.functions.Func1;

/**
 * Utility functions for making {@link Selector}s and {@link Splitter}s.
 *
 */
public final class Functions {

    private Functions() {
        // prevent instantiation
    }

    public static Func1<HasGeometry, Double> volumeIncrease(final Box b) {
        return new Func1<HasGeometry, Double>() {
            @Override
            public Double call(HasGeometry g) {
                Box gPlusB = g.geometry().mbb().add(b);
                return (double) (gPlusB.volume() - g.geometry().mbb().volume());
            }
        };
    }

}
