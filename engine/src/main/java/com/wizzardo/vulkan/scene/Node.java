package com.wizzardo.vulkan.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Node extends Spatial {
    protected List<Spatial> children = new ArrayList<>();

    public List<Spatial> getChildren() {
        return children;
    }

    public void attachChild(Spatial spatial) {
        children.add(spatial);
        spatial.setParent(this);
    }

    public boolean detachChild(Spatial spatial) {
        boolean removed = children.remove(spatial);
        if (removed) {
            spatial.setParent(null);
        }
        return removed;
    }

    public Stream<Geometry> geometries() {
        return StreamSupport.stream(new Spliterator<Geometry>() {
            Cursor<Spatial> cursor = new Cursor<>(null, children);

            @Override
            public boolean tryAdvance(Consumer<? super Geometry> action) {
                do {
                    while (cursor.position < cursor.list.size()) {
                        Spatial spatial = cursor.list.get(cursor.position++);
                        if (spatial instanceof Geometry) {
                            action.accept((Geometry) spatial);
                            return true;
                        } else if (spatial instanceof Node) {
                            cursor = new Cursor<>(cursor, ((Node) spatial).children);
                        } else {
                            throw new IllegalStateException("Unknown type of Spatial: " + spatial.getClass());
                        }
                    }
                    cursor = cursor.parent;
                } while (cursor != null);
                return false;
            }

            @Override
            public Spliterator<Geometry> trySplit() {
                return null;
            }

            @Override
            public long estimateSize() {
                return Long.MAX_VALUE;
            }

            @Override
            public int characteristics() {
                return 0;
            }
        }, false);
    }

    static class Cursor<T> {
        final Cursor<T> parent;
        final List<T> list;
        int position;

        Cursor(Cursor<T> parent, List<T> list) {
            this.parent = parent;
            this.list = list;
        }
    }
}
