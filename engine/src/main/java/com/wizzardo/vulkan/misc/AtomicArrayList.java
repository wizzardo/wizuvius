package com.wizzardo.vulkan.misc;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class AtomicArrayList<T> implements List<T> {
    protected volatile AtomicReferenceArray<T> array;
    protected AtomicInteger size = new AtomicInteger(0);
    protected AtomicInteger modificationsCounter = new AtomicInteger(0);

    public AtomicArrayList(int capacity) {
        array = new AtomicReferenceArray<>(capacity);
    }

    @Override
    public int size() {
        return size.get();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        for (int i = 0; i < size.get(); i++) {
            if (o.equals(array.get(i)))
                return true;
        }
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int position = 0;

            @Override
            public boolean hasNext() {
                return position < size();
            }

            @Override
            public T next() {
                return array.get(position++);
            }
        };
    }

    @Override
    public Object[] toArray() {
        Object[] objects = new Object[size()];
        for (int i = 0; i < objects.length; i++) {
            objects[i] = array.get(i);
        }
        return objects;
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        T1[] objects = a.length >= size() ? a : (T1[]) Array.newInstance(a.getClass().getComponentType(), size());
        for (int i = 0; i < objects.length; i++) {
            objects[i] = (T1) array.get(i);
        }
        return objects;
    }

    @Override
    public boolean add(T t) {
        int i = size.get();
        if (i < array.length() - 1) {
            modificationsCounter.incrementAndGet();
            if (array.compareAndSet(i, null, t)) {
                if (size.compareAndSet(i, i + 1)) {
                    return true;
                } else {
                    array.set(i, null);
                    return add(t);
                }
            } else {
                array.set(i, null);
                return add(t);
            }
        } else {
            increaseSizeTo(i * 2);
            return add(t);
        }
    }

    protected void increaseSizeTo(int newSize) {
        synchronized (this) {
            if (array.length() == newSize)
                return;

            AtomicReferenceArray<T> referenceArray = new AtomicReferenceArray<>(newSize);
            int modifications;
            do {
                modifications = modificationsCounter.get();
                for (int i = 0; i < size(); i++) {
                    referenceArray.set(i, array.get(i));
                }
            } while (modificationsCounter.get() != modifications);
            array = referenceArray;
        }

    }

    @Override
    public boolean remove(Object o) {
        int i = indexOf(o);
        if (i < 0)
            return false;
        if (array.compareAndSet(i, (T) o, null)) {
            shiftLeft(i + 1);
            return true;
        } else {
            return remove(o);
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o))
                return false;
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        for (T t : c) {
            if (!add(t))
                return false;
        }
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object o : c) {
            changed |= remove(o);
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        int modifications;
        do {
            modifications = modificationsCounter.incrementAndGet();
            for (int i = 0; i < size(); i++) {
                array.set(i, null);
            }
        } while (modificationsCounter.get() != modifications);
        size.set(0);
    }

    @Override
    public T get(int index) {
        return array.get(index);
    }

    @Override
    public T set(int index, T element) {
        if (index < size()) {
            T prev = array.get(index);
            modificationsCounter.incrementAndGet();
            if (array.compareAndSet(index, prev, element)) {
                return prev;
            } else {
                return set(index, element);
            }
        } else if (index == size()) {
            add(element);
            return null;
        } else
            throw new ArrayIndexOutOfBoundsException();
    }

    @Override
    public void add(int index, T element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T remove(int index) {
        T prev = array.get(index);
        modificationsCounter.incrementAndGet();
        shiftLeft(index + 1);
        return prev;
    }

    protected void shiftLeft(int start) {
        if (start < 1)
            return;

        for (int i = start; i < size(); i++) {
            array.set(i - 1, array.get(i));
        }
        array.set(size.decrementAndGet(), null);
    }

    @Override
    public int indexOf(Object o) {
        for (int i = 0; i < size(); i++) {
            if (o.equals(array.get(i)))
                return i;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        for (int i = size() - 1; i >= 0; i--) {
            if (o.equals(array.get(i)))
                return i;
        }
        return -1;
    }

    @Override
    public ListIterator<T> listIterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < size(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(array.get(i));
        }
        sb.append("]");
        return sb.toString();
    }
}
