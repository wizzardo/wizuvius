package com.wizzardo.vulkan.misc;

import java.util.Arrays;

public class IntArrayBuilder {

    protected int[] buf;
    protected int count;

    public IntArrayBuilder() {
        this(32);
    }

    public IntArrayBuilder(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative initial size: " + size);
        }
        buf = new int[size];
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity - buf.length > 0)
            grow(minCapacity);
    }


    private void grow(int minCapacity) {
        int oldCapacity = buf.length;
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        buf = Arrays.copyOf(buf, newCapacity);
    }

    public void append(int[] b, int off, int len) {
        if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) - b.length > 0)) {
            throw new IndexOutOfBoundsException();
        }
        ensureCapacity(count + len);
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }

    public void reset() {
        count = 0;
    }

    public int[] toIntArray() {
        return toIntArray(false);
    }

    public int[] toIntArray(boolean useInternalBufIfSameSize) {
        if (useInternalBufIfSameSize && buf.length == count)
            return buf;
        return Arrays.copyOf(buf, count);
    }

    public int size() {
        return count;
    }

    public void append(int[] data) {
        append(data, 0, data.length);
    }

    public void append(int value) {
        ensureCapacity(count + 1);
        buf[count++] = value;
    }
}
