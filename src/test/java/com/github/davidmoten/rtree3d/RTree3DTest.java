package com.github.davidmoten.rtree3d;

import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import org.junit.Test;

import com.github.davidmoten.rtree3d.geometry.Box;
import com.github.davidmoten.rtree3d.geometry.Geometry;
import com.github.davidmoten.rtree3d.geometry.Point;
import com.github.davidmoten.rx.Strings;

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

        Info info = new Info(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE,
                -Float.MAX_VALUE, -Float.MAX_VALUE);
        final Info range = entries.reduce(info, new Func2<Info, Entry<Object, Point>, Info>() {
            @Override
            public Info call(Info info, Entry<Object, Point> p) {
                return new Info(Math.min(info.minX, p.geometry().x()),
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
                                Point.create((entry.geometry().x() - range.minX)
                                        / (range.maxX - range.minX),
                                (entry.geometry().y() - range.minY) / (range.maxY - range.minY),
                                (entry.geometry().z() - range.minZ) / (range.maxZ - range.minZ)));
                    }
                });
        System.out.println(range);
        int n = 4;

        RTree<Object, Point> tree = RTree.star().minChildren((n) / 2).maxChildren(n).create();
        tree = tree.add(normalized.take(100000)).last().toBlocking().single();
        System.out.println(tree.size());
        System.out.println(tree.calculateDepth());
        System.out.println(tree.asString(3));
        long t = System.currentTimeMillis();
        int count = tree.search(Box.create(39.0, 22.0, 0, 40.0, 23.0, 3.15684946E11)).count()
                .toBlocking().single();
        t = System.currentTimeMillis() - t;
        System.out.println("search=" + count + " in " + t + "ms");
        PrintStream out = new PrintStream("target/out.txt");

        print(tree.root().get(), out, 4, 4);
        out.close();
        System.out.println("finished");
    }

    private static <T extends Geometry> void print(Node<Object, T> node, PrintStream out,
            int depth) {
        print(node, out, depth, depth);
    }

    private static <T extends Geometry> void print(Node<Object, T> node, PrintStream out,
            int minDepth, int maxDepth) {
        print(node, out, 1, minDepth, maxDepth);
    }

    private static <T extends Geometry> void print(Node<Object, T> node, PrintStream out, int depth,
            int minDepth, int maxDepth) {
        if (depth > maxDepth) {
            return;
        }
        if (node instanceof NonLeaf) {
            NonLeaf<Object, T> n = (NonLeaf<Object, T>) node;
            Box b = node.geometry().mbr();
            if (depth >= minDepth)
                print(b, out);
            for (Node<Object, T> child : n.children()) {
                print(child, out, depth + 1, minDepth, maxDepth);
            }
        } else if (node instanceof Leaf && depth >= minDepth) {
            Leaf<Object, T> n = (Leaf<Object, T>) node;
            print(n.geometry().mbr(), out);
        }
    }

    private static void print(Box b, PrintStream out) {
        out.format("%s,%s,%s,%s,%s,%s\n", b.x1(), b.y1(), b.z1(), b.x2(), b.y2(), b.z2());
    }

    private static class Info {
        final float minX;
        final float minY;
        final float minZ;
        final float maxX;
        final float maxY;
        final float maxZ;

        private Info(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
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

}
