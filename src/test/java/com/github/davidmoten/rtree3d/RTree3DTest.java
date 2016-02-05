package com.github.davidmoten.rtree3d;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import org.junit.Test;

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
        RTree<Object, Point> tree = RTree.star().minChildren(50).maxChildren(1000).create();
        tree = tree.add(entries).last().toBlocking().single();
        System.out.println(tree.size());
        System.out.println(tree.asString());

    }

}
