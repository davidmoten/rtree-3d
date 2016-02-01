package com.github.davidmoten.rtree3d;

import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public final class RTree<T, S extends Geometry> {
    
    private final Optional<? extends Node<T, S>> root;
    private final Context context;
    /**
     * Current size in Entries of the RTree.
     */
    private int size;
    
    private RTree(Optional<? extends Node<T, S>> root, int size, Context context) {
        this.root = root;
        this.size = size;
        this.context = context;
    }
    
    private RTree(Node<T, S> rootNode, int size, Context context) {
        this(Optional.of(rootNode), size, context);
    }

    public RTree<T, S> add(Entry<? extends T,? extends S> entry) {
        if (root.isPresent()) {
            List<Node<T, S>> nodes = root.get().add(entry);
            Node<T, S> node;
            if (nodes.size() == 1)
                node = nodes.get(0);
            else {
                node = new NonLeaf<T, S>(nodes, context);
            }
            return new RTree<T, S>(node, size + 1, context);
        } else
            return new RTree<T, S>(new Leaf<T, S>(Lists.newArrayList((Entry<T, S>) entry), context),
                    size + 1, context);
    }

}
