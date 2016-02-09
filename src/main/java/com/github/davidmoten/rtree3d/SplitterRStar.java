package com.github.davidmoten.rtree3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.davidmoten.rtree3d.geometry.HasGeometry;
import com.github.davidmoten.rtree3d.geometry.ListPair;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import rx.functions.Func1;

public final class SplitterRStar implements Splitter {

    private final Comparator<ListPair<?>> comparator;

    @SuppressWarnings("unchecked")
    public SplitterRStar() {
        this.comparator = Comparators.compose(Comparators.overlapListPairComparator,
                Comparators.volumePairComparator);
    }

    @Override
    public <T extends HasGeometry> ListPair<T> split(List<T> items, int minSize) {
        Preconditions.checkArgument(!items.isEmpty());
        // sort nodes into increasing x, calculate min overlap where both groups
        // have more than minChildren

        Map<SortType, List<ListPair<T>>> map = new HashMap<SortType, List<ListPair<T>>>(5, 1.0f);
        map.put(SortType.X_LOWER, getPairs(minSize, sort(items, INCREASING_X_LOWER)));
        map.put(SortType.X_UPPER, getPairs(minSize, sort(items, INCREASING_X_UPPER)));
        map.put(SortType.Y_LOWER, getPairs(minSize, sort(items, INCREASING_Y_LOWER)));
        map.put(SortType.Y_UPPER, getPairs(minSize, sort(items, INCREASING_Y_UPPER)));
        map.put(SortType.Z_LOWER, getPairs(minSize, sort(items, INCREASING_Z_LOWER)));
        map.put(SortType.Z_UPPER, getPairs(minSize, sort(items, INCREASING_Z_UPPER)));

        // compute S the sum of all margin-values of the lists above
        // the list with the least S is then used to find minimum overlap

        SortType leastMarginSumSortType = Collections.min(sortTypes, marginSumComparator(map));
        List<ListPair<T>> pairs = map.get(leastMarginSumSortType);

        return Collections.min(pairs, comparator);
    }

    private static enum SortType {
        X_LOWER, X_UPPER, Y_LOWER, Y_UPPER, Z_LOWER, Z_UPPER;
    }

    private static final List<SortType> sortTypes = Collections
            .unmodifiableList(Arrays.asList(SortType.values()));

    private static <T extends HasGeometry> Comparator<SortType> marginSumComparator(
            final Map<SortType, List<ListPair<T>>> map) {
        return Comparators.toComparator(new Func1<SortType, Double>() {
            @Override
            public Double call(SortType sortType) {
                return (double) marginValueSum(map.get(sortType));
            }
        });
    }

    private static <T extends HasGeometry> float marginValueSum(List<ListPair<T>> list) {
        float sum = 0;
        for (ListPair<T> p : list)
            sum += p.marginSum();
        return sum;
    }

    @VisibleForTesting
    static <T extends HasGeometry> List<ListPair<T>> getPairs(int minSize, List<T> list) {
        List<ListPair<T>> pairs = new ArrayList<ListPair<T>>(list.size() - 2 * minSize + 1);
        for (int i = minSize; i < list.size() - minSize + 1; i++) {
            List<T> list1 = list.subList(0, i);
            List<T> list2 = list.subList(i, list.size());
            ListPair<T> pair = new ListPair<T>(list1, list2);
            pairs.add(pair);
        }
        return pairs;
    }

    private static <T extends HasGeometry> List<T> sort(List<T> items,
            Comparator<HasGeometry> comparator) {
        ArrayList<T> list = new ArrayList<T>(items);
        Collections.sort(list, comparator);
        return list;
    }

    private static Comparator<HasGeometry> INCREASING_X_LOWER = new Comparator<HasGeometry>() {

        @Override
        public int compare(HasGeometry n1, HasGeometry n2) {
            return ((Float) n1.geometry().mbb().x1()).compareTo(n2.geometry().mbb().x1());
        }
    };

    private static Comparator<HasGeometry> INCREASING_X_UPPER = new Comparator<HasGeometry>() {

        @Override
        public int compare(HasGeometry n1, HasGeometry n2) {
            return ((Float) n1.geometry().mbb().x2()).compareTo(n2.geometry().mbb().x2());
        }
    };

    private static Comparator<HasGeometry> INCREASING_Y_LOWER = new Comparator<HasGeometry>() {

        @Override
        public int compare(HasGeometry n1, HasGeometry n2) {
            return ((Float) n1.geometry().mbb().y1()).compareTo(n2.geometry().mbb().y1());
        }
    };

    private static Comparator<HasGeometry> INCREASING_Y_UPPER = new Comparator<HasGeometry>() {

        @Override
        public int compare(HasGeometry n1, HasGeometry n2) {
            return ((Float) n1.geometry().mbb().y2()).compareTo(n2.geometry().mbb().y2());
        }
    };

    private static Comparator<HasGeometry> INCREASING_Z_LOWER = new Comparator<HasGeometry>() {

        @Override
        public int compare(HasGeometry n1, HasGeometry n2) {
            return ((Float) n1.geometry().mbb().z1()).compareTo(n2.geometry().mbb().z1());
        }
    };

    private static Comparator<HasGeometry> INCREASING_Z_UPPER = new Comparator<HasGeometry>() {

        @Override
        public int compare(HasGeometry n1, HasGeometry n2) {
            return ((Float) n1.geometry().mbb().z2()).compareTo(n2.geometry().mbb().z2());
        }
    };

}
