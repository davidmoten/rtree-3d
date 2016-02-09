package com.github.davidmoten.rtree3d;

import static com.github.davidmoten.rtree3d.Comparators.compose;
import static com.github.davidmoten.rtree3d.Comparators.volumeComparator;
import static com.github.davidmoten.rtree3d.Comparators.volumeIncreaseComparator;
import static java.util.Collections.min;

import java.util.List;

import com.github.davidmoten.rtree3d.geometry.Geometry;

/**
 * Uses minimal area increase to select a node from a list.
 *
 */
public final class SelectorMinimalVolumeIncrease implements Selector {

    @SuppressWarnings("unchecked")
    @Override
    public <T, S extends Geometry> Node<T, S> select(Geometry g, List<? extends Node<T, S>> nodes) {
        return min(nodes, compose(volumeIncreaseComparator(g.mbb()), volumeComparator(g.mbb())));
    }
}
