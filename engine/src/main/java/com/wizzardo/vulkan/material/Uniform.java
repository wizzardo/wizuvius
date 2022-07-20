package com.wizzardo.vulkan.material;

import com.wizzardo.vulkan.UniformBuffer;
import com.wizzardo.vulkan.UniformBuffers;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.nio.ByteBuffer;

public abstract class Uniform {
    public final int stage;
    public final int size;
    public final int binding;
    public final UniformBuffer uniformBuffer;
    final VkDevice device;

    public Uniform(VkPhysicalDevice physicalDevice, VkDevice device, int stage, int size, int binding) {
        this.stage = stage;
        this.size = size;
        this.binding = binding;
        this.device = device;
        this.uniformBuffer = UniformBuffers.createUniformBufferObject(physicalDevice, device, size);
        uniformBuffer.map(device);
    }

    public void update() {
        write(uniformBuffer.getBuffer().clear());
    }

    public void cleanup(VkDevice device) {
        uniformBuffer.cleanup(device);
    }

    protected abstract void write(ByteBuffer byteBuffer);

    public static class Int extends Uniform {
        protected int value;

        public Int(VkPhysicalDevice physicalDevice, VkDevice device, int stage, int binding, int value) {
            super(physicalDevice, device, stage, Integer.BYTES, binding);
            this.value = value;
        }

        public void set(int value) {
            this.value = value;
            update();
        }

        @Override
        protected void write(ByteBuffer byteBuffer) {
            byteBuffer.putInt(value);
        }
    }

    public static class Float extends Uniform {
        protected float value;

        public Float(VkPhysicalDevice physicalDevice, VkDevice device, int stage, int binding, float value) {
            super(physicalDevice, device, stage, java.lang.Float.BYTES, binding);
            this.value = value;
        }

        public void set(float value) {
            this.value = value;
            update();
        }

        @Override
        protected void write(ByteBuffer byteBuffer) {
            byteBuffer.putFloat(value);
        }
    }

    public static class Vec2 extends Uniform {
        protected Vector2f value;

        public Vec2(VkPhysicalDevice physicalDevice, VkDevice device, int stage, int binding, Vector2f value) {
            super(physicalDevice, device, stage, java.lang.Float.BYTES * 2, binding);
            this.value = value;
        }

        public void set(Vector2f value) {
            this.value = value;
            update();
        }

        @Override
        protected void write(ByteBuffer byteBuffer) {
            byteBuffer.putFloat(value.x);
            byteBuffer.putFloat(value.y);
        }
    }

    public static class Vec3 extends Uniform {
        protected Vector3f value;

        public Vec3(VkPhysicalDevice physicalDevice, VkDevice device, int stage, int binding, Vector3f value) {
            super(physicalDevice, device, stage, java.lang.Float.BYTES * 3, binding);
            this.value = value;
        }

        public void set(Vector3f value) {
            this.value = value;
            update();
        }

        @Override
        protected void write(ByteBuffer byteBuffer) {
            byteBuffer.putFloat(value.x);
            byteBuffer.putFloat(value.y);
            byteBuffer.putFloat(value.z);
        }
    }

    public static class Mat4 extends Uniform {
        protected Matrix4f value;

        public Mat4(VkPhysicalDevice physicalDevice, VkDevice device, int stage, int binding, Matrix4f value) {
            super(physicalDevice, device, stage, java.lang.Float.BYTES * 16, binding);
            this.value = value;
        }

        public void set(Matrix4f value) {
            this.value = value;
            update();
        }

        @Override
        protected void write(ByteBuffer byteBuffer) {
            value.get(byteBuffer);
        }
    }

    public static class Vec3Array extends Uniform {
        protected Vector3f[] value;

        public Vec3Array(VkPhysicalDevice physicalDevice, VkDevice device, int stage, int binding, Vector3f[] value) {
            super(physicalDevice, device, stage, java.lang.Float.BYTES * 3 * value.length, binding);
            this.value = value;
        }

        public void set(Vector3f[] value) {
            this.value = value;
            update();
        }

        @Override
        protected void write(ByteBuffer buffer) {
            for (int i = 0; i < value.length; i++) {
                Vector3f v = value[i];
                buffer.putFloat(v.x);
                buffer.putFloat(v.y);
                buffer.putFloat(v.z);
            }
        }
    }
}
