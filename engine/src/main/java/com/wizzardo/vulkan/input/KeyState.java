package com.wizzardo.vulkan.input;

import java.util.concurrent.atomic.AtomicIntegerArray;

public abstract class KeyState {
    protected AtomicBitSet keys = new AtomicBitSet(512);
    protected AtomicBitSet mouse = new AtomicBitSet(16);

    public boolean isKeyPressed(int key) {
        return keys.get(key);
    }

    protected void setPressed(int key, boolean pressed) {
        if (pressed)
            keys.set(key);
        else
            keys.clear(key);
    }

    public boolean isMouseButtonPressed(int key) {
        return mouse.get(key);
    }

    protected void setMouseButtonPressed(int key, boolean pressed) {
        if (pressed)
            mouse.set(key);
        else
            mouse.clear(key);
    }

    public int awtToNative(int awtKey) {
        return awtKey;
    }

    public int nativeToAwt(int nativeKey) {
        return nativeKey;
    }

    public abstract boolean isShiftPressed();

    public abstract boolean isCtrlPressed();

    public abstract boolean isAltPressed();

    public abstract boolean isMetaPressed();

    public static class AtomicBitSet {
        private final AtomicIntegerArray array;

        public AtomicBitSet(int length) {
            int intLength = (length + 31) >>> 5; // unsigned / 32
            array = new AtomicIntegerArray(intLength);
        }

        public void set(int n) {
            int bit = 1 << n;
            int idx = n >>> 5;
            while (true) {
                int num = array.get(idx);
                int num2 = num | bit;
                if (num == num2 || array.compareAndSet(idx, num, num2))
                    return;
            }
        }

        public void clear(int n) {
            int bit = ~(1 << n);
            int idx = n >>> 5;
            while (true) {
                int num = array.get(idx);
                int num2 = num & bit;
                if (num == num2 || array.compareAndSet(idx, num, num2))
                    return;
            }
        }

        public boolean get(int n) {
            int bit = 1 << n;
            int idx = n >>> 5;
            int num = array.get(idx);
            return (num & bit) != 0;
        }
    }
}
