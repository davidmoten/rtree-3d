package com.github.davidmoten.rtree3d;

import java.io.OutputStream;

import com.github.davidmoten.rtree3d.geometry.Geometry;

import rx.functions.Action2;

public class Serializer {

    public static <T, S extends Geometry> void serialize(RTree<T, S> tree,
            Action2<T, OutputStream> objectSerializer, OutputStream os) {
        
    }

}
