package com.github.davidmoten.rtree3d;

import static java.util.Collections.min;

import java.util.List;

public class SelectorMinimalVolumeIncrease implements Selector {

    @SuppressWarnings("unchecked")
    @Override
    public <T, S extends Geometry> Node<T, S> select(Geometry g, List<? extends Node<T, S>> nodes) {
        return min(nodes, Comparators.compose(Comparators.volumeIncreaseComparator(g.mbb()),
                Comparators.volumeComparator(g.mbb())));
    }

}
