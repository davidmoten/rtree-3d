package com.github.davidmoten.rtree3d;

import java.util.List;

public class Util {

    public static <T extends HasGeometry> Box mbb(List<T> list) {
        // TODO Auto-generated method stub
        return null;
    }

    public static <T, S extends Geometry> List<Entry<T, S>> add(List<Entry<T, S>> entries, Entry<T, S> entry) {
        // TODO Auto-generated method stub
        return null;
    }

    public static <T,S extends Geometry> List<? extends Node<T, S>> replace(List<? extends Node<T, S>> children,
            Node<T, S> child, List<Node<T, S>> list) {
        // TODO Auto-generated method stub
        return null;
    }

    public static <T, S extends Geometry> List<Node<T, S>> remove(List<? extends Node<T, S>> children,
            List<Node<T, S>> removeTheseNodes) {
        // TODO Auto-generated method stub
        return null;
    }

}
