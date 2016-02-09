package com.github.davidmoten.rtree3d;

import java.util.List;

import com.github.davidmoten.rtree3d.geometry.Box;
import com.github.davidmoten.rtree3d.geometry.HasGeometry;
import com.github.davidmoten.rtree3d.geometry.ListPair;

import rx.functions.Func1;

/**
 * Utility functions for making {@link Selector}s and {@link Splitter}s.
 *
 */
public final class Functions {

    private Functions() {
        // prevent instantiation
    }

    public static final Func1<ListPair<? extends HasGeometry>, Double> overlapListPair = new Func1<ListPair<? extends HasGeometry>, Double>() {

        @Override
        public Double call(ListPair<? extends HasGeometry> pair) {
            return (double) pair.group1().geometry().mbb()
                    .intersectionVolume(pair.group2().geometry().mbb());
        }
    };

    public static Func1<HasGeometry, Double> overlapVolume(final Box r,
            final List<? extends HasGeometry> list) {
        return new Func1<HasGeometry, Double>() {

            @Override
            public Double call(HasGeometry g) {
                Box gPlusR = g.geometry().mbb().add(r);
                double m = 0;
                for (HasGeometry other : list) {
                    if (other != g) {
                        m += gPlusR.intersectionVolume(other.geometry().mbb());
                    }
                }
                return m;
            }
        };
    }

    public static Func1<HasGeometry, Double> volumeIncrease(final Box r) {
        return new Func1<HasGeometry, Double>() {
            @Override
            public Double call(HasGeometry g) {
                Box gPlusR = g.geometry().mbb().add(r);
                return (double) (gPlusR.volume() - g.geometry().mbb().volume());
            }
        };
    }

}
