package com.github.davidmoten.rtree3d;

import java.util.Collections;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

import rx.Observable;
import rx.functions.Func1;

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

    public static <T, S extends Geometry> RTree<T, S> create() {
        return new RTree<T, S>(Optional.absent(), 0,
                new Context(2, 4, new SelectorMinimalVolumeIncrease(), new SplitterQuadratic()));
    }

    @SuppressWarnings("unchecked")
    public RTree<T, S> add(Entry<? extends T, ? extends S> entry) {
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
            return new RTree<T, S>(
                    new Leaf<T, S>(Collections.singletonList((Entry<T, S>) entry), context),
                    size + 1, context);
    }
    
    public RTree<T, S> add(T t, S geometry) {
        return add(Entry.entry(t, geometry));
    }
    
    /**
     * <p>
     * Returns an Observable sequence of {@link Entry} that satisfy the given
     * condition. Note that this method is well-behaved only if:
     * </p>
     * 
     * <code>condition(g) is true for {@link Geometry} g implies condition(r) is true for the minimum bounding rectangles of the ancestor nodes</code>
     * 
     * <p>
     * <code>distance(g) &lt; sD</code> is an example of such a condition.
     * </p>
     * 
     * @param condition
     *            return Entries whose geometry satisfies the given condition
     * @return sequence of matching entries
     */
    @VisibleForTesting
    Observable<Entry<T, S>> search(Func1<? super Geometry, Boolean> condition) {
        if (root.isPresent())
            return Observable.create(new OnSubscribeSearch<T, S>(root.get(), condition));
        else
            return Observable.empty();
    }
    
    /**
     * Returns a predicate function that indicates if {@link Geometry}
     * intersects with a given rectangle.
     * 
     * @param r
     *            the rectangle to check intersection with
     * @return whether the geometry and the rectangle intersect
     */
    public static Func1<Geometry, Boolean> intersects(final Box r) {
        return new Func1<Geometry, Boolean>() {
            @Override
            public Boolean call(Geometry g) {
                return g.intersects(r);
            }
        };
    }
    
    /**
     * Returns an {@link Observable} sequence of all {@link Entry}s in the
     * R-tree whose minimum bounding rectangle intersects with the given
     * rectangle.
     * 
     * @param r
     *            rectangle to check intersection with the entry mbr
     * @return entries that intersect with the rectangle r
     */
    public Observable<Entry<T, S>> search(final Box r) {
        return search(intersects(r));
    }

    /**
     * Returns an {@link Observable} sequence of all {@link Entry}s in the
     * R-tree whose minimum bounding rectangle intersects with the given point.
     * 
     * @param p
     *            point to check intersection with the entry mbr
     * @return entries that intersect with the point p
     */
    public Observable<Entry<T, S>> search(final Point p) {
        return search(p.mbb());
    }

    /**
     * Returns all entries in the tree as an {@link Observable} sequence.
     * 
     * @return all entries in the R-tree
     */
    public Observable<Entry<T, S>> entries() {
        return search(ALWAYS_TRUE);
    }
    
    /**
     * Returns the always true predicate. See {@link RTree#entries()} for
     * example use.
     */
    private static final Func1<Geometry, Boolean> ALWAYS_TRUE = new Func1<Geometry, Boolean>() {
        @Override
        public Boolean call(Geometry rectangle) {
            return true;
        }
    };


}
