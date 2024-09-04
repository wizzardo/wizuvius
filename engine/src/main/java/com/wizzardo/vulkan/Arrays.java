package com.wizzardo.vulkan;

public class Arrays {
    public static final int[] EMPTY_INT_ARRAY = new int[0];

    public static boolean contains(int[] src, int value) {
        return indexOf(src, value) != -1;
    }

    public static int indexOf(int[] src, int value) {
        for (int i = 0; i < src.length; i++) {
            if (src[i] == value)
                return i;
        }
        return -1;
    }
}
