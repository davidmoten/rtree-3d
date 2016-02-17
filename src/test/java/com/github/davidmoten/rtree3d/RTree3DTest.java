package com.github.davidmoten.rtree3d;

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.Test;

import com.github.davidmoten.rtree3d.geometry.Box;
import com.github.davidmoten.rtree3d.geometry.Geometry;
import com.github.davidmoten.rtree3d.geometry.Point;
import com.github.davidmoten.rtree3d.proto.PositionProtos.Position;
import com.github.davidmoten.rtree3d.proto.RTreeProtos;
import com.github.davidmoten.rtree3d.proto.RTreeProtos.Geom;
import com.github.davidmoten.rtree3d.proto.RTreeProtos.Node.Builder;
import com.github.davidmoten.rtree3d.proto.RTreeProtos.SubTreeId;
import com.github.davidmoten.rtree3d.proto.RTreeProtos.Tree;
import com.github.davidmoten.rx.Functions;
import com.github.davidmoten.rx.Strings;
import com.github.davidmoten.rx.slf4j.Logging;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.BinaryFixesFormat;
import au.gov.amsa.risky.format.Fix;
import rx.Observable;
import rx.Observable.Transformer;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

public class RTree3DTest {

    @Test
    public void createShuffle() throws FileNotFoundException {
        List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < 38377; i++) {
            list.add(i);
        }
        Collections.shuffle(list);
        PrintStream out = new PrintStream("target/order.txt");
        for (int i : list) {
            out.println(i);
        }
        out.close();
    }

    @Test
    public void test() throws IOException {
        // load entries, calculate bounds and normalize entries

        boolean useFixes = System.getProperty("fixes") != null;
        Observable<Entry<Object, Point>> entries;
        if (useFixes) {
            entries = getFixesShuffled();
        } else {
            entries = getGreekEarthquake3DDataShuffled();
        }

        final Box bounds = getBounds(entries);
        System.out.println(bounds);
        Observable<Entry<Object, Point>> normalized = normalize(entries, bounds);

        // create RTree
        int maxChildren = 4;
        RTree<Object, Point> tree = RTree.star().minChildren((maxChildren) / 2)
                .maxChildren(maxChildren).bounds(bounds).create();
        tree = tree.add(normalized).last().toBlocking().single();
        System.out.format("tree size=%s, depth=%s\\n", tree.size(), tree.calculateDepth());
        System.out.println(tree.asString(3));

        // try search on RTree
        long t = System.currentTimeMillis();
        int count = tree.search(
                Box.createNormalized(bounds, 39.0f, 22.0f, 0f, 40.0f, 23.0f, 3.15684946E11f))
                .count().toBlocking().single();
        t = System.currentTimeMillis() - t;
        System.out.println("search=" + count + " in " + t + "ms");
        // expect 118 records returned from search
        assertEquals(count, 118);

        // print out nodes as csv records for reading by R code and plotting
        for (int depth = 0; depth <= 10; depth++) {
            print(tree.root().get(), depth);
            System.out.println("depth file written " + depth);
        }

        Func1<Object, byte[]> serializer = new Func1<Object, byte[]>() {

            @Override
            public byte[] call(Object t) {
                return new byte[] {};
            }
        };
        // serialize RTree
        Tree protoTree = createProtoTree(tree, serializer);
        byte[] bytes = protoTree.toByteArray();
        System.out.println("bytes in protobuf = " + bytes.length);
        ByteArrayOutputStream b2 = new ByteArrayOutputStream();
        GZIPOutputStream g = new GZIPOutputStream(b2);
        g.write(bytes);
        g.close();
        System.out.println("zipped bytes = " + b2.size());
        System.out.println(1000000.0 / b2.size() * tree.size() + " positions = 1MB gzipped");

        // now create a node with the top portion of the r-tree down to a depth
        // with a number of total nodes less than a given maximum (but close
        // to). It's leaf nodes are uuids that correspond to serialized files in
        // dir for the rest of the r-tree at that leaf.
        File dir = new File("target/tree");
        dir.mkdirs();
        {
            for (int maxDepth = 3; maxDepth <= 3; maxDepth++) {
                for (File f : dir.listFiles())
                    f.delete();
                System.out.println("writing protos for top max depth=" + maxDepth);

                writeTreeAsSplitProtos(tree, maxDepth, dir, serializer);

                System.out.println("reading from protos");
                double sum = 0;
                long fileCount = 0;
                for (File file : dir.listFiles()) {
                    if (file.getName().equals("top")) {
                        TreeAndType tt = readTreeFromProto(file);
                        System.out.println("querying");
                        Box searchBox = createSearchBox(useFixes, bounds);
                        int c = tt.tree.search(searchBox).count().toBlocking().single();
                        System.out.println("found " + c + " in " + searchBox);
                    } else {
                        fileCount += 1;
                        sum = sum + file.length();
                    }
                }
                System.out.println(
                        "average sub-tree proto file size=" + Math.round(sum / fileCount) + "B");
            }
        }

        // search
        search(dir, useFixes);
        System.out.println("finished");

    }

    private static Observable<Entry<Object, Point>> normalize(
            Observable<Entry<Object, Point>> entries, final Box bounds) {
        return entries.map(new Func1<Entry<Object, Point>, Entry<Object, Point>>() {
            @Override
            public Entry<Object, Point> call(Entry<Object, Point> entry) {
                return Entry.entry(entry.value(), bounds.normalize(entry.geometry()));
            }
        }).lift(Logging.<Entry<Object, Point>> logger().showCount().showMemory().every(100000)
                .log());
    }

    private static Box getBounds(Observable<Entry<Object, Point>> entries) {
        return entries.reduce(null, new Func2<Box, Entry<Object, Point>, Box>() {
            @Override
            public Box call(Box box, Entry<Object, Point> p) {
                if (box == null)
                    return p.geometry().mbb();
                else
                    return Util.mbr(Lists.newArrayList(box, p.geometry().mbb()));
            }
        }).toBlocking().single();
    }

    private static Observable<Entry<Object, Point>> getFixesShuffled() {
        Observable<Entry<Object, Point>> entries;
        entries = BinaryFixes
                .from(new File(System.getProperty("fixes")), true, BinaryFixesFormat.WITH_MMSI)
                .map(new Func1<Fix, Entry<Object, Point>>() {
                    @Override
                    public Entry<Object, Point> call(Fix x) {
                        return Entry.entry(null, Point.create(x.lat(), x.lon(), x.time()));
                    }
                }).take(10000000);
        // shuffle entries
        entries = entries.toList().flatMapIterable(
                new Func1<List<Entry<Object, Point>>, Iterable<Entry<Object, Point>>>() {
                    @Override
                    public Iterable<Entry<Object, Point>> call(List<Entry<Object, Point>> list) {
                        System.out.println("shuffling");
                        Collections.shuffle(list);
                        System.out.println("shuffled");
                        return list;
                    }
                });
        return entries;
    }

    private static Observable<Entry<Object, Point>> getGreekEarthquake3DDataShuffled()
            throws IOException {
        final List<String> indexes = CharStreams.readLines(new InputStreamReader(
                RTree3DTest.class.getResourceAsStream("/greek-earthquake-shuffle.txt")));

        Observable<Entry<Object, Point>> entries = Observable
                .defer(new Func0<Observable<Entry<Object, Point>>>() {

                    @Override
                    public Observable<Entry<Object, Point>> call() {
                        try {
                            return Strings
                                    .from(new GZIPInputStream(RTree3DTest.class.getResourceAsStream(
                                            "/greek-earthquakes-1964-2000-with-times.txt.gz")))
                                    .compose(new Transformer<String, String>() {
                                @Override
                                public Observable<String> call(Observable<String> o) {
                                    return Strings.split(o, "\n");
                                }
                            }).filter(new Func1<String, Boolean>() {
                                @Override
                                public Boolean call(String line) {
                                    return !line.startsWith("DATE");
                                }
                            }).doOnNext(new Action1<String>() {
                                @Override
                                public void call(String line) {
                                    // System.out.println(line);
                                }
                            }).map(new Func1<String, String>() {
                                @Override
                                public String call(String line) {
                                    return line.trim();
                                }
                            }).filter(new Func1<String, Boolean>() {
                                @Override
                                public Boolean call(String line) {
                                    return line.length() > 0;
                                }
                            }).map(new Func1<String, Entry<Object, Point>>() {
                                @Override
                                public Entry<Object, Point> call(String line) {
                                    String[] items = line.split("\t");
                                    SimpleDateFormat sdf = new SimpleDateFormat(
                                            "yyy-MM-dd'T'HH:mm:ss.s");
                                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                    try {
                                        long time = sdf.parse(items[0]).getTime();
                                        float lat = Float.parseFloat(items[1]);
                                        float lon = Float.parseFloat(items[2]);
                                        return Entry.entry(null, Point.create(lat, lon, time));
                                    } catch (ParseException e) {
                                        throw new RuntimeException(e);
                                    }

                                }
                            }).toList().map(
                                    new Func1<List<Entry<Object, Point>>, List<Entry<Object, Point>>>() {
                                @Override
                                public List<Entry<Object, Point>> call(
                                        List<Entry<Object, Point>> list) {
                                    List<Entry<Object, Point>> list2 = new ArrayList<Entry<Object, Point>>();
                                    for (String index : indexes)
                                        list2.add(list.get(Integer.parseInt(index)));
                                    return list2;
                                }
                            }).flatMapIterable(Functions.<List<Entry<Object, Point>>> identity());
                        } catch (IOException e) {
                            return Observable.error(e);
                        }

                    }
                });
        return entries;
    }

    private Box createSearchBox(boolean useFixes, final Box bounds) {
        if (useFixes)
            return createSearchBoxPositions(bounds);
        else
            return createSearchBoxGreek(bounds);
    }

    private static Box createSearchBoxPositions(Box bounds) {
        // jervis bay
        long start = time("2014-01-01T12:00:00Z");
        long finish = time("2014-01-01T13:00:00Z");
        float lat1 = -35.287f;
        float lat2 = -34.849f;
        float lon1 = 150.469f;
        float lon2 = 151.1f;
        return Box.create(bounds.normX(lat1), bounds.normY(lon1), bounds.normZ(start),
                bounds.normX(lat2), bounds.normY(lon2), bounds.normZ(finish));
    }

    private static Box createSearchBoxGreek(Box bounds) {
        // greek earthquake data
        long start = time("1990-01-01T00:00:00Z");
        long finish = time("1991-01-01T00:00:00Z");
        float lat1 = 33f;
        float lat2 = 42f;
        float lon1 = 20f;
        float lon2 = 30f;
        return Box.create(bounds.normX(lat1), bounds.normY(lon1), bounds.normZ(start),
                bounds.normX(lat2), bounds.normY(lon2), bounds.normZ(finish));
    }

    private static <T, S extends Geometry> int depthWithMaxNodeCount(RTree<T, S> tree,
            int maxNodeCount) {
        Preconditions.checkArgument(maxNodeCount >= 0);
        int maxDepth = tree.calculateDepth();
        int depth = 0;
        int count = 0;
        while (true) {
            count = nodeCountAtDepth(tree, depth);
            if (count > maxNodeCount)
                return Math.max(0, depth - 1);
            else if (count == maxNodeCount)
                return depth;
            else
                depth++;
        }
    }

    private static <T, S extends Geometry> int nodeCountAtDepth(RTree<T, S> tree, int depth) {
        if (tree.root().isPresent())
            return nodeCountAtDepth(tree.root().get(), 0, depth);
        else
            return 0;
    }

    private static <T, S extends Geometry> int nodeCountAtDepth(Node<T, S> node, int depth,
            int maxDepth) {
        if (depth > maxDepth)
            return 0;

        if (node instanceof Leaf) {
            return ((Leaf<T, S>) node).count() + 1;
        } else {
            int count = 1;
            for (Node<T, S> child : ((NonLeaf<T, S>) node).children()) {
                count += nodeCountAtDepth(child, depth + 1, maxDepth);
            }
            return count;
        }
    }

    private void search(final File dir, boolean useFixes) {
        System.out.println("searching");
        final Scheduler scheduler = Schedulers.from(Executors.newFixedThreadPool(10));
        final RTree<String, Box> tree = readUpper(new File(dir, "top"));
        final Box searchBox = createSearchBox(useFixes, tree.context().bounds().get());
        long t = System.currentTimeMillis();
        int found = search(tree, searchBox, scheduler, dir).count().toBlocking().single();
        System.out.println(
                "search found " + found + " in " + (System.currentTimeMillis() - t) + "ms");
    }

    private Observable<Entry<Object, Geometry>> search(final RTree<String, Box> tree,
            final Box searchBox, final Scheduler scheduler, final File dir) {
        return tree.search(searchBox)
                .flatMap(new Func1<Entry<String, Box>, Observable<Entry<Object, Geometry>>>() {
                    @Override
                    public Observable<Entry<Object, Geometry>> call(
                            final Entry<String, Box> entry) {
                        return Observable.defer(new Func0<Observable<Entry<Object, Geometry>>>() {
                            @Override
                            public Observable<Entry<Object, Geometry>> call() {
                                String filename = entry.value();
                                System.out.println("reading " + filename);
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                                TreeAndType tt = readTreeFromProto(new File(dir, filename));
                                if (tt.hasLeaves)
                                    return tt.tree.search(searchBox);
                                else {
                                    return search((RTree<String, Box>) (RTree<?, ?>) tt.tree,
                                            searchBox, scheduler, dir);
                                }
                            }
                        }).subscribeOn(scheduler);
                    }
                });
    }

    private static byte[] readBytes(File file) {
        InputStream is = null;
        try {
            byte[] bytes = new byte[(int) file.length()];
            is = new BufferedInputStream(new FileInputStream(file));
            ByteStreams.readFully(is, bytes);
            return bytes;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static <T, S extends Geometry> Tree createProtoTree(RTree<T, S> tree,
            Func1<? super T, byte[]> serializer) {
        com.github.davidmoten.rtree3d.proto.RTreeProtos.Context c = createProtoContext(
                tree.context());
        com.github.davidmoten.rtree3d.proto.RTreeProtos.Node root = createProtoNode(
                tree.root().get(), serializer);
        return Tree.newBuilder().setContext(c).setRoot(root).setHasLeaves(true).build();
    }

    private static <T, S extends Geometry> com.github.davidmoten.rtree3d.proto.RTreeProtos.Context createProtoContext(
            Context context) {
        com.github.davidmoten.rtree3d.proto.RTreeProtos.Box b = createProtoBox(
                context.bounds().get());
        return com.github.davidmoten.rtree3d.proto.RTreeProtos.Context.newBuilder().setBounds(b)
                .setMinChildren(context.minChildren()).setMaxChildren(context.maxChildren())
                .build();
    }

    private static RTree<String, Box> readUpper(File file) {
        InputStream is = null;
        try {
            is = new BufferedInputStream(new GZIPInputStream(new FileInputStream(file)));
            com.github.davidmoten.rtree3d.proto.RTreeProtos.Tree tree = com.github.davidmoten.rtree3d.proto.RTreeProtos.Tree
                    .parseFrom(is);
            final Context ctx = createContext(tree.getContext());
            Node<String, Box> node = toUpperNode(tree.getRoot(), ctx);
            return RTree.create(node, ctx);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
        }
    }

    private static Context createContext(
            com.github.davidmoten.rtree3d.proto.RTreeProtos.Context c) {
        return new Context(c.getMinChildren(), c.getMaxChildren(), new SelectorRStar(),
                new SplitterRStar(), Optional.of(createBox(c.getBounds())));
    }

    private static <T, S extends Geometry> RTree<T, S> readLower(File file,
            Func1<byte[], T> deserializer) {
        InputStream is = null;
        try {
            is = new BufferedInputStream(new GZIPInputStream(new FileInputStream(file)));
            com.github.davidmoten.rtree3d.proto.RTreeProtos.Tree tree = com.github.davidmoten.rtree3d.proto.RTreeProtos.Tree
                    .parseFrom(is);
            final Context ctx = createContext(tree.getContext());
            Node<T, S> node = toLowerNode(tree.getRoot(), ctx, deserializer);
            return RTree.create(node, ctx);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
        }
    }

    private static <T, S extends Geometry> Node<T, S> toLowerNode(
            com.github.davidmoten.rtree3d.proto.RTreeProtos.Node node, Context context,
            Func1<? super byte[], ? extends T> deserializer) {
        Preconditions.checkArgument(node.getSubTreeIdsCount() > 0);
        Box box = createBox(node.getMbb());
        if (node.getChildrenCount() > 0) {
            // is non-leaf
            List<Node<T, S>> children = new ArrayList<Node<T, S>>();
            for (com.github.davidmoten.rtree3d.proto.RTreeProtos.Node n : node.getChildrenList()) {
                Node<T, S> nd = toLowerNode(n, context, deserializer);
                children.add(nd);
            }
            return new NonLeaf<T, S>(children, box, context);
        } else {
            // is leaf
            List<Entry<T, S>> entries = new ArrayList<Entry<T, S>>();
            for (com.github.davidmoten.rtree3d.proto.RTreeProtos.Entry ent : node
                    .getEntriesList()) {
                T t = deserializer.call(ent.getObject().toByteArray());
                S g = (S) createGeometry(ent.getGeometry());
                entries.add(Entry.entry(t, g));
            }
            return new Leaf<T, S>(entries, box, context);
        }
    }

    private static Node<String, Box> toUpperNode(
            com.github.davidmoten.rtree3d.proto.RTreeProtos.Node node, Context context) {
        Box box = createBox(node.getMbb());
        if (node.getSubTreeIdsCount() > 0) {
            // is leaf and has sub tree ids
            List<Entry<String, Box>> entries = new ArrayList<Entry<String, Box>>();
            for (SubTreeId id : node.getSubTreeIdsList()) {
                // TODO fix cast
                entries.add(Entry.entry(id.getId(), createBox(id.getMbb())));
            }
            return new Leaf<String, Box>(entries, box, context);
        } else {
            // is non-leaf
            List<Node<String, Box>> children = new ArrayList<Node<String, Box>>();
            for (com.github.davidmoten.rtree3d.proto.RTreeProtos.Node n : node.getChildrenList()) {
                Node<String, Box> nd = toUpperNode(n, context);
                children.add(nd);
            }
            return new NonLeaf<String, Box>(children, box, context);
        }
    }

    private static class TreeAndType {
        final RTree<Object, Geometry> tree;
        final boolean hasLeaves;

        TreeAndType(RTree<Object, Geometry> tree, boolean hasLeaves) {
            this.tree = tree;
            this.hasLeaves = hasLeaves;
        }

    }

    private static TreeAndType readTreeFromProto(File file) {
        InputStream is = null;
        try {
            is = new BufferedInputStream(new GZIPInputStream(new FileInputStream(file)));
            Tree tree = Tree.parseFrom(is);
            Context context = createContext(tree.getContext());
            Node<Object, Geometry> root = toNode(tree.getRoot(), context);
            RTree<Object, Geometry> tr = RTree.create(root, context);
            return new TreeAndType(tr, tree.getHasLeaves());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
        }
    }

    private static Box createBox(com.github.davidmoten.rtree3d.proto.RTreeProtos.Box b) {
        return Box.create(b.getXMin(), b.getYMin(), b.getZMin(), b.getXMax(), b.getYMax(),
                b.getZMax());
    }

    private static <T, S extends Geometry> com.github.davidmoten.rtree3d.proto.RTreeProtos.Node createProtoNode(
            Node<T, S> node, Func1<? super T, byte[]> serializer) {
        Builder b = RTreeProtos.Node.newBuilder();
        if (node instanceof Leaf) {
            for (Entry<T, S> entry : ((Leaf<T, S>) node).entries()) {
                b.addEntries(createProtoEntry(entry, serializer));
            }
        } else {
            // is NonLeaf
            NonLeaf<T, S> n = (NonLeaf<T, S>) node;
            for (Node<T, S> child : n.children()) {
                b.addChildren(createProtoNode(child, serializer));
            }
        }
        com.github.davidmoten.rtree3d.proto.RTreeProtos.Box box = createProtoBox(
                node.geometry().mbb());
        b.setMbb(box);
        return b.build();
    }

    private static <T, S extends Geometry> void writeTreeAsSplitProtos(RTree<T, S> tree,
            int maxDepth, File dir, Func1<? super T, byte[]> serializer) {
        com.github.davidmoten.rtree3d.proto.RTreeProtos.Node top = createProtoNodeSplit(
                tree.root().get(), 0, maxDepth, dir, serializer, tree.context());
        Tree t = Tree.newBuilder().setContext(createProtoContext(tree.context())).setRoot(top)
                .setHasLeaves(false).build();
        writeBytesToFile(t.toByteArray(), new File(dir, "top"), true);
    }

    private static <T, S extends Geometry> void writeNodeAsSplitProtos(Node<T, S> node,
            int maxDepth, File dir, Func1<? super T, byte[]> serializer, Context context) {
        writeBytesToFile(
                createProtoNodeSplit(node, 0, maxDepth, dir, serializer, context).toByteArray(),
                new File(dir, "top"), true);
    }

    private static void writeBytesToFile(byte[] bytes, File file, boolean zip) {
        OutputStream out = null;
        try {
            if (zip)
                out = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file)));
            else
                out = new BufferedOutputStream(new FileOutputStream(file));
            out.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (out != null)
                try {
                    out.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
        }
    }

    private static <T, S extends Geometry> com.github.davidmoten.rtree3d.proto.RTreeProtos.Node createProtoNodeSplit(
            Node<T, S> node, int depth, int maxDepth, File dir, Func1<? super T, byte[]> serializer,
            Context context) {
        Builder b = RTreeProtos.Node.newBuilder();
        if (depth <= maxDepth && node instanceof Leaf) {
            for (Entry<T, S> entry : ((Leaf<T, S>) node).entries()) {
                b.addEntries(createProtoEntry(entry, serializer));
            }
        } else if (depth < maxDepth && node instanceof NonLeaf) {
            // is NonLeaf
            NonLeaf<T, S> n = (NonLeaf<T, S>) node;
            for (Node<T, S> child : n.children()) {
                b.addChildren(
                        createProtoNodeSplit(child, depth + 1, maxDepth, dir, serializer, context));
            }
        } else if (depth == maxDepth && node instanceof NonLeaf) {
            // is NonLeaf
            NonLeaf<T, S> n = (NonLeaf<T, S>) node;
            for (Node<T, S> child : n.children()) {
                com.github.davidmoten.rtree3d.proto.RTreeProtos.Node protoNode = createProtoNode(
                        child, serializer);
                String id = UUID.randomUUID().toString().replace("-", "");
                File file = new File(dir, id);
                Tree tree = Tree.newBuilder().setRoot(protoNode).setHasLeaves(false)
                        .setContext(createProtoContext(context)).build();
                writeBytesToFile(tree.toByteArray(), file, true);
                b.addSubTreeIds(SubTreeId.newBuilder().setId(id)
                        .setMbb(createProtoBox(child.geometry().mbb())));
            }
        } else {
            throw new RuntimeException("unexpected");
        }
        b.setMbb(createProtoBox(node.geometry().mbb()));
        return b.build();

    }

    private static com.github.davidmoten.rtree3d.proto.RTreeProtos.Box createProtoBox(Box box) {
        return com.github.davidmoten.rtree3d.proto.RTreeProtos.Box.newBuilder().setXMin(box.x1())
                .setXMax(box.x2()).setYMin(box.y1()).setYMax(box.y2()).setZMin(box.z1())
                .setZMax(box.z2()).build();
    }

    private static Node<Object, Geometry> toNode(
            com.github.davidmoten.rtree3d.proto.RTreeProtos.Node node, Context context) {
        Box box = createBox(node.getMbb());
        if (node.getSubTreeIdsCount() > 0) {
            // is leaf and has sub tree ids
            List<Entry<Object, Geometry>> entries = new ArrayList<Entry<Object, Geometry>>();
            for (SubTreeId id : node.getSubTreeIdsList()) {
                entries.add(Entry.entry((Object) id.getId(), (Geometry) createBox(id.getMbb())));
            }
            return new Leaf<Object, Geometry>(entries, box, context);
        } else if (node.getChildrenCount() > 0) {
            // is non-leaf
            List<Node<Object, Geometry>> children = new ArrayList<Node<Object, Geometry>>();
            for (com.github.davidmoten.rtree3d.proto.RTreeProtos.Node n : node.getChildrenList()) {
                children.add(toNode(n, context));
            }
            return new NonLeaf<Object, Geometry>(children, box, context);
        } else {
            // is leaf
            List<Entry<Object, Geometry>> entries = new ArrayList<Entry<Object, Geometry>>();
            for (com.github.davidmoten.rtree3d.proto.RTreeProtos.Entry ent : node
                    .getEntriesList()) {
                Position p;
                try {
                    p = Position.parseFrom(ent.getObject());
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
                Geometry g = createGeometry(ent.getGeometry());
                entries.add(Entry.entry((Object) p, g));
            }
            return new Leaf<Object, Geometry>(entries, box, context);
        }
    }

    private static Geometry createGeometry(Geom g) {
        if (g.hasPoint()) {
            com.github.davidmoten.rtree3d.proto.RTreeProtos.Point p = g.getPoint();
            return Point.create(p.getX(), p.getY(), p.getZ());
        } else {
            return createBox(g.getBox());
        }
    }

    private static <T extends Geometry> void print(Node<Object, T> node, int depth)
            throws FileNotFoundException {

        PrintStream out = new PrintStream("target/out" + depth + ".txt");
        print(node, out, depth, depth);
        out.close();

    }

    private static <T extends Geometry> void print(Node<Object, T> node, PrintStream out,
            int minDepth, int maxDepth) {
        print(node, out, 0, minDepth, maxDepth);
    }

    private static <T extends Geometry> void print(Node<Object, T> node, PrintStream out, int depth,
            int minDepth, int maxDepth) {
        if (depth > maxDepth) {
            return;
        }
        if (node instanceof NonLeaf) {
            NonLeaf<Object, T> n = (NonLeaf<Object, T>) node;
            Box b = node.geometry().mbb();
            if (depth >= minDepth)
                print(b, out);
            for (Node<Object, T> child : n.children()) {
                print(child, out, depth + 1, minDepth, maxDepth);
            }
        } else if (node instanceof Leaf && depth >= minDepth) {
            Leaf<Object, T> n = (Leaf<Object, T>) node;
            print(n.geometry().mbb(), out);
        }
    }

    private static void print(Box b, PrintStream out) {
        out.format("%s,%s,%s,%s,%s,%s\n", b.x1(), b.y1(), b.z1(), b.x2(), b.y2(), b.z2());
    }

    private static long time(String isoDateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return sdf.parse(isoDateTime).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T, S extends Geometry> com.github.davidmoten.rtree3d.proto.RTreeProtos.Entry createProtoEntry(
            Entry<T, S> entry, Func1<? super T, byte[]> serializer) {
        // Position p =
        // Position.newBuilder().setIdentifierType(1).setValueInteger(123456789)
        // .setLatitude(entry.invX(point.geometry().x()))
        // .setLongitude(entry.invY(point.geometry().y()))
        // .setTimeEpochMs(Math.round((double)
        // entry.invZ(point.geometry().z()))).build();

        byte[] p = serializer.call(entry.value());
        com.github.davidmoten.rtree3d.proto.RTreeProtos.Entry ent = com.github.davidmoten.rtree3d.proto.RTreeProtos.Entry
                .newBuilder().setGeometry(createGeom(entry.geometry()))
                .setObject(ByteString.copyFrom(p)).build();
        return ent;
    }

    private static Geom createGeom(Geometry g) {
        com.github.davidmoten.rtree3d.proto.RTreeProtos.Geom.Builder b = Geom.newBuilder();
        if (g instanceof Box) {
            b.setBox(createProtoBox((Box) g));
        } else {
            // is Point
            b.setPoint(createProtoPoint((Point) g));
        }
        return b.build();
    }

    private static com.github.davidmoten.rtree3d.proto.RTreeProtos.Point createProtoPoint(Point g) {
        return com.github.davidmoten.rtree3d.proto.RTreeProtos.Point.newBuilder().setX(g.x())
                .setY(g.y()).setZ(g.z()).build();

    }

    public static void main(String[] args) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        System.out.println(sdf.parse("2014-01-01T12:00:00Z").getTime());
    }

}
