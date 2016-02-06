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
import rx.functions.Func1;

public class RTree3DTest {

    @Test
    public void test() throws IOException {
        Observable<Entry<Object, Point>> entries = Strings
                .from(new GZIPInputStream(RTree3DTest.class
                        .getResourceAsStream("/greek-earthquakes-1964-2000-with-times.txt.gz")))
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
                        SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss.s");
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
                });
        RTree<Object, Point> tree = RTree.star().minChildren(10).maxChildren(50).create();
        tree = tree.add(entries).last().toBlocking().single();
        System.out.println(tree.size());
        System.out.println(tree.asString());
        long t = System.currentTimeMillis();
        int count = tree.search(Box.create(39.0, 22.0, 0, 40.0, 23.0, 3.15684946E11)).count()
                .toBlocking().single();
        t = System.currentTimeMillis() - t;
        System.out.println("search=" + count + " in " + t + "ms");
        PrintStream out = new PrintStream("target/out.txt");
        print(tree.root().get(), out);
        out.close();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Geometry> void print(Node<Object, T> node, PrintStream out) {
        if (node instanceof NonLeaf) {
            NonLeaf<Object, T> n = (NonLeaf<Object, T>) node;
            Box b = node.geometry().mbr();
            print(b, out);
            for (Node<Object, T> child : n.children()) {
                print(child, out);
            }
        } else if (node instanceof Leaf) {
            Leaf<Object, T> n = (Leaf<Object, T>) node;
            print(n.geometry().mbr(), out);
        }
    }

    private static void print(Box b, PrintStream out) {
        out.format("%s,%s,%s,%s,%s,%s\n", b.x1(), b.y1(), b.z1(), b.x2(), b.y2(), b.z2());
    }

}
