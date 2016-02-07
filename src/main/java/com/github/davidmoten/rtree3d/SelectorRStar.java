package com.github.davidmoten.rtree3d;

import java.util.List;

import com.github.davidmoten.rtree3d.geometry.Geometry;

/**
 * Uses minimal overlap area selector for leaf nodes and minimal areea increase
 * selector for non-leaf nodes.
 */
public final class SelectorRStar implements Selector {

    private static Selector overlapVolumeSelector = new SelectorMinimalOverlapVolume();
    private static Selector volumeIncreaseSelector = new SelectorMinimalVolumeIncrease();

    @Override
    public <T, S extends Geometry> Node<T, S> select(Geometry g, List<? extends Node<T, S>> nodes) {
        boolean leafNodes = nodes.get(0) instanceof Leaf;
        if (leafNodes)
            return overlapVolumeSelector.select(g, nodes);
        else
            return volumeIncreaseSelector.select(g, nodes);
    }

}
