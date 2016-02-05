package com.github.davidmoten.rtree3d;

import static com.github.davidmoten.rtree3d.RTreeTest.e;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;

import com.github.davidmoten.junit.Asserts;
import com.github.davidmoten.rtree3d.Backpressure;
import com.github.davidmoten.rtree3d.Entry;
import com.github.davidmoten.rtree3d.Node;
import com.github.davidmoten.rtree3d.NodePosition;
import com.github.davidmoten.rtree3d.RTree;
import com.github.davidmoten.rtree3d.geometry.Box;
import com.github.davidmoten.rtree3d.geometry.Geometry;
import com.github.davidmoten.util.ImmutableStack;

import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;

public class BackpressureTest {

    @Test
    public void testConstructorIsPrivate() {
        Asserts.assertIsUtilityClass(Backpressure.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBackpressureSearch() {
        Subscriber<Object> sub = Mockito.mock(Subscriber.class);
        ImmutableStack<NodePosition<Object, Geometry>> stack = ImmutableStack.empty();
        Func1<Geometry, Boolean> condition = Mockito.mock(Func1.class);
        Backpressure.search(condition, sub, stack, 1);
        Mockito.verify(sub, Mockito.never()).onNext(Mockito.any());
    }

    @Test
    public void testBackpressureSearchNodeWithConditionThatAlwaysReturnsFalse() {
        RTree<Object, Box> tree = RTree.maxChildren(3).<Object, Box> create().add(e(1))
                .add(e(3)).add(e(5)).add(e(7));

        Set<Entry<Object, Box>> found = new HashSet<Entry<Object, Box>>();
        tree.search(e(1).geometry()).subscribe(backpressureSubscriber(found));
        assertEquals(1, found.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRequestZero() {
        Subscriber<Object> sub = new Subscriber<Object>() {

            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Object t) {

            }
        };
        sub.add(new Subscription() {
            volatile boolean subscribed = true;

            @Override
            public void unsubscribe() {
                subscribed = false;
            }

            @Override
            public boolean isUnsubscribed() {
                return !subscribed;
            }
        });
        Node<Object, Geometry> node = Mockito.mock(Node.class);
        NodePosition<Object, Geometry> np = new NodePosition<Object, Geometry>(node, 1);
        ImmutableStack<NodePosition<Object, Geometry>> stack = ImmutableStack
                .<NodePosition<Object, Geometry>> empty().push(np);
        Func1<Geometry, Boolean> condition = Mockito.mock(Func1.class);
        ImmutableStack<NodePosition<Object, Geometry>> stack2 = Backpressure.search(condition, sub,
                stack, 0);
        assertTrue(stack2 == stack);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRequestZeroWhenUnsubscribed() {
        Subscriber<Object> sub = new Subscriber<Object>() {

            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Object t) {

            }
        };
        sub.add(new Subscription() {

            volatile boolean subscribed = true;

            @Override
            public void unsubscribe() {
                subscribed = false;
            }

            @Override
            public boolean isUnsubscribed() {
                return !subscribed;
            }
        });
        sub.unsubscribe();
        Node<Object, Geometry> node = Mockito.mock(Node.class);
        NodePosition<Object, Geometry> np = new NodePosition<Object, Geometry>(node, 1);
        ImmutableStack<NodePosition<Object, Geometry>> stack = ImmutableStack
                .<NodePosition<Object, Geometry>> empty().push(np);
        Func1<Geometry, Boolean> condition = Mockito.mock(Func1.class);
        ImmutableStack<NodePosition<Object, Geometry>> stack2 = Backpressure.search(condition, sub,
                stack, 1);
        assertTrue(stack2.isEmpty());
    }

    @Test
    public void testBackpressureIterateWhenNodeHasMaxChildrenAndIsRoot() {
        Entry<Object, Box> e1 = RTreeTest.e(1);
        @SuppressWarnings("unchecked")
        List<Entry<Object, Box>> list = Arrays.asList(e1, e1, e1, e1);
        RTree<Object, Box> tree = RTree.star().maxChildren(4).<Object, Box> create()
                .add(list);
        HashSet<Entry<Object, Box>> expected = new HashSet<Entry<Object, Box>>(list);
        final HashSet<Entry<Object, Box>> found = new HashSet<Entry<Object, Box>>();
        tree.entries().subscribe(backpressureSubscriber(found));
        assertEquals(expected, found);
    }

    @Test
    public void testBackpressureRequestZero() {
        Entry<Object, Box> e1 = RTreeTest.e(1);
        @SuppressWarnings("unchecked")
        List<Entry<Object, Box>> list = Arrays.asList(e1, e1, e1, e1);
        RTree<Object, Box> tree = RTree.star().maxChildren(4).<Object, Box> create()
                .add(list);
        HashSet<Entry<Object, Box>> expected = new HashSet<Entry<Object, Box>>(list);
        final HashSet<Entry<Object, Box>> found = new HashSet<Entry<Object, Box>>();
        tree.entries().subscribe(new Subscriber<Entry<Object, Box>>() {

            @Override
            public void onStart() {
                request(1);
            }

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Entry<Object, Box> t) {
                found.add(t);
                request(0);
            }
        });
        assertEquals(expected, found);
    }

    @Test
    public void testBackpressureIterateWhenNodeHasMaxChildrenAndIsNotRoot() {
        Entry<Object, Box> e1 = RTreeTest.e(1);
        List<Entry<Object, Box>> list = new ArrayList<Entry<Object, Box>>();
        for (int i = 1; i <= 17; i++)
            list.add(e1);
        RTree<Object, Box> tree = RTree.star().maxChildren(4).<Object, Box> create()
                .add(list);
        HashSet<Entry<Object, Box>> expected = new HashSet<Entry<Object, Box>>(list);
        final HashSet<Entry<Object, Box>> found = new HashSet<Entry<Object, Box>>();
        tree.entries().subscribe(backpressureSubscriber(found));
        assertEquals(expected, found);
    }

    @Test
    public void testBackpressureIterateWhenConditionFailsAgainstNonLeafNode() {
        Entry<Object, Box> e1 = e(1);
        List<Entry<Object, Box>> list = new ArrayList<Entry<Object, Box>>();
        for (int i = 1; i <= 17; i++)
            list.add(e1);
        list.add(e(2));
        RTree<Object, Box> tree = RTree.star().maxChildren(4).<Object, Box> create()
                .add(list);
        HashSet<Entry<Object, Box>> expected = new HashSet<Entry<Object, Box>>(list);
        final HashSet<Entry<Object, Box>> found = new HashSet<Entry<Object, Box>>();
        tree.entries().subscribe(backpressureSubscriber(found));
        assertEquals(expected, found);
    }

    @Test
    public void testBackpressureIterateWhenConditionFailsAgainstLeafNode() {
        Entry<Object, Box> e3 = e(3);
        RTree<Object, Box> tree = RTree.star().maxChildren(4).<Object, Box> create()
                .add(e(1)).add(e3);
        Set<Entry<Object, Box>> expected = Collections.singleton(e3);
        final Set<Entry<Object, Box>> found = new HashSet<Entry<Object, Box>>();
        tree.search(e3.geometry()).subscribe(backpressureSubscriber(found));
        assertEquals(expected, found);
    }

    @Test
    public void testBackpressureFastPathNotInitiatedTwice() {
        Entry<Object, Box> e3 = e(3);
        RTree<Object, Box> tree = RTree.star().maxChildren(4).<Object, Box> create()
                .add(e(1)).add(e3);
        Set<Entry<Object, Box>> expected = Collections.singleton(e3);
        final Set<Entry<Object, Box>> found = new HashSet<Entry<Object, Box>>();
        tree.search(e3.geometry()).subscribe(new Subscriber<Entry<Object, Box>>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Entry<Object, Box> t) {
                found.add(t);
                request(Long.MAX_VALUE);
            }
        });
        assertEquals(expected, found);
    }

    private static Subscriber<Entry<Object, Box>> backpressureSubscriber(
            final Set<Entry<Object, Box>> found) {
        return new Subscriber<Entry<Object, Box>>() {

            @Override
            public void onStart() {
                request(1);
            }

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Entry<Object, Box> t) {
                found.add(t);
                request(1);
            }
        };
    }

}
