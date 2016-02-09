package com.github.davidmoten.rtree3d;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.Test;

import com.github.davidmoten.rtree3d.geometry.Box;
import com.github.davidmoten.rtree3d.geometry.Geometry;
import com.github.davidmoten.rtree3d.geometry.Point;
import com.github.davidmoten.rtree3d.proto.RTreeProtos;
import com.github.davidmoten.rtree3d.proto.RTreeProtos.Node.Builder;
import com.github.davidmoten.rtree3d.proto.RTreeProtos.Position;
import com.github.davidmoten.rtree3d.proto.RTreeProtos.SubTreeId;
import com.github.davidmoten.rx.Strings;

import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.BinaryFixesFormat;
import au.gov.amsa.risky.format.Fix;
import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

public class RTree3DTest {

    @Test
    public void test() throws IOException {
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
                                        return Entry.entry(null,
                                                Point.create(lat, lon, time / 1E12));
                                    } catch (ParseException e) {
                                        throw new RuntimeException(e);
                                    }

                                }
                            });
                        } catch (IOException e) {
                            return Observable.error(e);
                        }

                    }
                });

        if ("true".equals(System.getProperty("fixes"))) {
            entries = BinaryFixes
                    .from(new File("/home/dave/2014-01-01-binary-fixes-with-mmsi"), true,
                            BinaryFixesFormat.WITH_MMSI)
                    .map(new Func1<Fix, Entry<Object, Point>>() {
                        @Override
                        public Entry<Object, Point> call(Fix x) {
                            return Entry.entry(null, Point.create(x.lat(), x.lon(), x.time()));
                        }
                    }).take(1000000).toList().flatMapIterable(
                            new Func1<List<Entry<Object, Point>>, Iterable<Entry<Object, Point>>>() {
                                @Override
                                public Iterable<Entry<Object, Point>> call(
                                        List<Entry<Object, Point>> list) {
                                    Collections.shuffle(list);
                                    return list;
                                }
                            });
        }

        Range info = new Range(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE,
                -Float.MAX_VALUE, -Float.MAX_VALUE);
        final Range range = entries.reduce(info, new Func2<Range, Entry<Object, Point>, Range>() {
            @Override
            public Range call(Range info, Entry<Object, Point> p) {
                return new Range(Math.min(info.minX, p.geometry().x()),
                        Math.min(info.minY, p.geometry().y()),
                        Math.min(info.minZ, p.geometry().z()),
                        Math.max(info.maxX, p.geometry().x()),
                        Math.max(info.maxY, p.geometry().y()),
                        Math.max(info.maxZ, p.geometry().z()));
            }
        }).toBlocking().single();
        Observable<Entry<Object, Point>> normalized = entries
                .map(new Func1<Entry<Object, Point>, Entry<Object, Point>>() {
                    @Override
                    public Entry<Object, Point> call(Entry<Object, Point> entry) {
                        return Entry.entry(entry.value(),
                                Point.create(range.normX(entry.geometry().x()),
                                        range.normY(entry.geometry().y()),
                                        range.normZ(entry.geometry().z())));
                    }
                });
        System.out.println(range);
        int n = 4;

        RTree<Object, Point> tree = RTree.star().minChildren((n) / 2).maxChildren(n).create();
        tree = tree.add(normalized).last().toBlocking().single();
        System.out.println(tree.size());
        System.out.println(tree.calculateDepth());
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
                range);
        byte[] bytes = pNode.toByteArray();
        System.out.println("bytes in protobuf = " + bytes.length);
        ByteArrayOutputStream b2 = new ByteArrayOutputStream();
        GZIPOutputStream g = new GZIPOutputStream(b2);
        g.write(bytes);
        g.close();
        System.out.println("zipped bytes = " + b2.size());

        System.out.println(1000000.0 / b2.size() * tree.size() + " positions = 1MB gzipped");

        File dir = new File("target/tree");
        dir.mkdir();
        for (File f : dir.listFiles())
            f.delete();

        // now create a node with the top portion of the r-tree down to a depth
        // with a number of total nodes less than a given maximum (but close
        // to). It's leaf nodes are uuids that correspond to serialized files in
        // dir for the rest of the r-tree at that leaf.

        writeNodeAsSplitProtos(tree.root().get(), range, 5, dir);

        System.out.println("finished");
    }

    private static Box createBox(com.github.davidmoten.rtree3d.proto.RTreeProtos.Box b) {
        return Box.create(b.getXMin(), b.getYMin(), b.getZMin(), b.getXMax(), b.getYMax(),
                b.getZMax());
    }

    private static com.github.davidmoten.rtree3d.proto.RTreeProtos.Node toProtoNode(
            Node<Object, Point> node, Range range) {
        Builder b = RTreeProtos.Node.newBuilder();
        if (node instanceof Leaf) {
            for (Entry<Object, Point> entry : ((Leaf<Object, Point>) node).entries()) {
                Position p = Position.newBuilder().setIdentifierType(1).setValueInteger(123456789)
                        .setLatitude(range.invX(entry.geometry().x()))
                        .setLongitude(range.invY(entry.geometry().y()))
                        .setTimeEpochMs(Math.round((double) range.invZ(entry.geometry().z())))
                        .build();
                b.addPositions(p);
            }
        } else {
            // is NonLeaf
            NonLeaf<Object, Point> n = (NonLeaf<Object, Point>) node;
            for (Node<Object, Point> child : n.children()) {
                b.addChildren(toProtoNode(child, range));
            }
        }
        com.github.davidmoten.rtree3d.proto.RTreeProtos.Box box = createProtoBox(
                node.geometry().mbb());
        b.setMbb(box);
        return b.build();
    }

    private static void writeNodeAsSplitProtos(Node<Object, Point> node, Range range, int maxDepth,
            File dir) {
        writeBytesToFile(toProtoNodeSplit(node, range, 0, maxDepth, dir).toByteArray(),
                new File(dir, "top"), true);
    }

    private static void writeBytesToFile(byte[] bytes, File file, boolean zip) {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file)));
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
            Node<Object, Point> node, Range range, int depth, int maxDepth, File dir) {
        Builder b = RTreeProtos.Node.newBuilder();
        if (depth <= maxDepth && node instanceof Leaf) {
            for (Entry<Object, Point> entry : ((Leaf<Object, Point>) node).entries()) {
                Position p = Position.newBuilder().setIdentifierType(1).setValueInteger(123456789)
                        .setLatitude(range.invX(entry.geometry().x()))
                        .setLongitude(range.invY(entry.geometry().y()))
                        .setTimeEpochMs(Math.round((double) range.invZ(entry.geometry().z())))
                        .build();
                b.addPositions(p);
            }
        } else if (depth < maxDepth && node instanceof NonLeaf) {
            // is NonLeaf
            NonLeaf<Object, Point> n = (NonLeaf<Object, Point>) node;
            for (Node<Object, Point> child : n.children()) {
                b.addChildren(toProtoNodeSplit(child, range, depth + 1, maxDepth, dir));
            }
        } else if (depth == maxDepth && node instanceof NonLeaf) {
            // is NonLeaf
            NonLeaf<Object, Point> n = (NonLeaf<Object, Point>) node;
            for (Node<Object, Point> child : n.children()) {
                com.github.davidmoten.rtree3d.proto.RTreeProtos.Node proto = toProtoNode(child,
                        range);
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
                entries.add(Entry.entry((Object) id, (Geometry) createBox(id.getMbb())));
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
            for (com.github.davidmoten.rtree3d.proto.RTreeProtos.Position p : node
                    .getPositionsList()) {
                entries.add(Entry.entry((Object) p,
                        (Geometry) Point.create(p.getLatitude(), p.getLongitude())));
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

    private static class Range {
        final float minX;
        final float minY;
        final float minZ;
        final float maxX;
        final float maxY;
        final float maxZ;

        private Range(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        float normX(float x) {
            return (x - minX) / (maxX - minX);
        }

        float normY(float y) {
            return (y - minY) / (maxY - minY);
        }

        float normZ(float z) {
            return (z - minZ) / (maxZ - minZ);
        }

        float invX(float x) {
            return x * (maxX - minX) + minX;
        }

        float invY(float y) {
            return y * (maxY - minY) + minY;
        }

        float invZ(float z) {
            return z * (maxZ - minZ) + minZ;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Info [minX=");
            builder.append(minX);
            builder.append(", minY=");
            builder.append(minY);
            builder.append(", minZ=");
            builder.append(minZ);
            builder.append(", maxX=");
            builder.append(maxX);
            builder.append(", maxY=");
            builder.append(maxY);
            builder.append(", maxZ=");
            builder.append(maxZ);
            builder.append("]");
            return builder.toString();
        }

    }

    public static void main(String[] args) {
        int count = BinaryFixes.from(new File("/home/dave/2014-01-01-binary-fixes-with-mmsi"), true,
                BinaryFixesFormat.WITH_MMSI).count().toBlocking().single();
        System.out.println(count);
    }

}
