package org.lwjgl.system;

import org.lwjgl.PointerBuffer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.function.LongPredicate;

import static org.lwjgl.system.MemoryUtil.UNSAFE;
import static org.lwjgl.system.Pointer.BITS32;
import static org.lwjgl.system.jni.JNINativeInterface.NewDirectByteBuffer;

public class MemoryHelpers {

    private static final long MARK;
    private static final long POSITION;
    private static final long LIMIT;
    private static final long CAPACITY;

    private static final long ADDRESS;


    static {
        try {
            MARK = getMarkOffset();
            POSITION = getPositionOffset();
            LIMIT = getLimitOffset();
            CAPACITY = getCapacityOffset();

            ADDRESS = getAddressOffset();
        } catch (Throwable t) {
            throw new UnsupportedOperationException(t);
        }
    }

    private static long getFieldOffset(Class<?> containerType, Class<?> fieldType, LongPredicate predicate) {
        Class<?> c = containerType;
        while (c != Object.class) {
            Field[] fields = c.getDeclaredFields();
            for (Field field : fields) {
                if (!field.getType().isAssignableFrom(fieldType) || Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }

                long offset = UNSAFE.objectFieldOffset(field);
                if (predicate.test(offset)) {
                    return offset;
                }
            }
            c = c.getSuperclass();
        }
        throw new UnsupportedOperationException("Failed to find field offset in class.");
    }

    private static long getFieldOffsetInt(Object container, int value) {
        return getFieldOffset(container.getClass(), int.class, offset -> UNSAFE.getInt(container, offset) == value);
    }

    private static long getAddressOffset() {
        long MAGIC_ADDRESS = 0xDEADBEEF8BADF00DL & (BITS32 ? 0xFFFF_FFFFL : 0xFFFF_FFFF_FFFF_FFFFL);

        ByteBuffer bb = Objects.requireNonNull(NewDirectByteBuffer(MAGIC_ADDRESS, 0));

        return getFieldOffset(bb.getClass(), long.class, offset -> UNSAFE.getLong(bb, offset) == MAGIC_ADDRESS);
    }

    private static final int MAGIC_CAPACITY = 0x0D15EA5E;
    private static final int MAGIC_POSITION = 0x00FACADE;

    private static long getMarkOffset() {
        ByteBuffer bb = Objects.requireNonNull(NewDirectByteBuffer(1L, 0));
        return getFieldOffsetInt(bb, -1);
    }

    private static long getPositionOffset() {
        ByteBuffer bb = Objects.requireNonNull(NewDirectByteBuffer(-1L, MAGIC_CAPACITY));
        bb.position(MAGIC_POSITION);
        return getFieldOffsetInt(bb, MAGIC_POSITION);
    }

    private static long getLimitOffset() {
        ByteBuffer bb = Objects.requireNonNull(NewDirectByteBuffer(-1L, MAGIC_CAPACITY));
        bb.limit(MAGIC_POSITION);
        return getFieldOffsetInt(bb, MAGIC_POSITION);
    }

    private static long getCapacityOffset() {
        ByteBuffer bb = Objects.requireNonNull(NewDirectByteBuffer(-1L, MAGIC_CAPACITY));
        bb.limit(0);
        return getFieldOffsetInt(bb, MAGIC_CAPACITY);
    }


    public static ByteBuffer remapByteBuffer(ByteBuffer buffer, long address, int size) {
        UNSAFE.putLong(buffer, ADDRESS, address);
        UNSAFE.putInt(buffer, MARK, -1);
        UNSAFE.putInt(buffer, LIMIT, size);
        UNSAFE.putInt(buffer, CAPACITY, size);
        buffer.position(0);
        return buffer;
    }
}
