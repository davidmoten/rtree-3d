package com.github.davidmoten.rtree3d;

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
import com.github.davidmoten.rtree3d.proto.RTreeProtos.Node.Builder;
import com.github.davidmoten.rtree3d.proto.RTreeProtos.SubTreeId;
import com.github.davidmoten.rx.Functions;
import com.github.davidmoten.rx.Strings;
import com.github.davidmoten.rx.slf4j.Logging;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
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
    public void testShuffle() throws FileNotFoundException {
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
        boolean useFixes = false && System.getProperty("fixes") != null;
        if (useFixes) {
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
                        public Iterable<Entry<Object, Point>> call(
                                List<Entry<Object, Point>> list) {
                            System.out.println("shuffling");
                            Collections.shuffle(list);
                            System.out.println("shuffled");
                            return list;
                        }
                    });
        }

        final Box bounds = entries.reduce(null, new Func2<Box, Entry<Object, Point>, Box>() {
            @Override
            public Box call(Box box, Entry<Object, Point> p) {
                if (box == null)
                    return p.geometry().mbb();
                else
                    return Util.mbr(Lists.newArrayList(box, p.geometry().mbb()));
            }
        }).toBlocking().single();

        File dir = new File("target/tree");
        dir.mkdirs();

        Observable<Entry<Object, Point>> normalized = entries
                .map(new Func1<Entry<Object, Point>, Entry<Object, Point>>() {
                    @Override
                    public Entry<Object, Point> call(Entry<Object, Point> entry) {
                        return Entry.entry(entry.value(), bounds.normalize(entry.geometry()));
                    }
                })
                //
                .lift(Logging.<Entry<Object, Point>> logger().showCount().showMemory().every(100000)
                        .log());
        System.out.println(bounds);
        int n = 4;

        RTree<Object, Point> tree = RTree.minChildren((n) / 2).maxChildren(n).create();
        tree = tree.add(normalized).last().toBlocking().single();
        System.out.format("tree size=%s, depth=%s\\n", tree.size(), tree.calculateDepth());
        System.out.println(tree.asString(3));
        long t = System.currentTimeMillis();
        int count = tree.search(Box.create(39.0, 22.0, 0, 40.0, 23.0, 3.15684946E11)).count()
                .toBlocking().single();
        t = System.currentTimeMillis() - t;
        System.out.println("search=" + count + " in " + t + "ms");
        for (int i = 0; i <= 10; i++) {
            print(tree.root().get(), i);
            System.out.println("depth file written " + i);
        }
        com.github.davidmoten.rtree3d.proto.RTreeProtos.Node pNode = toProtoNode(tree.root().get(),
                bounds);
        byte[] bytes = pNode.toByteArray();
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
        {
            for (int maxDepth = 4; maxDepth <= 8; maxDepth++) {
                for (File f : dir.listFiles())
                    f.delete();
                System.out.println("writing protos for top max depth=" + maxDepth);
                com.github.davidmoten.rtree3d.proto.RTreeProtos.Box protoBounds = com.github.davidmoten.rtree3d.proto.RTreeProtos.Box
                        .newBuilder().setXMin(bounds.x1()).setXMax(bounds.x2()).setYMin(bounds.y1())
                        .setYMax(bounds.y2()).setZMin(bounds.z1()).setZMax(bounds.z2()).build();
                com.github.davidmoten.rtree3d.proto.RTreeProtos.Context protoContext = com.github.davidmoten.rtree3d.proto.RTreeProtos.Context
                        .newBuilder().setBounds(protoBounds).setMinChildren(2).setMaxChildren(4)
                        .build();
                writeBytesToFile(protoContext.toByteArray(), new File(dir, "context"), false);

                writeNodeAsSplitProtos(tree.root().get(), bounds, maxDepth, dir);

                System.out.println("reading from protos");
                double sum = 0;
                long fileCount = 0;
                for (File file : dir.listFiles()) {
                    if (!file.getName().equals("context")) {
                        RTree<Object, Geometry> tr = readFromProto(file, tree.context());
                        if (file.getName().equals("top")) {
                            System.out.println("querying");
                            Box searchBox = createSearchBox(useFixes, bounds);
                            int c = tr.search(searchBox).count().toBlocking().single();
                            System.out.println("found " + c + " in " + searchBox);
                        } else {
                            fileCount += 1;
                            sum = sum + file.length();
                        }
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

    private void search(final File dir, boolean useFixes) {
        try {
            final Scheduler scheduler = Schedulers.from(Executors.newFixedThreadPool(1));
            long t = System.currentTimeMillis();
            com.github.davidmoten.rtree3d.proto.RTreeProtos.Context context = com.github.davidmoten.rtree3d.proto.RTreeProtos.Context
                    .parseFrom(readBytes(new File(dir, "context")));
            Box bounds = createBox(context.getBounds());
            final Context ctx = new Context(context.getMinChildren(), context.getMaxChildren(),
                    new SelectorRStar(), new SplitterRStar());
            RTree<String, Geometry> tree = (RTree<String, Geometry>) (RTree<?, ?>) readFromProto(
                    new File(dir, "top"), ctx);
            final Box searchBox = createSearchBox(useFixes, bounds);
            int found = tree.search(searchBox).flatMap(
                    new Func1<Entry<String, Geometry>, Observable<Entry<Object, Geometry>>>() {
                        @Override
                        public Observable<Entry<Object, Geometry>> call(
                                final Entry<String, Geometry> entry) {
                            return Observable
                                    .defer(new Func0<Observable<Entry<Object, Geometry>>>() {
                                @Override
                                public Observable<Entry<Object, Geometry>> call() {
                                    String filename = entry.value();
                                    System.out.println("reading " + filename);
                                    RTree<Object, Geometry> tree = readFromProto(
                                            new File(dir, filename), ctx);
                                    System.out.println("loaded " + filename);
                                    return tree.search(searchBox);
                                }
                            }).subscribeOn(scheduler);

                        }
                    }).count().toBlocking().single();
            System.out.println(
                    "search found " + found + " in " + (System.currentTimeMillis() - t) + "ms");
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
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

    private static RTree<Object, Geometry> readFromProto(File file, Context context) {
        InputStream is = null;
        try {
            is = new BufferedInputStream(new GZIPInputStream(new FileInputStream(file)));
            com.github.davidmoten.rtree3d.proto.RTreeProtos.Node node = com.github.davidmoten.rtree3d.proto.RTreeProtos.Node
                    .parseFrom(is);
            is.close();
            Node<Object, Geometry> root = toNode(node, context);
            RTree<Object, Geometry> tree = RTree.create(root, context);
            return tree;
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

    private static com.github.davidmoten.rtree3d.proto.RTreeProtos.Node toProtoNode(
            Node<Object, Point> node, Box bounds) {
        Builder b = RTreeProtos.Node.newBuilder();
        if (node instanceof Leaf) {
            for (Entry<Object, Point> entry : ((Leaf<Object, Point>) node).entries()) {
                b.addEntries(createProtoEntry(bounds, entry));
            }
        } else {
            // is NonLeaf
            NonLeaf<Object, Point> n = (NonLeaf<Object, Point>) node;
            for (Node<Object, Point> child : n.children()) {
                b.addChildren(toProtoNode(child, bounds));
            }
        }
        com.github.davidmoten.rtree3d.proto.RTreeProtos.Box box = createProtoBox(
                node.geometry().mbb());
        b.setMbb(box);
        return b.build();
    }

    private static void writeNodeAsSplitProtos(Node<Object, Point> node, Box bounds, int maxDepth,
            File dir) {
        writeBytesToFile(toProtoNodeSplit(node, bounds, 0, maxDepth, dir).toByteArray(),
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

    private static com.github.davidmoten.rtree3d.proto.RTreeProtos.Node toProtoNodeSplit(
            Node<Object, Point> node, Box bounds, int depth, int maxDepth, File dir) {
        Builder b = RTreeProtos.Node.newBuilder();
        if (depth <= maxDepth && node instanceof Leaf) {
            for (Entry<Object, Point> entry : ((Leaf<Object, Point>) node).entries()) {
                b.addEntries(createProtoEntry(bounds, entry));
            }
        } else if (depth < maxDepth && node instanceof NonLeaf) {
            // is NonLeaf
            NonLeaf<Object, Point> n = (NonLeaf<Object, Point>) node;
            for (Node<Object, Point> child : n.children()) {
                b.addChildren(toProtoNodeSplit(child, bounds, depth + 1, maxDepth, dir));
            }
        } else if (depth == maxDepth && node instanceof NonLeaf) {
            // is NonLeaf
            NonLeaf<Object, Point> n = (NonLeaf<Object, Point>) node;
            for (Node<Object, Point> child : n.children()) {
                com.github.davidmoten.rtree3d.proto.RTreeProtos.Node proto = toProtoNode(child,
                        bounds);
                String id = UUID.randomUUID().toString().replace("-", "");
                File file = new File(dir, id);
                writeBytesToFile(proto.toByteArray(), file, true);
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
                Box g = createBox(ent.getBox());
                entries.add(
                        Entry.entry((Object) p, (Geometry) Point.create(g.x1(), g.y1(), g.z1())));
            }
            return new Leaf<Object, Geometry>(entries, box, context);
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

    private static com.github.davidmoten.rtree3d.proto.RTreeProtos.Entry createProtoEntry(
            Box bounds, Entry<Object, Point> entry) {
        Position p = Position.newBuilder().setIdentifierType(1).setValueInteger(123456789)
                .setLatitude(bounds.invX(entry.geometry().x()))
                .setLongitude(bounds.invY(entry.geometry().y()))
                .setTimeEpochMs(Math.round((double) bounds.invZ(entry.geometry().z()))).build();
        com.github.davidmoten.rtree3d.proto.RTreeProtos.Entry ent = com.github.davidmoten.rtree3d.proto.RTreeProtos.Entry
                .newBuilder().setBox(createProtoBox(entry.geometry().mbb()))
                .setObject(p.toByteString()).build();
        return ent;
    }

    public static void main(String[] args) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        System.out.println(sdf.parse("2014-01-01T12:00:00Z").getTime());
    }

}
