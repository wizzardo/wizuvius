package org.lwjgl.system;

public class MemoryAccessJNI {

    static native int getPointerSize();

    @NativeType("void * (*) (size_t)")
    private static native long malloc();

    @NativeType("void * (*) (size_t, size_t)")
    private static native long calloc();

    @NativeType("void * (*) (void *, size_t)")
    private static native long realloc();

    @NativeType("void (*) (void *)")
    private static native long free();

    @NativeType("void * (*) (size_t, size_t)")
    private static native long aligned_alloc();

    @NativeType("void (*) (void *)")
    private static native long aligned_free();

    static native byte ngetByte(long var0);

    @NativeType("int8_t")
    static byte getByte(@NativeType("void *") long ptr) {
        if (Checks.CHECKS) {
            Checks.check(ptr);
        }

        return ngetByte(ptr);
    }

    static native short ngetShort(long var0);

    @NativeType("int16_t")
    static short getShort(@NativeType("void *") long ptr) {
        if (Checks.CHECKS) {
            Checks.check(ptr);
        }

        return ngetShort(ptr);
    }

    static native int ngetInt(long var0);

    @NativeType("int32_t")
    static int getInt(@NativeType("void *") long ptr) {
        if (Checks.CHECKS) {
            Checks.check(ptr);
        }

        return ngetInt(ptr);
    }

    static native long ngetLong(long var0);

    @NativeType("int64_t")
    static long getLong(@NativeType("void *") long ptr) {
        if (Checks.CHECKS) {
            Checks.check(ptr);
        }

        return ngetLong(ptr);
    }

    static native float ngetFloat(long var0);

    static float getFloat(@NativeType("void *") long ptr) {
        if (Checks.CHECKS) {
            Checks.check(ptr);
        }

        return ngetFloat(ptr);
    }

    static native double ngetDouble(long var0);

    static double getDouble(@NativeType("void *") long ptr) {
        if (Checks.CHECKS) {
            Checks.check(ptr);
        }

        return ngetDouble(ptr);
    }

    static native long ngetAddress(long var0);

    @NativeType("intptr_t")
    static long getAddress(@NativeType("void *") long ptr) {
        if (Checks.CHECKS) {
            Checks.check(ptr);
        }

        return ngetAddress(ptr);
    }

    static native void nputByte(long var0, byte var2);

    static void putByte(@NativeType("void *") long ptr, @NativeType("int8_t") byte value) {
        if (Checks.CHECKS) {
            Checks.check(ptr);
        }

        nputByte(ptr, value);
    }

    static native void nputShort(long var0, short var2);

    static void putShort(@NativeType("void *") long ptr, @NativeType("int16_t") short value) {
        if (Checks.CHECKS) {
            Checks.check(ptr);
        }

        nputShort(ptr, value);
    }

    static native void nputInt(long var0, int var2);

    static void putInt(@NativeType("void *") long ptr, @NativeType("int32_t") int value) {
        if (Checks.CHECKS) {
            Checks.check(ptr);
        }

        nputInt(ptr, value);
    }

    static native void nputLong(long var0, long var2);

    static void putLong(@NativeType("void *") long ptr, @NativeType("int64_t") long value) {
        if (Checks.CHECKS) {
            Checks.check(ptr);
        }

        nputLong(ptr, value);
    }

    static native void nputFloat(long var0, float var2);

    static void putFloat(@NativeType("void *") long ptr, float value) {
        if (Checks.CHECKS) {
            Checks.check(ptr);
        }

        nputFloat(ptr, value);
    }

    static native void nputDouble(long var0, double var2);

    static void putDouble(@NativeType("void *") long ptr, double value) {
        if (Checks.CHECKS) {
            Checks.check(ptr);
        }

        nputDouble(ptr, value);
    }

    static native void nputAddress(long var0, long var2);

    static void putAddress(@NativeType("void *") long ptr, @NativeType("intptr_t") long value) {
        if (Checks.CHECKS) {
            Checks.check(ptr);
        }

        nputAddress(ptr, value);
    }
}
