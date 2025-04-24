package com.wizzardo.vulkan.material;

import com.wizzardo.vulkan.ResourceCleaner;
import com.wizzardo.vulkan.UniformBuffer;
import com.wizzardo.vulkan.UniformBuffers;
import com.wizzardo.vulkan.VulkanApplication;
import org.joml.*;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.nio.ByteBuffer;

public abstract class Uniform {
    public final int stage;
    public final int size;
    public final int binding;
    public final UniformBuffer uniformBuffer;
    final VkDevice device;

    public Uniform(VkPhysicalDevice physicalDevice, VkDevice device, int stage, int size, int binding, ResourceCleaner cleaner) {
        this(physicalDevice, device, stage, size, binding, cleaner,null);
    }

    public Uniform(VulkanApplication app, int stage, int size, int binding) {
        this(app.getPhysicalDevice(), app.getDevice(), stage, size, binding, app.getResourceCleaner(),null);
    }

    public Uniform(VkPhysicalDevice physicalDevice, VkDevice device, int stage, int size, int binding, ResourceCleaner cleaner, UniformBuffer uniformBuffer) {
        this.stage = stage;
        this.size = size;
        this.binding = binding;
        this.device = device;

        if (uniformBuffer != null) {
            this.uniformBuffer = uniformBuffer;
        } else {
            this.uniformBuffer = UniformBuffers.createUniformBufferObject(physicalDevice, device, size);
            this.uniformBuffer.map(device, cleaner);
        }
    }

    public void update() {
        write(uniformBuffer.getBuffer().clear());
    }

    protected abstract void write(ByteBuffer byteBuffer);

    public static class Int extends Uniform {
        protected int value;

        public Int(VulkanApplication app, int stage, int binding, int value) {
            super(app, stage, Integer.BYTES, binding);
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

        public Float(VulkanApplication app, int stage, int binding, float value) {
            super(app, stage, java.lang.Float.BYTES, binding);
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

        public Vec2(VulkanApplication app, int stage, int binding, Vector2f value) {
            super(app, stage, java.lang.Float.BYTES * 2, binding);
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

        public Vec3(VulkanApplication app, int stage, int binding, Vector3f value) {
            super(app, stage, java.lang.Float.BYTES * 3, binding);
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

    public static class Vec4 extends Uniform {
        protected Quaternionf value;

        public Vec4(VulkanApplication app, int stage, int binding, Quaternionf value) {
            super(app, stage, java.lang.Float.BYTES * 4, binding);
            this.value = value;
        }

        public void set(Quaternionf value) {
            this.value = value;
            update();
        }

        @Override
        protected void write(ByteBuffer byteBuffer) {
            byteBuffer.putFloat(value.x);
            byteBuffer.putFloat(value.y);
            byteBuffer.putFloat(value.z);
            byteBuffer.putFloat(value.w);
        }
    }

    public static class Mat4 extends Uniform {
        protected Matrix4f value;

        public Mat4(VulkanApplication app, int stage, int binding, Matrix4f value) {
            super(app, stage, java.lang.Float.BYTES * 16, binding);
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

    public static class Mat4Array extends Uniform {
        protected Matrix4f[] value;

        public Mat4Array(VulkanApplication app, int stage, int binding, Matrix4f[] value) {
            super(app, stage, java.lang.Float.BYTES * 16 * value.length, binding);
            this.value = value;
        }

        public void set(Matrix4f[] value) {
            this.value = value;
            update();
        }

        @Override
        protected void write(ByteBuffer byteBuffer) {
            for (int i = 0; i < value.length; i++) {
                value[i].get(i * java.lang.Float.BYTES * 16, byteBuffer);
            }
        }
    }

    public static class Vec3Array extends Uniform {
        protected Vector3f[] value;

        public Vec3Array(VulkanApplication app, int stage, int binding, Vector3f[] value) {
            super(app, stage, java.lang.Float.BYTES * 3 * value.length, binding);
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

    public static class IntArray extends Uniform {
        protected int[] value;

        public IntArray(VulkanApplication app, int stage, int binding, int[] value) {
            super(app, stage, java.lang.Integer.BYTES * value.length, binding);
            this.value = value;
        }

        public IntArray(VulkanApplication app, int stage, int binding, int[] value, UniformBuffer uniformBuffer) {
            super(app.getPhysicalDevice(), app.getDevice(), stage, java.lang.Integer.BYTES * value.length, binding, app.getResourceCleaner(), uniformBuffer);
            this.value = value;
        }

        public void set(int[] value) {
            this.value = value;
            update();
        }

        @Override
        protected void write(ByteBuffer buffer) {
            for (int i = 0; i < value.length; i++) {
                int v = value[i];
                buffer.putInt(v);
            }
        }
    }
}
