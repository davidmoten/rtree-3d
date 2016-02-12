package com.github.davidmoten.rtree3d;

import java.io.InputStream;
import java.io.OutputStream;

import com.github.davidmoten.rtree3d.geometry.Geometry;

import rx.functions.Action2;
import rx.functions.Func1;

public class Serializer {

    public static <T, S extends Geometry> void serialize(RTree<T, S> tree,
            Action2<T, OutputStream> objectSerializer, OutputStream os) {
        // TODO
    }

    public static <T, S extends Geometry> RTree<T, S> deserialize(InputStream is,
            Func1<InputStream, T> objectDeserializer) {
        // TODO
        return null;
    }

}
