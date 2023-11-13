package com.wizzardo.vulkan.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Node {
    protected List<Node> children = new ArrayList<>();
    protected String name;
    protected Node parent;

    public Node(String name) {
        this.name = name;
    }

    public Node() {
    }

    public List<Node> getChildren() {
        return children;
    }

    public void attachChild(Node spatial) {
        children.add(spatial);
        spatial.setParent(this);
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    protected void setParent(Node parent) {
        this.parent = parent;
    }

    public Node getParent() {
        return parent;
    }

    public boolean detachChild(Node spatial) {
        boolean removed = children.remove(spatial);
        if (removed) {
            spatial.setParent(null);
        }
        return removed;
    }

    public Stream<Geometry> geometries() {
        return StreamSupport.stream(new Spliterator<Geometry>() {
            Cursor<Node> cursor = new Cursor<>(null, children);

            @Override
            public boolean tryAdvance(Consumer<? super Geometry> action) {
                do {
                    while (cursor.position < cursor.list.size()) {
                        Node node = cursor.list.get(cursor.position++);
                        if (!node.children.isEmpty())
                            cursor = new Cursor<>(cursor, node.children);

                        if (node instanceof Geometry) {
                            action.accept((Geometry) node);
                            return true;
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
