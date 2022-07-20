package com.wizzardo.vulkan.material;

import com.wizzardo.vulkan.Material;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public abstract class SpecializationConstantInfo implements Consumer<ByteBuffer> {
    public final int stage;
    public final int constantId;
    public final int size;

    public SpecializationConstantInfo(int stage, int constantId, int size) {
        this.stage = stage;
        this.constantId = constantId;
        this.size = size;
    }

    public static class Int extends SpecializationConstantInfo {
        protected int value;

        public Int(int stage, int constantId, int value) {
            super(stage, constantId, Integer.BYTES);
            this.value = value;
        }

        @Override
        public void accept(ByteBuffer byteBuffer) {
            byteBuffer.putInt(value);
        }
    }
}
