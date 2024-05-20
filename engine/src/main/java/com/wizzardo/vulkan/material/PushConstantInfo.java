package com.wizzardo.vulkan.material;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public abstract class PushConstantInfo implements Consumer<ByteBuffer> {
    public final int stage;
    public final int size;

    public PushConstantInfo(int stage, int size) {
        this.stage = stage;
        if ((size / 4) * 4 == size)
            this.size = size;
        else
            this.size = (size / 4 + 1) * 4;
    }

    public static class Int extends PushConstantInfo {
        protected int value;

        public Int(int stage, int value) {
            super(stage, Integer.BYTES);
            this.value = value;
        }

        @Override
        public void accept(ByteBuffer byteBuffer) {
            byteBuffer.putInt(value);
        }
    }
}
