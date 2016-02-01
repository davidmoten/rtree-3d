package com.github.davidmoten.rtree3d;

import java.util.Comparator;

import rx.functions.Func1;

public final class Comparators {

    public static <R, T extends Comparable<T>> Comparator<R> toComparator(
            final Func1<R, T> function) {
        return new Comparator<R>() {

            @Override
            public int compare(R g1, R g2) {
                return function.call(g1).compareTo(function.call(g2));
            }
        };
    }

    @SafeVarargs
    public static <T> Comparator<T> compose(final Comparator<T>... comparators) {
        return new Comparator<T>() {
            @Override
            public int compare(T t1, T t2) {
                for (Comparator<T> comparator : comparators) {
                    int value = comparator.compare(t1, t2);
                    if (value != 0)
                        return value;
                }
                return 0;
            }
        };
    }

    public static <T extends HasGeometry> Comparator<HasGeometry> volumeIncreaseComparator(
            final Box r) {
        return toComparator(Functions.volumeIncrease(r));
    }

    public static Comparator<HasGeometry> volumeComparator(Box b) {
        return new Comparator<HasGeometry>() {

            @Override
            public int compare(HasGeometry g1, HasGeometry g2) {
                return ((Float) g1.geometry().mbb().add(b).volume())
                        .compareTo(g2.geometry().mbb().add(b).volume());
            }
        };
    }
}
