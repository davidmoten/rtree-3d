package com.github.davidmoten.rtree3d;

import java.util.List;

/**
 *
 * Not thread safe.
 *
 * @param <T>
 *            list type
 */
public final class ListPair<T extends HasGeometry> {
    private final Group<T> group1;
    private final Group<T> group2;
    // these non-final variable mean that this class is not thread-safe
    // because access to them is not synchronized
    private float volumeSum = -1;

    public ListPair(List<T> list1, List<T> list2) {
        this.group1 = new Group<T>(list1);
        this.group2 = new Group<T>(list2);
    }

    public Group<T> group1() {
        return group1;
    }

    public Group<T> group2() {
        return group2;
    }

    public float volumeSum() {
        if (volumeSum == -1)
            volumeSum = group1.geometry().mbb().volume() + group2.geometry().mbb().volume();
        return volumeSum;
    }

}
